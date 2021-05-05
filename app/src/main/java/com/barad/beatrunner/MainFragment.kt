package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.playlistlist.PlaylistListAdapter
import com.barad.beatrunner.service.ForegroundService
import com.google.android.exoplayer2.ui.PlayerControlView

class MainFragment : Fragment() {

    private var foregroundService: ForegroundService? = null

    private lateinit var viewModel: MainVM

    private lateinit var playerView: PlayerControlView

    private lateinit var btnTempo: Button
    private lateinit var btnStartLog: Button
    private lateinit var btnStopLog: Button
    private lateinit var tv_title: TextView
    private lateinit var tvTempo: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvTempo2: TextView
    private lateinit var tvSteps2: TextView
    private lateinit var btnResetSteps: Button
    private lateinit var tvMusicTempo: TextView
    private lateinit var inputTempo: EditText
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchAdaptiveTempo: Switch

    private lateinit var application: Application

    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is ForegroundService.MusicServiceBinder) {
                playerView.player = service.getPlayerInstance()
                foregroundService = service.service

                foregroundService?.steps?.observe(viewLifecycleOwner, {
                    tvSteps.setText(it.toString())
                })

                foregroundService?.sensorTempo?.observe(viewLifecycleOwner, {
                    tvTempo.setText(it.toString())
                })

                foregroundService?.gyroSteps?.observe(viewLifecycleOwner, {
                    tvSteps2.setText(it.toString())
                })

                foregroundService?.gyroSensorTempo?.observe(viewLifecycleOwner, {
                    tvTempo2.setText(it.toString())
                })

                foregroundService?.currentMusic?.observe(viewLifecycleOwner, {
                    tv_title.setText("${it.artist} - ${it.title}")
                    tvMusicTempo.setText(it.tempo.toString())
                })
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        application = requireNotNull(this.activity).application

        playerView = view.findViewById(R.id.player_view)
        playerView.showShuffleButton = true
        playerView.setShowFastForwardButton(false)
        playerView.setShowRewindButton(false)
        playerView.showTimeoutMs = 0

        val mainVMFactory = MainVMFactory(application)

        viewModel = ViewModelProvider(this, mainVMFactory).get(MainVM::class.java)

        btnTempo = view.findViewById(R.id.btn_setTempo)

        tv_title = view.findViewById(R.id.tv_title)
        tvTempo = view.findViewById(R.id.tv_tempoLabel)
        tvSteps = view.findViewById(R.id.tv_stepsLabel)
        tvTempo2 = view.findViewById(R.id.tv_tempo2Label)
        tvSteps2 = view.findViewById(R.id.tv_steps2Label)
        btnResetSteps = view.findViewById(R.id.btnResetSteps)
        tvMusicTempo = view.findViewById(R.id.tv_music_tempo)
        inputTempo = view.findViewById(R.id.et_tempo)
        switchAdaptiveTempo = view.findViewById(R.id.switchAllowTempoChange)
        btnStartLog = view.findViewById(R.id.btnStartLog)
        btnStopLog = view.findViewById(R.id.btnStopLog)


        val playlistAdapter = PlaylistListAdapter(
                { playlist -> adapterOnClick(playlist) },
                { playlist -> onButtonClick(playlist) },
                "Play"
        )

        val recyclerView: RecyclerView = view.findViewById(R.id.main_playlist_recycle_view)
        recyclerView.adapter = playlistAdapter

        viewModel.playlists.observe(viewLifecycleOwner, {
            it?.let { list ->
                playlistAdapter.submitList(list as MutableList<Playlist>)
            }
        })

        viewModel.playlistWithSongs.observe(viewLifecycleOwner,{
            foregroundService?.changePlaylist(it.songs)
        })


        requireActivity().startService(Intent(requireActivity(), ForegroundService::class.java))
        requireActivity().bindService(Intent(requireActivity(),
                ForegroundService::class.java), connection, Context.BIND_AUTO_CREATE)

        btnTempo.setOnClickListener {
            inputTempo.text.toString().toFloatOrNull()?.let { it1 ->
                foregroundService?.onTempoChangeFromUi(it1)
            }
        }

        btnResetSteps.setOnClickListener {
            foregroundService?.resetSteps()
        }

        btnStartLog.setOnClickListener {
            foregroundService?.startTimer()
        }

        btnStopLog.setOnClickListener {
            foregroundService?.stopTimer()
        }

        switchAdaptiveTempo.setOnCheckedChangeListener { _, isChecked ->
            foregroundService?.ALLOW_TEMPO_CHANGE = isChecked
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_PlaylistListFragment)
        }
    }

    private fun adapterOnClick(playlist: Playlist) {
        //findNavController().navigate(PlaylistListFragmentDirections.actionPlaylistListFragmentToPlaylistDetailFragment(playlist))
    }

    private fun onButtonClick(playlist: Playlist) {
        viewModel.getAllSongForPlaylist(playlist.playlistId)
    }
}