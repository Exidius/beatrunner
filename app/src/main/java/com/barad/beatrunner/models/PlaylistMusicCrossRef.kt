package com.barad.beatrunner.models

import androidx.room.*

@Entity(primaryKeys = ["playlistId", "musicId"])
data class PlaylistMusicCrossRef (
        val playlistId: Int,
        val musicId: Int
)

data class PlaylistWithMusics(
        @Embedded val playlist: Playlist,
        @Relation(
                parentColumn = "playlistId",
                entityColumn = "musicId",
                associateBy = Junction(PlaylistMusicCrossRef::class)
        )
        val songs: List<Music>
)