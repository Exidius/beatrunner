package com.barad.beatrunner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.barad.beatrunner.models.Music

@Dao
interface MusicDao {
    @Query("SELECT * FROM music")
    fun getAll(): List<Music>

    @Insert
    suspend fun insertAll(vararg musics: Music)

    @Insert
    suspend fun insert(music: Music)
}