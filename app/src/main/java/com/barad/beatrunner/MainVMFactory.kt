package com.barad.beatrunner

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.data.MusicStore

class MainVMFactory(
        private val application: Application,
        private val musicDao: MusicDao,
        private val musicStore: MusicStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainVM::class.java)) {
            return MainVM(application, musicDao, musicStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}