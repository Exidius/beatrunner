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
import androidx.room.Room
import com.barad.beatrunner.models.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MusicStore(
    private val context: Context,
    private val musicDao: MusicDao) {

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

    private var _musicsInDb = listOf<Music>()

    fun getAllMusicFromDevice(fetchTempo: Boolean) {
        val musicList: ArrayList<Music> = arrayListOf()
        GlobalScope.launch {
            _musicsInDb = musicDao.getAll()
        }

        val audioCollection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
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
                        musicId = audioCursor.getString(0).toInt(),
                        title = audioCursor.getString(1),
                        artist = audioCursor.getString(2),
                        album = audioCursor.getString(3),
                        uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                audioCursor.getString(0).toLong()).toString(),
                        path = audioCursor.getString(5),
                        tempo = if (fetchTempo) fetchTempo(audioCursor.getString(5)) else -1f)

                    // Only save audio if it is longer than 30 seconds = 30000 miliseconds
                    // Todo: only analise bpm if its longer than 30 secs
                    if(audioCursor.getString(4).toLong() > 30000) {
                        musicList.add(music)
                        _musicList.postValue(musicList)
                        if (!_musicsInDb.contains(music)) {
                            musicDao.insert(music)
                        }
                    }
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