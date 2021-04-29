package com.barad.beatrunner.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.MainActivity
import com.barad.beatrunner.R
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt


class MusicService : LifecycleService(), SensorEventListener {

    var MAX_TEMPO_DIFFERENCE = 20
    var SIGNIFICANT_TEMPO_DIFFERENCE = 3
    var ALLOW_TEMPO_CHANGE = true
    var MINIMUM_THRESHOLD = 12

    private val _sensorTempo = MutableLiveData<Float>()
    val sensorTempo
        get() = _sensorTempo

    private val _steps = MutableLiveData<Int>()
    val steps
        get() = _steps

    private val _currentMusic = MutableLiveData<Music>()
    val currentMusic
        get() = _currentMusic

    private val _currentPlaylist = MutableLiveData<List<Music>>()
    val currentPlaylist
        get() = _currentPlaylist

    private var sensorManager: SensorManager? = null
    private var sensorGyro: Sensor? = null
    private var sensorAcc: Sensor? = null
    private var sensorLinAcc: Sensor? = null
    private val sensorQueue: Queue<Float> = LinkedList()
    private val timeQueue: Queue<Instant> = LinkedList()
    private var latest = Instant.now()
    private var currentThreshold = 0f

    private var lastSongTempo = 0f

    private lateinit var player: SimpleExoPlayer
    private lateinit var musicDao: MusicDao
    private lateinit var musicEventListener: MusicEventListener

    private val NOTIFICATION_CHANNEL_ID = "playback_channel"
    private val NOTIFICATION_ID = 2
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val placeHolderMusic = Music(
        Int.MAX_VALUE,
        "Title",
        "Artist",
        "Album",
        "URI",
        "PATH",
        0f
    )

    private var startOfDifference = Instant.MAX
    private var tempoChangeRegistered = false
    private var isDifferenceMax = false
    private var isDifferenceSignificant = false
    private var foundMatchingSongs = false
    private var currentPlaybackTempo = 0.0f

    init{
        currentPlaylist.value = listOf(placeHolderMusic)
        currentMusic.value = placeHolderMusic
        sensorTempo.value = 0f
        steps.value = 0

        currentPlaylist.observe(this, {
            switchSong()
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        intent.let {
            displayNotification()
            registerManager()
        }
        return MusicServiceBinder()
    }

    fun play() {
        if(_sensorTempo.value != 0f) {
            player.prepare()
            if (currentMusic.value?.startTime != 0L) {
                currentMusic.value?.let { player.seekTo(it.startTime) }
            }
            lastSongTempo = currentMusic.value?.tempo!!
            player.playWhenReady = true
        }
    }

    fun playNextInPlaylist() {
        player.next()
        play()
    }

    fun changePlaylist(songs: List<Music>) {
        currentPlaylist.value = songs
    }

    private fun currentPlaylistIsNullOrDefault(): Boolean {
        return currentPlaylist.value == null ||
                currentPlaylist.value?.contains(placeHolderMusic) == true
    }

    fun resetSteps() {
        steps.value = 0
    }

    fun onTempoChangeFromUi(tempo: Float) {
        timeQueue.clear()
        sensorTempo.value = tempo
    }

    private fun checkTempoDifference() {
        when {
            abs(currentMusic.value?.tempo!! - sensorTempo.value!!) > MAX_TEMPO_DIFFERENCE -> {
                isDifferenceMax = true
                isDifferenceSignificant = true
                if (!tempoChangeRegistered) {
                    startOfDifference = Instant.now()
                    tempoChangeRegistered = true
                }
            }
            abs(currentPlaybackTempo - sensorTempo.value!!) > SIGNIFICANT_TEMPO_DIFFERENCE -> {
                isDifferenceMax = false
                isDifferenceSignificant = true
                if (!tempoChangeRegistered) {
                    startOfDifference = Instant.now()
                    tempoChangeRegistered = true
                }
            }
            else -> {
                isDifferenceMax = false
                isDifferenceSignificant = false
                startOfDifference = Instant.now()
                tempoChangeRegistered = false
            }
        }
    }

    private fun switchSong() {
        val songsMatchTempo = mutableListOf<Music>()
        currentPlaylist.value?.forEach {
            if (abs(it.tempo - sensorTempo.value!!) < MAX_TEMPO_DIFFERENCE) {
                songsMatchTempo.add(it)
            }
        }
        if (songsMatchTempo.isEmpty()) {
            currentPlaylist.value?.let { setPlayback(it,false) }
        } else {
            setPlayback(songsMatchTempo)
        }
    }

    private fun setPlayback(musicList: List<Music>, allowSpeedChange: Boolean = true) {
        setPlayerPlaylist(musicList)

        if(allowSpeedChange && ALLOW_TEMPO_CHANGE) {
            changePlaybackSpeed()
        }
    }

    private fun changePlaybackSpeed() {
        var speed = 1f
        if (currentMusic.value?.tempo!! != 0.0f) {
            speed = sensorTempo.value!! / (currentMusic.value?.tempo!!) //TODO: multiples of 2
        }
        Log.d("barad-speedMultiplier", speed.toString())
        player.setPlaybackParameters(PlaybackParameters(speed))
        currentPlaybackTempo = speed * currentMusic.value?.tempo!!
    }

    private fun setPlayerPlaylist(musicList: List<Music>) {
        val mediaItems = mutableListOf<MediaItem>()
        musicList.forEach { mediaItems.add(MediaItem.fromUri(it.uri)) }
        player.setMediaItems(mediaItems.shuffled())
    }

    private fun displayNotification() {
        if(playerNotificationManager == null) {
            playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                applicationContext,
                NOTIFICATION_CHANNEL_ID,
                R.string.playback,
                0,
                NOTIFICATION_ID,
                object : PlayerNotificationManager.MediaDescriptionAdapter {

                    override fun createCurrentContentIntent(player: Player): PendingIntent? =
                        PendingIntent.getActivity(
                            applicationContext,
                            0,
                            Intent(applicationContext, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )

                    override fun getCurrentContentText(player: Player): String? {
                        return "${sensorTempo.value} - ${steps.value}"
                    }

                    override fun getCurrentContentTitle(player: Player): String {
                        return "${currentMusic.value?.artist} - ${currentMusic.value?.title}"
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {
                        return null
                    }
                },
                object : PlayerNotificationManager.NotificationListener {

                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        super.onNotificationPosted(notificationId, notification, ongoing)
                        if (!ongoing) {
                            stopForeground(false)
                        } else {
                            startForeground(notificationId, notification)
                        }

                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        super.onNotificationCancelled(notificationId, dismissedByUser)
                        stopSelf()
                    }
                }
            ).apply {
                setControlDispatcher(DefaultControlDispatcher(0, 0))
                setPlayer(player)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(application).build()
        musicDao = AppDatabase.getInstance(application).musicDao()
        musicEventListener = MusicEventListener(this, musicDao)
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

        sensorManager!!.registerListener(this, sensorGyro, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager!!.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {

            checkTempoDifference()

            if(startOfDifference != Instant.MAX && sensorTempo.value != 0f) {
                if (isDifferenceSignificant && isDifferenceMax &&
                    Instant.now().toEpochMilli() - startOfDifference.toEpochMilli() > 3000) {

                    switchSong()

                } else if (isDifferenceSignificant &&
                    Instant.now().toEpochMilli() - startOfDifference.toEpochMilli() > 3000) {

                    changePlaybackSpeed()

                }

                var diff2 = Instant.now().toEpochMilli() - startOfDifference.toEpochMilli()
                Log.d("barad-asd", "${isDifferenceSignificant} ${diff2} ${sensorTempo.value} ${currentPlaybackTempo} ${currentMusic.value?.tempo}")
            }
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x: Float = event.values[0]
                    val y: Float = event.values[1]
                    val z: Float = event.values[2]

                    sensorQueue.add(sqrt(x * x + y * y + z * z))
                    if (sensorQueue.average() > currentThreshold && sensorQueue.average() > MINIMUM_THRESHOLD) {
                        currentThreshold = sensorQueue.average().toFloat()
                        val diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
                        if (diff > 250) {
                            latest = Instant.now()
                            timeQueue.add(Instant.now())
                            steps.value = steps.value?.plus(1)
                        }
                    }

                    if (sensorQueue.size > 10) {
                        sensorQueue.remove()
                    }
                    if (timeQueue.size > 10) {
                        timeQueue.remove()
                    }

                    if (timeQueue.size > 2) {
                        val timeInstantList: List<Instant> = timeQueue.map { x -> x }
                        var sum: Long = 0
                        for (i in 1 until timeInstantList.size) {
                            sum += timeInstantList[i].toEpochMilli() - timeInstantList[i - 1].toEpochMilli()
                        }
                        sensorTempo.value = (60f / (sum / (timeInstantList.size - 1)) * 1000f)
                    }

                    Log.d("barad-lll", "${currentThreshold} ${sensorQueue.average()} ${(sensorQueue.average() > currentThreshold)}")

                    currentThreshold *= 0.997f
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