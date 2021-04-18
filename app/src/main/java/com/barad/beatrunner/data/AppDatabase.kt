package com.barad.beatrunner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.models.PlaylistMusicCrossRef

@Database(entities = [Music::class, Playlist::class, PlaylistMusicCrossRef::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao() : MusicDao
    abstract fun playlistDao() : PlaylistDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "db1"
                    )
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}