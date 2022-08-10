package com.namespace.hlsplayer

import android.app.Activity
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


class videoPlayer: Activity() {

    private var exoPlayer: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var url_ = ""
    private lateinit var playerView: PlayerView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.videoswitch)
        playerView = findViewById(R.id.playerView)

        var playable = intent.getStringExtra("Playable")
        Log.d("____TTT_________T___ ",(playable.toString()))
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) //will hide the status bar
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) //will rotate the screen
        if (playable != null) {
            if (playable.isNotEmpty()) {
                url_ = playable
                preparePlayer()
            } else {
                url_ = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
                Toast.makeText(this, "Address is empty or not Valid! Playing a pre-defined Url", Toast.LENGTH_SHORT).show()
                //   URL = Uri.parse("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
                preparePlayer()
            }
        }
    }


    private fun preparePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer?.playWhenReady = true
        playerView.player = exoPlayer
        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaItem =
            MediaItem.fromUri(url_.toString())
        val mediaSource =
            HlsMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem)
        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.seekTo(playbackPosition)
        exoPlayer?.playWhenReady = playWhenReady
        exoPlayer?.prepare()

    }
    fun clickEvent(v: View) {
        Toast.makeText(v.getContext(), "Available quality", Toast.LENGTH_LONG).show()
    }
    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
    }
}

