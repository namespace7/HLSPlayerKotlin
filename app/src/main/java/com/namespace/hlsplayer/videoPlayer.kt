package com.namespace.hlsplayer

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar


const val MAX_HEIGHT = 539
const val MAX_WIDTH = 959


class videoPlayer: AppCompatActivity() {

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
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.videoswitch)
        //retrieve url data from previous class
        val playable = intent.getStringExtra("Playable")
        Log.d("____TTT_________T___ ",(playable.toString()))
      //  progressBar =  findViewById(R.id.circularProgressBar)
        //#setting screen to full

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) //will hide the status bar
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE //will rotate the screen

        //checking if received url data variable is null
        if (playable != null) {
            if (playable.isNotEmpty()) {
                HLS_STATIC_URL = playable

                initPlayer()
                exoPlayer.prepare()
                addPlayerListeners()
            }
        }

        //track selection onclick

        exoQuality.setOnClickListener{
            if(trackDialog == null){
                initPopupQuality()
            }
            trackDialog?.show()
        }

    }


////// HLS_STATIC_URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
    //Listener on player
    private fun addPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener{

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when(playbackState){
                    Player.STATE_BUFFERING ->{
                        progressBar.visibility =View.VISIBLE

                    }
                    Player.STATE_READY -> {
                        progressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED ->
                    {
                        playerView.keepScreenOn = false

                    }
                    Player.STATE_IDLE ->
                    {
                        playerView.keepScreenOn = false
                    }

                }
                playerView.keepScreenOn = playWhenReady
            }


            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)

                val builder = AlertDialog.Builder(this@videoPlayer)
                //set title for alert dialog
                builder.setTitle("Alert")
                //set message for alert dialog
                builder.setMessage("Player Error ! " + error.sourceException.message )
                builder.setIcon(android.R.drawable.ic_dialog_alert)

                //performing positive action
                builder.setPositiveButton("OK")
                { _, _ ->
                    var intent = Intent(applicationContext,MainActivity::class.java)
                    startActivity(intent)
                }

                // Create the AlertDialog
                val alertDialog: AlertDialog = builder.create()
                // Set other dialog properties
                alertDialog.setCancelable(false)
                alertDialog.show()

                releasePlayer()
            }
        })
    exoPlayer.playWhenReady = true //run file/link when ready to play.
    }



    private fun initPlayer(){

        //defining player view
        playerView = findViewById(R.id.playerView)
        exoQuality = playerView.findViewById(R.id.change_quality)

        progressBar =  findViewById(R.id.circularProgressBar)

        playerView.useController = true//set to true or false to see controllers
        playerView.requestFocus()
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Hls Player"))

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


    override fun onResume() {
        super.onResume()
exoPlayer.playWhenReady = true

    }

    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady =false
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