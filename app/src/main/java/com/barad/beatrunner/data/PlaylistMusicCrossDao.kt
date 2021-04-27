package com.barad.beatrunner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.barad.beatrunner.models.PlaylistMusicCrossRef
import com.barad.beatrunner.models.PlaylistWithMusics

@Dao
interface PlaylistMusicCrossDao  {
    @Insert
    suspend fun insert(playlistMusicCrossRef: PlaylistMusicCrossRef)

    @Insert
    suspend fun insertAll(vararg playlistMusicCrossRef: PlaylistMusicCrossRef)

    @Transaction
    @Query("SELECT * FROM playlist")
    fun getPlaylistsWithMusics(): List<PlaylistWithMusics>

    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistId LIKE :id")
    fun getPlaylistByIdWithMusics(id: Int): PlaylistWithMusics

    @Query("DELETE FROM playlist_music_cross_ref WHERE playlistId LIKE :playlistId AND musicId LIKE :musicId")
    fun deleteById(playlistId: Int, musicId: Int)
}