package com.barad.beatrunner.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist (
    @PrimaryKey(autoGenerate = true)

    var playlistId: Int,

    var name: String,

)