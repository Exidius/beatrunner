package com.barad.beatrunner.playlistlist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.MainVM
import com.barad.beatrunner.MainVMFactory
import com.barad.beatrunner.R
import com.barad.beatrunner.models.Playlist

class PlaylistListFragment : Fragment() {

    private lateinit var viewModel: PlaylistListVM

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_playlistlist, container, false)

        val viewModelFactory = PlaylistListVMFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, viewModelFactory).get(PlaylistListVM::class.java)

        val playlistAdapter = PlaylistListAdapter { playlist -> adapterOnClick(playlist) }

        val recyclerView: RecyclerView = view.findViewById(R.id.playlist_recycler_view)
        recyclerView.adapter = playlistAdapter

        viewModel.getAllPlaylist()

        viewModel.playlists.observe(viewLifecycleOwner, {
            it?.let { list ->
                list.forEach { Log.d("barad-pl", "${it.playlistId} - ${it.name}") }
                playlistAdapter.submitList(list as MutableList<Playlist>)
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_PlaylistListFragment_to_MainFragment)
        }

        view.findViewById<Button>(R.id.btnCreateNewList).setOnClickListener {
            //findNavController().navigate(R.id.action_PlaylistListFragment_to_PlaylistDetailFragment)
            viewModel.insertEmptyPlaylist()
        }
    }

    private fun adapterOnClick(playlist: Playlist) {
        findNavController().navigate(PlaylistListFragmentDirections.actionPlaylistListFragmentToPlaylistDetailFragment(playlist))
    }
}

