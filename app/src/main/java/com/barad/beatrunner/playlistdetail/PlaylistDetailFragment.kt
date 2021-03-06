package com.barad.beatrunner.playlistdetail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.R
import com.barad.beatrunner.models.Music

class PlaylistDetailFragment : Fragment() {

    private lateinit var viewModel: PlaylistDetailVM

    private lateinit var etName: EditText

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_playlistdetail, container, false)

        val args: PlaylistDetailFragmentArgs by navArgs()

        val viewModelFactory = PlaylistDetailVMFactory(requireActivity().application, args.playlist)
        viewModel = ViewModelProvider(this, viewModelFactory).get(PlaylistDetailVM::class.java)

        etName = view.findViewById(R.id.etPlaylistName)

        val musicAdapter = MusicListAdapter(
            { music -> adapterOnClick(music) },
            { music -> onButtonClick(music) },
            "Remove"
        )

        val recyclerView: RecyclerView = view.findViewById(R.id.songs_recycler_view)
        recyclerView.adapter = musicAdapter

        viewModel.playlistWithSongs.observe(viewLifecycleOwner, { songs ->
            songs?.let { it ->
                it.songs.forEach { Log.d("barad-asd", it.toString()) }
                musicAdapter.submitList(it.songs as MutableList<Music>)
            }
        })

        viewModel.getAllSongForPlaylist(args.playlist.playlistId)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.playlist.observe(viewLifecycleOwner, {
            etName.setText(it.name)
        })

        etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.playlist.value?.name = s.toString()
                viewModel.savePlaylist()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
        
        view.findViewById<Button>(R.id.btnPlaylistDelete).setOnClickListener {
            viewModel.removePlaylist()
            findNavController().navigate(R.id.action_PlaylistDetailFragment_to_PlaylistListFragment)
        }

        view.findViewById<Button>(R.id.btnToAddSong).setOnClickListener {
           findNavController().navigate(PlaylistDetailFragmentDirections.actionPlaylistDetailFragmentToAddSongToPlaylistFragment(viewModel.playlist.value!!))
        }

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.action_PlaylistDetailFragment_to_PlaylistListFragment)
        }
    }

    private fun adapterOnClick(music: Music) {
        //findNavController().navigate(PlaylistListFragmentDirections.actionPlaylistListFragmentToPlaylistDetailFragment())
    }

    private fun onButtonClick(music: Music) {
        viewModel.removeSong(music.musicId)
    }
}