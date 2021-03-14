package com.barad.beatrunner.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.barad.beatrunner.models.Music

class MusicStore {
    fun getAllMusicFromDevice(context: Context): List<Music> {
        var musicList: ArrayList<Music> = arrayListOf()

        val audioCollection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val proj: Array<String> = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            // Might be useful later: MediaStore.Audio.Media.RELATIVE_PATH

        )
        val audioCursor: Cursor? = context.contentResolver.query(audioCollection, proj, null, null, null)
        if (audioCursor != null) {
            while (audioCursor.moveToNext()) {
                val music = Music(
                id = audioCursor.getString(0),
                title = audioCursor.getString(1),
                artist = audioCursor.getString(2),
                album = audioCursor.getString(3),
                uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        audioCursor.getString(0).toLong()),
                path =  audioCursor.getString(5),
                tempo = -1.0f)

                musicList.add(music)

            }
        }

        return musicList.toList()
    }
}