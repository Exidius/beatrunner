package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}