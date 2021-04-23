package com.barad.beatrunner

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.service.MusicEventListener
import com.barad.beatrunner.service.MusicService
import com.barad.beatrunner.service.SensorService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView

class MainVM(
        application: Application,
        playerView: PlayerControlView
) : ViewModel() {

    private val _sensorTempo = MutableLiveData<Float>()
    val sensorTempo
        get() = _sensorTempo

    private val _currentMusic = MutableLiveData<Music>()
    val currentMusic
        get() = _currentMusic

    private val _steps = MutableLiveData<Int>()
    val steps
        get() = _steps

    private var _musicService = MutableLiveData<MusicService>()
    val musicService
        get() = _musicService

    //var sensorService: SensorService = SensorService(application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)


    val musicDao = AppDatabase.getInstance(application).musicDao()
    val musicStore = MusicStore(application, musicDao)

    init{
        //sensorService.register()
    }

    fun getAllMusicFromDevice() {
        musicStore.getAllMusicFromDevice(true)

    }


}