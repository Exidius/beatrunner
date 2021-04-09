package com.barad.beatrunner.service

import android.util.Log
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

    fun onTempoChange(tempo: Float) {
        var musicList = listOf<Music>()

        GlobalScope.launch(Dispatchers.Default) {
            musicList = musicDao.getAll()
        }.invokeOnCompletion {
            var closestTempo = 0f
            musicList.forEach {
                if (abs(it.tempo-tempo) < abs(closestTempo-tempo)) {
                    closestTempo = it.tempo
                    mediaItem = MediaItem.fromUri(it.uri)
                }
            }
        }

        if (player.currentMediaItem != mediaItem) {
            mediaItem?.let { player.setMediaItem(it) }
            player.prepare()
            player.play()
        }
    }
}