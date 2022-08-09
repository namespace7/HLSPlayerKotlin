package com.namespace.hlsplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


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
                var intent = Intent(applicationContext,videoPlayer::class.java)
                intent.putExtra("Playable","")
                startActivity(intent)
            }
        }
        demoButton.setOnClickListener {
            var intent = Intent(applicationContext,videoPlayer::class.java)
            intent.putExtra("Playable","")
            startActivity(intent)
        }
    }
}