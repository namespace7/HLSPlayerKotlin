package com.namespace.hlsplayer

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val playButton = findViewById<Button>(R.id.playButton)
        val demoButton = findViewById<Button>(R.id.demoButton)
        val editText = findViewById<EditText>(R.id.url_field)
        playButton.setOnClickListener {
            // Getting the user input
            val text = editText.text.toString()
            if (text != null && text.isNotEmpty()) {
                var intent = Intent(applicationContext,videoPlayer::class.java)
                intent.putExtra("Playable",text)
                startActivity(intent)
            } else {
                MaterialAlertDialogBuilder(this).also {
                    it.setTitle("Alert")
                    it.setMessage("URL Field cannot be Null")
                    it.setCancelable(false)
                    it.setPositiveButton("Got it")
                    { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()

                    }
                    it.show()
                }
            }
        }
        demoButton.setOnClickListener {
            var HLS_STATIC_URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
            var intent = Intent(applicationContext,videoPlayer::class.java)
            intent.putExtra("Playable",HLS_STATIC_URL)
            startActivity(intent)
        }
    }
}