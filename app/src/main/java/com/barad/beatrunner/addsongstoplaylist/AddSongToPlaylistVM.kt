package com.barad.beatrunner.addsongstoplaylist

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.PlaylistDao
import com.barad.beatrunner.data.PlaylistMusicCrossDao
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistMusicCrossRef
import com.barad.beatrunner.models.PlaylistWithMusics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddSongToPlaylistVM(
    private val playlist: Playlist,
    private val musicDao: MusicDao,
    private val playlistMusicDao: PlaylistMusicCrossDao
) : ViewModel() {

    private val _musicList = MutableLiveData<List<Music>>()
    val musicList
        get() = _musicList

    init {
        getAllSong()
    }

    fun insertCrossEntity(musicId: Int) {
        GlobalScope.launch {
            try {
                playlistMusicDao.insert(PlaylistMusicCrossRef(playlist.playlistId, musicId))
            } catch (e: Exception) {
                Log.d("exception", e.toString())
            }
        }.invokeOnCompletion {
            val playlistSongs = playlistMusicDao.getPlaylistByIdWithMusics(playlist.playlistId).songs
            val allSongs =  musicDao.getAll()
            val filteredResult = allSongs.filter { x -> !playlistSongs.contains(x) }
            musicList.postValue(filteredResult)
        }
    }

    fun getAllSong() {
        GlobalScope.launch {
            val playlistSongs = playlistMusicDao.getPlaylistByIdWithMusics(playlist.playlistId).songs
            val allSongs =  musicDao.getAll()
            val filteredResult = allSongs.filter { x -> !playlistSongs.contains(x) }
            musicList.postValue(filteredResult)
        }
    }
}

class AddSongToPlaylistVMFactory(
    private val context: Context,
    private val playlist: Playlist
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddSongToPlaylistVM::class.java)) {
            return AddSongToPlaylistVM(
                playlist,
                AppDatabase.getInstance(context).musicDao(),
                AppDatabase.getInstance(context).playlistMusicDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}