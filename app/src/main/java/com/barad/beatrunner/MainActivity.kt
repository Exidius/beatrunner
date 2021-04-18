package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.service.MusicEventListener
import com.barad.beatrunner.service.MusicService
import com.barad.beatrunner.service.SensorService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView


class MainActivity : AppCompatActivity() {

    private lateinit var sensorService: SensorService
    private lateinit var musicService: MusicService

    private lateinit var playerView: PlayerControlView
    private lateinit var player: SimpleExoPlayer

    private lateinit var btnTempo: Button
    private lateinit var tv_title: TextView
    private lateinit var tvTempo: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvMusicTempo: TextView
    private lateinit var inputTempo: EditText

    private lateinit var musicDao: MusicDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    @SuppressLint("ObsoleteSdkInt")
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else {
            true
        }
    }

}