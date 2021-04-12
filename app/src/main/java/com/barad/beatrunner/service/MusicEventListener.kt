package com.barad.beatrunner.service

import android.util.Log
import androidx.annotation.Nullable
import com.barad.beatrunner.data.MusicDao
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.MediaItemTransitionReason
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MusicEventListener(private val player: SimpleExoPlayer,
                         private val musicService: MusicService,
                         private val musicDao: MusicDao) : Player.EventListener {

    override fun onMediaItemTransition(@Nullable mediaItem: MediaItem?, @MediaItemTransitionReason reason: Int) {
        if (mediaItem != null) {
            GlobalScope.launch {
                musicService.currentMusic.postValue(musicDao.getById(mediaItem.mediaId))
            }
        }
        player.prepare()
        player.seekTo(15000)
        player.play()
    }
}