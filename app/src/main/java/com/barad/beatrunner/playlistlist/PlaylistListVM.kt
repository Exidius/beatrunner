package com.barad.beatrunner.playlistlist

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.PlaylistDao
import com.barad.beatrunner.models.Playlist
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlaylistListVM(private val playlistDao: PlaylistDao) : ViewModel() {

    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists
        get() = _playlists

    fun getAllPlaylist() {
        GlobalScope.launch { playlists.postValue(playlistDao.getAll())}
    }

    fun insertEmptyPlaylist() {
        GlobalScope.launch {
            playlistDao.insert(Playlist(name = "NewPlaylist"))
            playlists.postValue(playlistDao.getAll())
        }
    }
}

class PlaylistListVMFactory(
private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistListVM::class.java)) {
            return PlaylistListVM(AppDatabase.getInstance(context).playlistDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}