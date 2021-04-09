package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.service.MusicService
import com.barad.beatrunner.service.SensorService
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private lateinit var sensorService: SensorService
    private lateinit var musicService: MusicService

    private lateinit var playerView: PlayerControlView
    private lateinit var player: SimpleExoPlayer
    private lateinit var btn_slower: Button
    private lateinit var btn_faster: Button
    private lateinit var btn_reset: Button
    private lateinit var tv_tempo: TextView
    private var speed = 1.0F

    private lateinit var tv_gyro: TextView
    private lateinit var tv_gyro2: TextView
    private lateinit var tv_gyro3: TextView

    private var asdText = ""

    private lateinit var musicDao: MusicDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(this, AppDatabase::class.java, "db1").build()
        musicDao = db.musicDao()

        if(isStoragePermissionGranted()) {
            val musicStore = MusicStore(this, musicDao)

/*            musicStore.musicList.observe(this, { list ->
                Log.d("barad-log-main", "----music list---")
                list.forEach {
                    Log.d("barad-log-main", it.toString())
                }
                Log.d("barad-log-main", "-----------------")
            })*/

            //musicStore.getAllMusicFromDevice(true)

            player = SimpleExoPlayer.Builder(this).build()

            playerView = findViewById(R.id.player_view)
            playerView.showTimeoutMs = 0
            playerView.player = player

            btn_slower = findViewById(R.id.btn_slower)
            btn_faster = findViewById(R.id.btn_faster)
            btn_reset = findViewById(R.id.btn_reset)

            btn_slower.setOnClickListener{
                speed -= 0.1F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }
            btn_reset.setOnClickListener{
                speed = 1.0F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }
            btn_faster.setOnClickListener{
                speed += 0.1F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }

            tv_gyro = findViewById(R.id.tv_gyro)
            tv_gyro2 = findViewById(R.id.tv_gyro2)
            tv_gyro3 = findViewById(R.id.tv_gyro3)

            sensorService = SensorService(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            musicService = MusicService(musicDao, player)

            sensorService.tempo.observe(this, {
                tv_gyro2.setText(it.toString())
                musicService.onTempoChange(it)
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