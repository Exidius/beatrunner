package com.barad.beatrunner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.barad.beatrunner.models.Music

@Database(entities = [Music::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao() : MusicDao
}