package com.barad.beatrunner.service

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class MusicService(
        private val musicDao: MusicDao,
        private val player: SimpleExoPlayer) {

    var mediaItem: MediaItem? = null
    var musicList = listOf<Music>()
    var lastTempo = 0f
    var currentMusicTempo = 0f

    private val _currentMusic = MutableLiveData<Music>()
    val currentMusic
        get() = _currentMusic

    init{
        currentMusic.value = Music(Int.MAX_VALUE,"Title","Artist","Album","URI","PATH",0f)
    }

    fun onTempoChange(tempo: Float) {
        if(abs(currentMusicTempo - tempo) > 20) {
            while (musicList.isEmpty()) {
                fetchMusicList()
            }
            selectNewMusic(tempo)
            switchMusic()
        }
        // Else modify current tempo
    }

    private fun fetchMusicList() {
        GlobalScope.launch(Dispatchers.Default) {
            musicList = musicDao.getAll()
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
        currentMusicTempo = closestTempo
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
}