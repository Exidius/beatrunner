package com.barad.beatrunner.playlistdetail

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.PlaylistDao
import com.barad.beatrunner.data.PlaylistMusicCrossDao
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistMusicCrossRef
import com.barad.beatrunner.models.PlaylistWithMusics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlaylistDetailVM(
        private val playlistDao: PlaylistDao,
        private val playlistMusicDao: PlaylistMusicCrossDao,
        private val playlist_: Playlist
) : ViewModel() {
    private val _playlist = MutableLiveData<Playlist>()
    val playlist
        get() = _playlist

    private val _playlistWithSongs = MutableLiveData<PlaylistWithMusics>()
    val playlistWithSongs
        get() = _playlistWithSongs

    init {
        _playlist.value = playlist_
    }

    fun insertCrossEntity() {
        GlobalScope.launch {
            //playlistMusicDao.insert(PlaylistMusicCrossRef(1, 3233))
            playlistMusicDao.insert(PlaylistMusicCrossRef(2, 4459))
            playlistMusicDao.insert(PlaylistMusicCrossRef(3, 4460))
            playlistMusicDao.insert(PlaylistMusicCrossRef(4, 4461))
            playlistMusicDao.insert(PlaylistMusicCrossRef(5, 4462))
        }
    }

    fun removePlaylist() {
        GlobalScope.launch {
            playlist.value?.let { playlistDao.deleteById(it.playlistId) }
        }
    }

    fun removeSong(musicId: Int) {
        GlobalScope.launch {
            playlist.value?.playlistId?.let { playlistMusicDao.deleteById(it, musicId) }
            playlistWithSongs.postValue(playlist.value?.playlistId?.let {
                playlistMusicDao.getPlaylistByIdWithMusics(
                    it
                )
            })
        }
    }

    fun getAllSongForPlaylist(playlistId: Int) {
        GlobalScope.launch {
            playlistWithSongs.postValue(playlistMusicDao.getPlaylistByIdWithMusics(playlistId))
        }
    }

    fun savePlaylist() {
        playlist.value?.name?.let { Log.d("barad-asdvm", it) }
        playlist.value?.playlistId?.let { Log.d("barad-asdvm", it.toString()) }
        GlobalScope.launch { playlist.value?.let { playlistDao.updateSong(it) } }
    }
}

class PlaylistDetailVMFactory(
    private val context: Context,
    private val playlist_: Playlist
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistDetailVM::class.java)) {
            return PlaylistDetailVM(AppDatabase.getInstance(context).playlistDao(), AppDatabase.getInstance(context).playlistMusicDao(), playlist_) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}