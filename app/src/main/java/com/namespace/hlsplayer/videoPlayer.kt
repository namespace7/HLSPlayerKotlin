package com.namespace.hlsplayer

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util


const val MAX_HEIGHT = 539
const val MAX_WIDTH = 959


class videoPlayer: AppCompatActivity(),Player.Listener {

    private lateinit var dialogBuilder: Dialog
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var playerView: PlayerView
    private lateinit var exoQuality: ImageButton
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isPlayerPlaying = true
    private  var HLS_STATIC_URL =""
    private var trackDialog: Dialog? = null
    private lateinit var progressBar: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.videoswitch)
        //retrieve url data from previous class
        var playable = intent.getStringExtra("Playable")
        Log.d("____TTT_________T___ ",(playable.toString()))

        //#setting screen to full

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) //will hide the status bar
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE //will rotate the screen

        //checking if received url data variable is null
        if (playable != null) {
            if (playable.isNotEmpty()) {
                HLS_STATIC_URL = playable
                 val mediaItem = MediaItem.Builder()
                    .setUri(HLS_STATIC_URL)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
                initPlayer()
            } else {
                HLS_STATIC_URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
                val mediaItem = MediaItem.Builder()
                    .setUri(HLS_STATIC_URL)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
                Toast.makeText(this, "Address is empty or not Valid! Playing a pre-defined Url", Toast.LENGTH_SHORT).show()
                initPlayer()
            }
        }

        //defining player view
        playerView = findViewById(R.id.playerView)
        exoQuality = playerView.findViewById(R.id.change_quality)
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Hls Player"))
        exoQuality.setOnClickListener{
            if(trackDialog == null){
                initPopupQuality()
            }
            trackDialog?.show()
        }
    }
    //Listener on player
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when(playbackState){
            Player.STATE_BUFFERING ->{
                progressBar.visibility =View.VISIBLE
            }
            Player.STATE_READY -> {
                progressBar.visibility = View.GONE
            }
            Player.EVENT_PLAYER_ERROR ->{
                Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun initPlayer(){
        progressBar = findViewById(R.id.loading)
        playerView = findViewById(R.id.playerView)
        val mediaItem = MediaItem.Builder()
            .setUri(HLS_STATIC_URL)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        trackSelector = DefaultTrackSelector(this)
        // When player is initialized it'll be played with a quality of MaxVideoSize to prevent loading in 1080p from the start
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(MAX_WIDTH,MAX_HEIGHT))
        exoPlayer = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build().apply {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem)
            prepare()
        }
        playerView.player = exoPlayer
    }

    private fun releasePlayer(){
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }
    //Android life cycle
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()

        if (Util.SDK_INT <= 23) {
            initPlayer()
            playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        if (Util.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        if (Util.SDK_INT > 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    // QUALITY SELECTOR

    private fun initPopupQuality() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var videoRenderer : Int? = null
        if(mappedTrackInfo == null) return else exoQuality.visibility = View.VISIBLE
        for(i in 0 until mappedTrackInfo.rendererCount){
            if(isVideoRenderer(mappedTrackInfo, i)){
                videoRenderer = i
            }
        }
        if(videoRenderer == null){
            exoQuality.visibility = View.GONE
            return
        }
        val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(this, "Available Quality", trackSelector, videoRenderer)
        trackSelectionDialogBuilder.setTrackNameProvider{
            // Override function getTrackName
            getString(R.string.exo_track_resolution_pixel, it.height)
        }
        trackDialog = trackSelectionDialogBuilder.build()
    }

    private fun isVideoRenderer(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length == 0) {
            return false
        }
        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
        return C.TRACK_TYPE_VIDEO == trackType
    }
}