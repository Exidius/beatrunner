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
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.playlistlist.PlaylistListAdapter
import com.barad.beatrunner.playlistlist.PlaylistListFragmentDirections
import com.barad.beatrunner.service.MusicService
import com.google.android.exoplayer2.ui.PlayerControlView

class MainFragment : Fragment() {

    private var musicService: MusicService? = null

    private lateinit var viewModel: MainVM

    private lateinit var playerView: PlayerControlView

    private lateinit var btnTempo: Button
    private lateinit var tv_title: TextView
    private lateinit var tvTempo: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvMusicTempo: TextView
    private lateinit var inputTempo: EditText

    private lateinit var application: Application

    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicServiceBinder) {
                playerView.player = service.getPlayerInstance()
                musicService = service.service

                musicService?.steps?.observe(viewLifecycleOwner, {
                    tvTempo.setText(it.toString())
                })

                musicService?.sensorTempo?.observe(viewLifecycleOwner, {
                    tvSteps.setText(it.toString())
                })

                musicService?.currentMusic?.observe(viewLifecycleOwner, {
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

        if(isStoragePermissionGranted()) {

            playerView = view.findViewById(R.id.player_view)
            playerView.showShuffleButton = true
            playerView.setShowFastForwardButton(false)
            playerView.setShowRewindButton(false)
            playerView.showTimeoutMs = 0

            val mainVMFactory = MainVMFactory(application)

            viewModel = ViewModelProvider(this, mainVMFactory).get(MainVM::class.java)

            btnTempo = view.findViewById(R.id.btn_setTempo)

            tv_title = view.findViewById(R.id.tv_title)
            tvTempo = view.findViewById(R.id.tv_tempo)
            tvSteps = view.findViewById(R.id.tv_steps)
            tvMusicTempo = view.findViewById(R.id.tv_music_tempo)
            inputTempo = view.findViewById(R.id.et_tempo)


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

            requireActivity().startService(Intent(requireActivity(), MusicService::class.java))
            requireActivity().bindService(Intent(requireActivity(),
                    MusicService::class.java), connection, Context.BIND_AUTO_CREATE)

            btnTempo.setOnClickListener {
                inputTempo.text.toString().toFloatOrNull()?.let { it1 ->
                    musicService?.onTempoChangeFromUi(it1)
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllMusicFromDevice()

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_PlaylistListFragment)
        }
    }

    private fun adapterOnClick(playlist: Playlist) {
        //findNavController().navigate(PlaylistListFragmentDirections.actionPlaylistListFragmentToPlaylistDetailFragment(playlist))
    }

    private fun onButtonClick(playlist: Playlist) {
        viewModel.getAllSongForPlaylist(playlist.playlistId)
        viewModel.playlistWithSongs.value?.let { musicService?.changePlaylist(it.songs) }
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