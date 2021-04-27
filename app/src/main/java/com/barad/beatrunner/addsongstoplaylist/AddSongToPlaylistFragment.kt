package com.barad.beatrunner.addsongstoplaylist

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
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.R
import com.barad.beatrunner.models.Music
import com.barad.beatrunner.models.Playlist
import com.barad.beatrunner.playlistdetail.MusicListAdapter
import com.barad.beatrunner.playlistdetail.PlaylistDetailFragmentArgs
import com.barad.beatrunner.playlistlist.PlaylistListAdapter
import com.barad.beatrunner.playlistlist.PlaylistListFragmentDirections
import com.barad.beatrunner.playlistlist.PlaylistListVM
import com.barad.beatrunner.playlistlist.PlaylistListVMFactory

class AddSongToPlaylistFragment : Fragment() {

    private lateinit var viewModel: AddSongToPlaylistVM

    private lateinit var etName: EditText

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_addsong, container, false)

        etName = view.findViewById(R.id.etSearchSong)

        val args: AddSongToPlaylistFragmentArgs by navArgs()

        val viewModelFactory = AddSongToPlaylistVMFactory(requireActivity().application, args.playlist)
        viewModel = ViewModelProvider(this, viewModelFactory).get(AddSongToPlaylistVM::class.java)

        val musicAdapter = MusicListAdapter(
            { music -> adapterOnClick(music) },
            { music -> onButtonClick(music) },
            "Add"
        )

        val recyclerView: RecyclerView = view.findViewById(R.id.add_songs_recycler_view)
        recyclerView.adapter = musicAdapter

        viewModel.musicList.observe(viewLifecycleOwner, { songs ->
            songs?.let { it ->
                musicAdapter.submitList(it as MutableList<Music>)
            }
        })

        //viewModel.insertCrossEntity()

        etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                musicAdapter.filter.filter(s)
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: AddSongToPlaylistFragmentArgs by navArgs()
        view.findViewById<Button>(R.id.btnAddSongBack).setOnClickListener {
            findNavController().navigate(AddSongToPlaylistFragmentDirections.actionAddSongToPlaylistFragmentToPlaylistDetailFragment(args.playlist))
        }
    }

    private fun adapterOnClick(music: Music) {
        //findNavController().navigate(PlaylistListFragmentDirections.actionPlaylistListFragmentToPlaylistDetailFragment())
    }

    private fun onButtonClick(music: Music) {
        viewModel.insertCrossEntity(music.musicId)
        viewModel.getAllSong()
    }
}