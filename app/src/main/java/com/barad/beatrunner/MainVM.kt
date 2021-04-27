package com.barad.beatrunner

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.service.MusicService
import com.google.android.exoplayer2.ui.PlayerControlView

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

class MainVMFactory(
        private val application: Application,
        private val playerView: PlayerControlView
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainVM::class.java)) {
            return MainVM(application, playerView) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}