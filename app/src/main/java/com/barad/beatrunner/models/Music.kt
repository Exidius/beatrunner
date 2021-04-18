package com.barad.beatrunner.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music")
data class Music(
    @PrimaryKey(autoGenerate = true)
    var musicId: Int,

    var title: String,

    var artist: String,

    var album: String,

    var uri: String,

    var path: String,

    var tempo: Float) {
    override fun toString(): String = "$artist - $title t: $tempo at: $uri | $path id: $musicId"
}