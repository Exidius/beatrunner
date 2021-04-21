package com.barad.beatrunner.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.R
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class MusicService : Service() {

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
    }

    fun onTempoChange(tempo: Float) {
        if(abs(_currentMusic.value!!.tempo - tempo) > 20) {
            while (musicList.isEmpty()) {
                fetchMusicList()
            }
            //selectNewMusic(tempo)
            //switchMusic()
            lastTempo = tempo
            switchMusic(selectNewMusicList(tempo))
        }
        var speed: Float = 1f//tempo / currentMusic.value!!.tempo //TODO: multiples of 2
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

    override fun onBind(intent: Intent?): IBinder? {
        intent?.let {
            onTempoChange(intent.getFloatExtra("tempo",150f))
            //musicDao = AppDatabase.getInstance(applicationContext).musicDao()
            displayNotification()
        }
        return MusicServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(application).build()
        musicDao = AppDatabase.getInstance(application).musicDao()
        musicEventListener = MusicEventListener(player,this, musicDao)
        player.addListener(musicEventListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            onTempoChange(intent.getFloatExtra("start",120f))
            //musicDao = AppDatabase.getInstance(applicationContext).musicDao()
            displayNotification()
        }

        return START_NOT_STICKY
    }

    private fun displayNotification() {
        if(playerNotificationManager == null) {
            playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                    this,
                    NOTIFICATION_CHANNEL_ID,
                    R.string.playback,
                    0,
                    NOTIFICATION_ID,
                    object : PlayerNotificationManager.MediaDescriptionAdapter {

                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            // return pending intent
                            return null
                        }

                        override fun getCurrentContentText(player: Player): String? {
                            return "${currentMusic.value?.artist}"
                        }

                        override fun getCurrentContentTitle(player: Player): String {
                            return "${currentMusic.value?.title}"
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
            )
            playerNotificationManager?.setPlayer(player)
        }
    }

    inner class MusicServiceBinder : Binder() {
        fun getPlayerInstance() = player
        fun getCurrentMusic() = currentMusic
        val service
            get() = this@MusicService
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        super.onDestroy()
    }

}