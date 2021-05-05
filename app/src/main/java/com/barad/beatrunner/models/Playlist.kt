package com.barad.beatrunner.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "playlist")
@Parcelize
data class Playlist (
    @PrimaryKey(autoGenerate = true)
    var playlistId: Int = 0,

    var name: String = "Playlist",

) : Parcelable