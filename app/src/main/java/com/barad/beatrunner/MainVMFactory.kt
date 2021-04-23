package com.barad.beatrunner

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ui.PlayerControlView

class MainVMFactory(
        private val application: Application,
        private val playerView: PlayerControlView
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainVM::class.java)) {
            return MainVM(application, playerView) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}