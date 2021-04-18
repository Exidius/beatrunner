package com.barad.beatrunner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.barad.beatrunner.models.Music

@Dao
interface MusicDao {
    @Query("SELECT * FROM music")
    fun getAll(): List<Music>

    @Query("SELECT * FROM music WHERE musicId LIKE :id")
    fun getById(id: Int): Music

    @Query("DELETE FROM music WHERE musicId LIKE :id")
    fun deleteById(id: Int)

    @Insert
    suspend fun insertAll(vararg musics: Music)

    @Insert
    suspend fun insert(music: Music)
}