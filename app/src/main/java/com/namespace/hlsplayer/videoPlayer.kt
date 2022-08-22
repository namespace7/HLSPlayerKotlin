package com.namespace.hlsplayer

import android.app.AlertDialog
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import com.namespace.hlsplayer.databinding.VideoswitchBinding


const val MAX_HEIGHT = 539
const val MAX_WIDTH = 959


class videoPlayer: AppCompatActivity() {

    companion object {
        @JvmField
        val ARG_VIDEO_POSITION = "VideoActivity.POSITION"
    }
    var isInPipMode:Boolean = false
    private var videoPosition:Long = 0L
    var isPIPModeEnabled:Boolean = true //Has the user disabled PIP mode in AppOpps?
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
    private lateinit var binding: VideoswitchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding  = VideoswitchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //retrieve url data from previous class
        val playable = intent.getStringExtra("Playable")
        Log.d("____TTT_________T___ ",(playable.toString()))
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            this?.putLong(ARG_VIDEO_POSITION, exoPlayer.currentPosition)
        })
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        videoPosition = savedInstanceState!!.getLong(ARG_VIDEO_POSITION)
    }

    override fun onBackPressed(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && isPIPModeEnabled) {
            enterPIPMode()
        } else {
            super.onBackPressed()
        }
    }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if(newConfig !=null){
            videoPosition = exoPlayer.currentPosition
            isInPipMode = !isInPictureInPictureMode
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
    //Called when the user touches the Home or Recents button to leave the app.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPIPMode()
    }
    @Suppress("DEPRECATION")
    fun enterPIPMode(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            videoPosition = exoPlayer.currentPosition
            binding.playerView.useController = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
            /** We need to check this because the system permission check is publically hidden for integers for non-manufacturer-built apps
               https://github.com/aosp-mirror/platform_frameworks_base/blob/studio-3.1.2/core/java/android/app/AppOpsManager.java#L1640
               ********* If we didn't have that problem *********
                val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                if(appOpsManager.checkOpNoThrow(AppOpManager.OP_PICTURE_IN_PICTURE, packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid, packageName) == AppOpsManager.MODE_ALLOWED)
                30MS window in even a restricted memory device (756mb+) is more than enough time to check, but also not have the system complain about holding an action hostage.
             */
            Handler().postDelayed({checkPIPPermission()}, 30)
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun checkPIPPermission(){
        isPIPModeEnabled = isInPictureInPictureMode
        if(!isInPictureInPictureMode){
            onBackPressed()
        }
    }


    //Listener on player
    private fun addPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener{

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when(playbackState){
                    Player.STATE_BUFFERING ->{
                        binding.circularProgressBar.visibility =View.VISIBLE

                    }
                    Player.STATE_READY -> {
                        binding.circularProgressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED ->
                    {
                        binding.playerView.keepScreenOn = false

                    }
                    Player.STATE_IDLE ->
                    {
                        binding.playerView.keepScreenOn = false
                    }

                }
                binding.playerView.keepScreenOn = playWhenReady
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
                    val intent = Intent(applicationContext,MainActivity::class.java)
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

        exoQuality = binding.playerView.findViewById(R.id.change_quality)
        binding.playerView.useController = true//set to true or false to see controllers
        binding.playerView.requestFocus()
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
       binding.playerView.player = exoPlayer
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
        videoPosition = exoPlayer.currentPosition
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        if (Util.SDK_INT > 23) {
            binding.playerView.onPause()
            releasePlayer()
        }
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            && packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            finishAndRemoveTask()
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