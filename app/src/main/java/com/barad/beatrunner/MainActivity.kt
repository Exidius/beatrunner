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

        val db = Room.databaseBuilder(this, AppDatabase::class.java, "db1").build()
        musicDao = db.musicDao()

        if(isStoragePermissionGranted()) {
            val musicStore = MusicStore(this, musicDao)

            musicStore.getAllMusicFromDevice(true)

            player = SimpleExoPlayer.Builder(this).build()

            playerView = findViewById(R.id.player_view)
            playerView.showTimeoutMs = 0
            playerView.player = player

            btnTempo = findViewById(R.id.btn_setTempo)

            tv_title = findViewById(R.id.tv_title)
            tvTempo = findViewById(R.id.tv_tempo)
            tvSteps = findViewById(R.id.tv_steps)
            tvMusicTempo = findViewById(R.id.tv_music_tempo)
            inputTempo = findViewById(R.id.et_tempo)

            sensorService = SensorService(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            musicService = MusicService(musicDao, player)

            btnTempo.setOnClickListener {
                inputTempo.text.toString().toFloatOrNull()?.let { it1 -> musicService.onTempoChange(it1) }
            }

            sensorService.steps.observe(this, {
                tvTempo.setText(it.toString())
            })

            sensorService.tempo.observe(this, {
                tvSteps.setText(it.toString())
                musicService.onTempoChange(it)
            })

            musicService.currentMusic.observe(this, {
                tv_title.setText("${it.artist} - ${it.title}")
                tvMusicTempo.setText(it.tempo.toString())
            })
        }

    }

    override fun onStart() {
        super.onStart()
        sensorService.register()
    }

    override fun onStop() {
        super.onStop()
        sensorService.unregister()
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