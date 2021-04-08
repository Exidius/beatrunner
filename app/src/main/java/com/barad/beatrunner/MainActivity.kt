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


class MainActivity : AppCompatActivity(), SensorEventListener {

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
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorGyro: Sensor
    private lateinit var sensorAcc: Sensor
    private lateinit var sensorLinAcc: Sensor

    val sensorQueue: Queue<Float> = LinkedList()
    val timeQueue: Queue<Instant> = LinkedList()

    private var asdText = ""
    private var latest = Instant.now()

    var mediaItem: MediaItem? = null

    private lateinit var musicDao: MusicDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(this, AppDatabase::class.java, "db1").build()
        musicDao = db.musicDao()

        if(isStoragePermissionGranted()) {
            val musicStore = MusicStore(this, musicDao)

            musicStore.musicList.observe(this, { list ->
                Log.d("barad-log-main", "----music list---")
                list.forEach {
                    Log.d("barad-log-main", it.toString())
                }
                Log.d("barad-log-main", "-----------------")
            })

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
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorLinAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)


        }

    }

    override fun onStart() {
        super.onStart()

        // DELAY
        sensorManager.registerListener(this,sensorGyro,SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,sensorLinAcc,SensorManager.SENSOR_DELAY_FASTEST)

    }

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val sensorType: Int = event.sensor.type

            when (sensorType) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x: Float = event.values[0]
                    val y: Float = event.values[1]
                    val z: Float = event.values[2]
                    var tempo = 172f

                    sensorQueue.add(sqrt(x*x+y*y+z*z))
                    if (sensorQueue.average() > 20) {
                        var lel = Instant.now().toEpochMilli() - latest.toEpochMilli()
                        if (lel > 150) {
                            latest = Instant.now()
                            timeQueue.add(Instant.now())
                            asdText = ""
                            timeQueue.forEach { asdText += it.toString() + " | " }
                            Log.d("brd-1", timeQueue.size.toString())
                            Log.d("brd-2", asdText)
                            tv_gyro2.setText(timeQueue.size.toString())
                        }
                    }

                    if (sensorQueue.size > 10) { sensorQueue.remove() }
                    if (timeQueue.size > 10) { timeQueue.remove() }

                    if (timeQueue.size > 2) {
                        val list:List<Instant> = timeQueue.map { x -> x }
                        var sum: Long = 0
                        for(i in 1 until list.size) {
                            sum += list[i].toEpochMilli()-list[i-1].toEpochMilli()
                        }
                        tempo = (60f/(sum/list.size)*1000f)
                        tv_gyro3.setText(tempo.toString())



                        var musicList = listOf<Music>()

                        GlobalScope.launch(Dispatchers.Default) {
                            musicList = musicDao.getAll()
                        }.invokeOnCompletion {
                            var closestTempo = 0f
                            var selected = musicList.get(0)
                            musicList.forEach {
                                if (abs(it.tempo-tempo) < abs(closestTempo-tempo)) {
                                    closestTempo = it.tempo
                                    mediaItem = MediaItem.fromUri(it.uri)
                                }
                            }
                        }

                        Log.d("barad-select-s2", mediaItem.toString())

                        if (player.currentMediaItem != mediaItem) {
                            mediaItem?.let { player.setMediaItem(it) }
                            player.prepare()
                            player.play()
                        }


                    }

                    tv_gyro.setText("%.2f".format(x) + " / " + "%.2f".format(y) + " / " + "%.2f".format(z))
                }
                else -> {
                }
            }
        }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}