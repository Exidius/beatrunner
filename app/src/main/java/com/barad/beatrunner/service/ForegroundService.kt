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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.util.*
import kotlin.math.abs


class ForegroundService : LifecycleService(), SensorEventListener {

    inner class MusicServiceBinder : Binder() {
        fun getPlayerInstance() = player
        val service
            get() = this@ForegroundService
    }

    private var MAX_TEMPO_DIFFERENCE = 20
    private var SIGNIFICANT_TEMPO_DIFFERENCE = 3
    var ALLOW_TEMPO_CHANGE = true

    private lateinit var context: Context

    private val logTimer = Timer()

    private val _sensorTempo = MutableLiveData<Float>()
    val sensorTempo
        get() = _sensorTempo

    private val _steps = MutableLiveData<Float>()
    val steps
        get() = _steps

    private val _gyroSensorTempo = MutableLiveData<Float>()
    val gyroSensorTempo
        get() = _gyroSensorTempo

    private val _gyroSteps = MutableLiveData<Float>()
    val gyroSteps
        get() = _gyroSteps

    private val _currentMusic = MutableLiveData<Music>()
    val currentMusic
        get() = _currentMusic

    private val _currentPlaylist = MutableLiveData<List<Music>>()
    val currentPlaylist
        get() = _currentPlaylist

    private val accelerationStepDetector = AccelerationStepDetector(_steps, _sensorTempo)
    private val gyroscopeStepDetector = GyroscopeStepDetector(_gyroSteps, _gyroSensorTempo)

    private var sensorManager: SensorManager? = null
    private var sensorGyro: Sensor? = null
    private var sensorAcc: Sensor? = null

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
        steps.value = 0f
        gyroSensorTempo.value = 0f
        gyroSteps.value = 0f

        currentPlaylist.observe(this, {
            switchSong()
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        context = applicationContext
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        intent.let {
            context = applicationContext
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

    fun startTimer() {
        val startTime = Instant.now().toEpochMilli()
        logTimer.schedule(object : TimerTask() {
            override fun run() {
                val line = "t: ${Instant.now().toEpochMilli()} gs: ${gyroSteps.value} gt: ${gyroSensorTempo.value} as: ${steps.value} at: ${sensorTempo.value} ${System.lineSeparator()}"

                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/tempolog-${startTime}.txt").toURI())

                if(file.exists()) {
                    val fileWriter = FileWriter(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS + "/tempolog-${startTime}.txt"
                        ),
                        true
                    )
                    val out = BufferedWriter(fileWriter)
                    out.write(line)
                    out.close()
                } else {
                    file.createNewFile();
                }
            }
        },0,1000)
    }

    fun stopTimer() {
        logTimer.cancel()
    }

    fun playNextInPlaylist() {
        player.next()
        play()
    }

    fun changePlaylist(songs: List<Music>) {
        currentPlaylist.value = songs
    }

    fun resetSteps() {
        sensorTempo.value = 0f
        steps.value = 0f
        gyroSensorTempo.value = 0f
        gyroSteps.value = 0f
    }

    fun onTempoChangeFromUi(tempo: Float) {
        accelerationStepDetector.reset()
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
            currentPlaylist.value?.let { setPlayback(it, false) }
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

    private fun registerManager(delay: Int = SensorManager.SENSOR_DELAY_FASTEST) {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorGyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorAcc = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager!!.registerListener(this, sensorGyro, delay)
        sensorManager!!.registerListener(this, sensorAcc, delay)

        gyroscopeStepDetector.startTimer()
        accelerationStepDetector.startTimer()
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
            }
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerationStepDetector.onSensorEvent(event)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeStepDetector.onSensorEvent(event)
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
        gyroscopeStepDetector.stopTimer()
        accelerationStepDetector.stopTimer()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}