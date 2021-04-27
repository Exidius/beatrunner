package com.barad.beatrunner.data

import androidx.room.*
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistWithMusics

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist")
    fun getAll(): List<Playlist>

    @Query("SELECT * FROM playlist WHERE playlistId LIKE :id")
    fun getById(id: Int): Playlist

    @Query("DELETE FROM playlist WHERE playlistId LIKE :id")
    fun deleteById(id: Int)

    @Update
    fun updateSong(playlist: Playlist);

    @Insert
    suspend fun insertAll(vararg playlist: Playlist)

    @Insert
    suspend fun insert(playlist: Playlist)

    @Transaction
    @Query("SELECT * FROM playlist")
    fun getPlaylistsWithMusics(): List<PlaylistWithMusics>

    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistId LIKE :id")
    fun getPlaylistByIdWithMusics(id: Int): PlaylistWithMusics
}