package com.barad.beatrunner.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.MainActivity
import com.barad.beatrunner.R
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class MusicService : Service(), SensorEventListener {

    private val _sensorTempo = MutableLiveData<Float>()
    val sensorTempo
        get() = _sensorTempo

    private val _steps = MutableLiveData<Int>()
    val steps
        get() = _steps

    private var sensorManager: SensorManager? = null

    private var sensorGyro: Sensor? = null
    private var sensorAcc: Sensor? = null
    private var sensorLinAcc: Sensor? = null

    val sensorQueue: Queue<Float> = LinkedList()
    val timeQueue: Queue<Instant> = LinkedList()

    private var latest = Instant.now()

    private lateinit var player: SimpleExoPlayer
    private lateinit var musicDao: MusicDao
    private lateinit var musicEventListener: MusicEventListener

    var mediaItem: MediaItem? = null
    var musicList = listOf<Music>()
    var lastTempo = 0f

    private val NOTIFICATION_CHANNEL_ID = "playback_channel"
    private val NOTIFICATION_ID = 2
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val _currentMusic = MutableLiveData<Music>()
    val currentMusic
        get() = _currentMusic

    init{
        currentMusic.value = Music(Int.MAX_VALUE,"Title","Artist","Album","URI","PATH",0f)
        sensorTempo.value = 0f
        steps.value = 0
    }

    fun onTempoChangeFromUi(tempo: Float) {
        onTempoChange(tempo)
        timeQueue.clear()
        sensorTempo.value = tempo
    }

    private fun onTempoChange(tempo: Float) {
        if(abs(_currentMusic.value!!.tempo - tempo) > 20) {
            while (musicList.isEmpty()) {
                fetchMusicList()
            }
            //selectNewMusic(tempo)
            //switchMusic()
            lastTempo = tempo
            switchMusic(selectNewMusicList(tempo))
        }
        var speed: Float = tempo / currentMusic.value!!.tempo //TODO: multiples of 2
        Log.d("barad-tempo", speed.toString())
        player.setPlaybackParameters(PlaybackParameters(speed))
    }

    fun onSongChange() {
        switchMusic(selectNewMusicList(lastTempo))
    }

    private fun fetchMusicList() {
        GlobalScope.launch() {
            try {
                musicList = musicDao.getAll()
            } catch (e: Exception) {
                Log.d("barad-serv", e.toString())
            }
        }
    }

    private fun selectNewMusic(tempo: Float) {
        var closestTempo = 0f
        musicList.forEach {
            if (abs(it.tempo - tempo) < abs(closestTempo - tempo) ||
                abs(it.tempo - tempo)*2 < abs(closestTempo - tempo) ||
                abs(it.tempo - tempo)/2 < abs(closestTempo - tempo)) {
                closestTempo = it.tempo
                mediaItem = MediaItem.fromUri(it.uri)
                currentMusic.value = it
            }
        }
    }

    private fun selectNewMusicList(tempo: Float) : List<MediaItem> {
        val candidates = mutableListOf<Music>()

        for (i in musicList.indices) {
            if (abs(musicList[i].tempo - tempo) < 20) {
                candidates.add(musicList[i])
                if (candidates.size >= 20) {
                    break
                }
            }
        }

        val toReturn = mutableListOf<MediaItem>()
        candidates.forEach { toReturn.add(MediaItem.fromUri(it.uri)) }
        return toReturn.shuffled()
    }

    private fun switchMusic() {
        if (player.currentMediaItem != mediaItem) {
            var oldVolume = player.volume

            mediaItem?.let { player.setMediaItem(it) }

            player.prepare()
            player.seekTo(15000)
            player.play()
        }
    }

    private fun switchMusic(musicList: List<MediaItem>) {
        player.setMediaItems(musicList)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        intent?.let {
            displayNotification()
            registerManager()
        }
        return MusicServiceBinder()
    }

    private fun displayNotification() {
        if(playerNotificationManager == null) {
            playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                applicationContext,
                NOTIFICATION_CHANNEL_ID,
                R.string.playback,
                0,
                NOTIFICATION_ID,
                object :  PlayerNotificationManager.MediaDescriptionAdapter {

                    override fun createCurrentContentIntent(player: Player): PendingIntent? = PendingIntent.getActivity(
                            applicationContext,
                            0,
                            Intent(applicationContext, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT)

                    override fun getCurrentContentText(player: Player): String? {
                        return "${sensorTempo.value} - ${steps.value}"
                    }

                    override fun getCurrentContentTitle(player: Player): String {
                        return "${currentMusic.value?.artist} - ${currentMusic.value?.title}"
                    }

                    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        return null
                    }
                },
                object : PlayerNotificationManager.NotificationListener {

                    override fun onNotificationPosted(notificationId: Int,
                                                      notification: Notification,
                                                      ongoing: Boolean) {
                        super.onNotificationPosted(notificationId, notification, ongoing)
                        if (!ongoing) {
                            stopForeground(false)
                        } else {
                            startForeground(notificationId, notification)
                        }

                    }

                    override fun onNotificationCancelled(notificationId: Int,
                                                         dismissedByUser: Boolean) {
                        super.onNotificationCancelled(notificationId, dismissedByUser)
                        stopSelf()
                    }
                }
            ).apply {
                setControlDispatcher(DefaultControlDispatcher(0,0))
                setPlayer(player)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(application).build()
        musicDao = AppDatabase.getInstance(application).musicDao()
        musicEventListener = MusicEventListener(player,this, musicDao)
        player.addListener(musicEventListener)
    }

    inner class MusicServiceBinder : Binder() {
        fun getPlayerInstance() = player
        val service
            get() = this@MusicService
    }

    private fun registerManager() {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorGyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorAcc = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorLinAcc = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensorManager!!.registerListener(this,sensorGyro,SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager!!.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this,sensorLinAcc,SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x: Float = event.values[0]
                    val y: Float = event.values[1]
                    val z: Float = event.values[2]

                    sensorQueue.add(sqrt(x*x+y*y+z*z))
                    if (sensorQueue.average() > 17) {
                        val diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
                        if (diff > 150) {
                            latest = Instant.now()
                            timeQueue.add(Instant.now())
                            steps.value = steps.value?.plus(1)
                            sensorTempo.value?.let { onTempoChange(it) }
                        }
                    }

                    if (sensorQueue.size > 10) { sensorQueue.remove() }
                    if (timeQueue.size > 10) { timeQueue.remove() }

                    if (timeQueue.size > 2) {
                        val timeInstantList:List<Instant> = timeQueue.map { x -> x }
                        var sum: Long = 0
                        for(i in 1 until timeInstantList.size) {
                            sum += timeInstantList[i].toEpochMilli()-timeInstantList[i-1].toEpochMilli()
                        }
                        sensorTempo.value = (60f/(sum/(timeInstantList.size-1))*1000f)
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}