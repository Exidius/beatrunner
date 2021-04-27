package com.barad.beatrunner.models

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "music")
@Parcelize
data class Music(
    @PrimaryKey(autoGenerate = true)
    var musicId: Int,

    var title: String,

    var artist: String,

    var album: String,

    var uri: String,

    var path: String,

    var tempo: Float) : Parcelable {
    override fun toString(): String = "$artist - $title t: $tempo at: $uri | $path id: $musicId"
}