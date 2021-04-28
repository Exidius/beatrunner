package com.barad.beatrunner

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.data.PlaylistDao
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistWithMusics
import com.barad.beatrunner.service.MusicService
import com.google.android.exoplayer2.ui.PlayerControlView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainVM(
        application: Application
) : ViewModel() {

    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists
        get() = _playlists

    private val _playlistWithSongs = MutableLiveData<PlaylistWithMusics>()
    val playlistWithSongs
        get() = _playlistWithSongs

    val playlistDao = AppDatabase.getInstance(application).playlistDao()
    val playlistMusicDao = AppDatabase.getInstance(application).playlistMusicDao()
    val musicDao = AppDatabase.getInstance(application).musicDao()

    init{
        getAllPlaylist()
    }

    fun getAllPlaylist() {
        GlobalScope.launch {
        val asd = playlistDao.getAll()
            playlists.postValue(asd)
        }
    }

    fun getAllSongForPlaylist(playlistId: Int) {
        GlobalScope.launch {
            playlistWithSongs.postValue(playlistMusicDao.getPlaylistByIdWithMusics(playlistId))
        }
    }
}

class MainVMFactory(
        private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainVM::class.java)) {
            return MainVM(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}