package com.barad.beatrunner.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.models.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MusicStore {

    init {
        System.loadLibrary("TempoFetcher")
        tempoFetcher()
    }

    private val _musicList = MutableLiveData<List<Music>>()
    val musicList
        get() = _musicList

    private val _fetchFinished = MutableLiveData<Boolean>()
    val fetchFinished
        get() = _fetchFinished

    fun getAllMusicFromDevice(context: Context, fetchTempo: Boolean) {
        val musicList: ArrayList<Music> = arrayListOf()

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
            MediaStore.Audio.Media.DATA
        )
        val audioCursor: Cursor? = context.contentResolver.query(audioCollection, proj, null, null, null)
        if (audioCursor != null) {
            GlobalScope.launch {
            while (audioCursor.moveToNext()) {
                val music = Music(
                        id = audioCursor.getString(0),
                        title = audioCursor.getString(1),
                        artist = audioCursor.getString(2),
                        album = audioCursor.getString(3),
                        uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                audioCursor.getString(0).toLong()),
                        path = audioCursor.getString(5),
                        tempo = if (fetchTempo) fetchTempo(audioCursor.getString(5)) else -1f)

                    musicList.add(music)
                    _musicList.postValue(musicList)
                }
            }.invokeOnCompletion { _fetchFinished.postValue(true) }
        }
    }

    private fun fetchTempo(path: String): Float {
            val tempo = decode(path)
            Log.d("barad-log-musicStore-fetchTempo() - tempo: ",tempo.toString())
            return tempo
    }

    private suspend fun fetchTempoAsync(path: String): Float {
        return GlobalScope.async(Dispatchers.Default) {
            val tempo = decode(path)
            Log.d("barad-log-musicStore-fetchTempoAsync() - tempo: ",tempo.toString())
            return@async tempo
        }.await()
    }

    private external fun tempoFetcher();
    private external fun decode(path: String): Float
}