package com.namespace.hlsplayer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.namespace.hlsplayer.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity()  {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playButton.setOnClickListener {
            // Getting the user input
            val text = binding.urlField.text.toString()
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
        binding.demoButton.setOnClickListener {
            var HLS_STATIC_URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
            var intent = Intent(applicationContext,videoPlayer::class.java)
            intent.putExtra("Playable",HLS_STATIC_URL)
            startActivity(intent)
        }
    }


    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        if(isOnline(this)){
            Snackbar.make(findViewById(android.R.id.content), "Connected,Application is online ", Snackbar.LENGTH_LONG)
                .show();
        } else {
            val builder = AlertDialog.Builder(this)
            //set title for alert dialog
            builder.setTitle("Alert")
            //set message for alert dialog
            builder.setMessage("Player Error ! No Data Connectivity Detected")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            //performing positive action
            builder.setPositiveButton("Goto Setting")
            { _, _ ->
                var intent = Intent((Settings.ACTION_WIFI_SETTINGS))

                startActivity(intent)
            }
            builder.setNegativeButton("OK")
            { _, _ ->
                var intent = Intent(applicationContext,MainActivity::class.java)
                finish()
                startActivity(intent)
            }

            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }
}