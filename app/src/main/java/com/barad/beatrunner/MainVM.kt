package com.barad.beatrunner

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.service.MusicEventListener
import com.barad.beatrunner.service.MusicService
import com.barad.beatrunner.service.SensorService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView

class MainVM(
        application: Application,
        musicDao: MusicDao,
        musicStore: MusicStore
) : ViewModel() {
    lateinit var sensorService: SensorService
    val player: SimpleExoPlayer = SimpleExoPlayer.Builder(application).build()
    //val musicService = MusicService(musicDao)

    init{
        //val musicEventListener = MusicEventListener(player, musicService, musicDao)
       // player.addListener(musicEventListener)
    }

}