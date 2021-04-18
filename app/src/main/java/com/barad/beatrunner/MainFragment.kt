package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.barad.beatrunner.data.AppDatabase
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.service.MusicEventListener
import com.barad.beatrunner.service.MusicService
import com.barad.beatrunner.service.SensorService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView

class MainFragment : Fragment() {

    private lateinit var sensorService: SensorService

    private lateinit var viewModel: MainVM
    private lateinit var playerView: PlayerControlView

    private lateinit var btnTempo: Button
    private lateinit var tv_title: TextView
    private lateinit var tvTempo: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvMusicTempo: TextView
    private lateinit var inputTempo: EditText

    private lateinit var application: Application

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        application = requireNotNull(this.activity).application
        if(isStoragePermissionGranted()) {

            sensorService = SensorService(application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)

            val musicDao = AppDatabase.getInstance(application).musicDao()
            val musicStore = MusicStore(application, musicDao)
            val mainVMFactory = MainVMFactory(application, musicDao, musicStore)

            musicStore.getAllMusicFromDevice(true)

            viewModel = ViewModelProvider(this, mainVMFactory).get(MainVM::class.java)
            viewModel.sensorService = sensorService

            playerView = view.findViewById(R.id.player_view)
            playerView.showShuffleButton = true
            playerView.setShowFastForwardButton(false)
            playerView.setShowRewindButton(false)
            playerView.showTimeoutMs = 0
            playerView.player = viewModel.player

            btnTempo = view.findViewById(R.id.btn_setTempo)

            tv_title = view.findViewById(R.id.tv_title)
            tvTempo = view.findViewById(R.id.tv_tempo)
            tvSteps = view.findViewById(R.id.tv_steps)
            tvMusicTempo = view.findViewById(R.id.tv_music_tempo)
            inputTempo = view.findViewById(R.id.et_tempo)

            btnTempo.setOnClickListener {
                inputTempo.text.toString().toFloatOrNull()?.let { it1 ->
                    viewModel.musicService.onTempoChange(it1)
                }
                tvTempo.setText(inputTempo.text.toString())
            }

            viewModel.sensorService.steps.observe(viewLifecycleOwner, {
                tvTempo.setText(it.toString())
            })

            viewModel.sensorService.tempo.observe(viewLifecycleOwner, {
                tvSteps.setText(it.toString())
                viewModel.musicService.onTempoChange(it)
            })

            viewModel.musicService.currentMusic.observe(viewLifecycleOwner, {
                tv_title.setText("${it.artist} - ${it.title}")
                tvMusicTempo.setText(it.tempo.toString())
            })
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        sensorService.register()
    }

    override fun onStop() {
        super.onStop()
        sensorService.unregister()
    }

    @SuppressLint("ObsoleteSdkInt")
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(application.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else { requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                )
                false
            }
        } else {
            true
        }
    }
}