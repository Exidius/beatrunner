package com.barad.beatrunner.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music")
data class Music(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int,
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "artist")
    var artist: String,
    @ColumnInfo(name = "album")
    var album: String,
    @ColumnInfo(name = "uri")
    var uri: String,
    @ColumnInfo(name = "path")
    var path: String,
    @ColumnInfo(name = "tempo")
    var tempo: Float) {
    override fun toString(): String = "$artist - $title t: $tempo at: $uri | $path id: $id"
}