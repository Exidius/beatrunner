package com.barad.beatrunner

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistWithMusics
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

    private val playlistDao = AppDatabase.getInstance(application).playlistDao()
    private val playlistMusicDao = AppDatabase.getInstance(application).playlistMusicDao()

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