package com.barad.beatrunner.models

import android.net.Uri

data class Music(
    var id: String,
    var title: String,
    var artist: String,
    var album: String,
    var uri: Uri) {
    override fun toString(): String = title + " - " + artist + " / " + uri
}