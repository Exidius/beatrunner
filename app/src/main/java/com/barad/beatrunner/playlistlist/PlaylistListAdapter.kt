package com.barad.beatrunner.playlistlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.R
import com.barad.beatrunner.models.Playlist

class PlaylistListAdapter(private val onClick: (Playlist) -> Unit, private val onButtonClick: (Playlist) -> Unit, private val btnText: String) :
        ListAdapter<Playlist, PlaylistListAdapter.PlaylistViewHolder>(PlaylistDiffCallback) {

    /* ViewHolder for Playlist, takes in the inflated view and the onClick behavior. */
    class PlaylistViewHolder(itemView: View, val onClick: (Playlist) -> Unit, val onButtonClick: (Playlist)-> Unit, val btnText: String) :
            RecyclerView.ViewHolder(itemView) {
        private val playlistTextView: TextView = itemView.findViewById(R.id.playlistName)
        private var currentPlaylist: Playlist? = null
        private val multiButton: Button = itemView.findViewById(R.id.btnPlaylistMulti)

        init {
            itemView.setOnClickListener {
                currentPlaylist?.let {
                    onClick(it)
                }
            }
            multiButton.setOnClickListener {
                currentPlaylist?.let {
                    onButtonClick(it)
                }
            }
        }

        /* Bind playlist name and image. */
        fun bind(playlist: Playlist) {
            currentPlaylist = playlist

            playlistTextView.text = playlist.name
            if (btnText.isNullOrBlank()) { multiButton.visibility = View.GONE }
            else { multiButton.text = btnText }
        }
    }

    /* Creates and inflates view and return PlaylistViewHolder. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.playlist_item, parent, false)
        return PlaylistViewHolder(view, onClick, onButtonClick, btnText)
    }

    /* Gets current playlist and uses it to bind view. */
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist)

    }
}

object PlaylistDiffCallback : DiffUtil.ItemCallback<Playlist>() {
    override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
        return oldItem.playlistId == newItem.playlistId
    }
}