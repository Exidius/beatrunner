package com.barad.beatrunner.playlistdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barad.beatrunner.R
import com.barad.beatrunner.models.Music
import java.util.*
import kotlin.collections.ArrayList

class MusicListAdapter(private val onClick: (Music) -> Unit, private val onButtonClick: (Music) -> Unit, private val btnText: String):
        ListAdapter<Music, MusicListAdapter.MusicViewHolder>(MusicDiffCallback), Filterable {

    var filterList: List<Music> = currentList.toList()
    var originalList = currentList.toList()

    /* ViewHolder for Music, takes in the inflated view and the onClick behavior. */
    class MusicViewHolder(itemView: View, val onClick: (Music) -> Unit, val onButtonClick: (Music)-> Unit, val btnText: String) :
            RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvSongTitle)
        private val artistTextView: TextView = itemView.findViewById(R.id.tvSongArtist)
        private val tempoTextView: TextView = itemView.findViewById(R.id.tvSongTempo)
        private val multiButton: Button = itemView.findViewById(R.id.btnSongMulti)

        private var currentMusic: Music? = null

        init {
            itemView.setOnClickListener {
                currentMusic?.let {
                    onClick(it)
                }
            }
            multiButton.setOnClickListener {
                currentMusic?.let {
                    onButtonClick(it)
                }
            }

        }

        /* Bind music name and image. */
        fun bind(music: Music) {
            currentMusic = music

            titleTextView.text = music.title
            artistTextView.text = music.artist
            tempoTextView.text = music.tempo.toString()
            if (btnText.isNullOrBlank()) { multiButton.visibility = View.GONE }
            else { multiButton.text = btnText }

        }
    }

    /* Creates and inflates view and return MusicViewHolder. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.music_item, parent, false)

        return MusicViewHolder(view, onClick, onButtonClick, btnText)
    }

    /* Gets current music and uses it to bind view. */
    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = getItem(position)
        holder.bind(music)
    }

    override fun getFilter(): Filter {
        if(originalList.isEmpty()) { originalList = currentList.toList() }

        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchText = constraint.toString()
                filterList = if (searchText.isNullOrBlank()) {
                    originalList
                } else {
                    val resultList = ArrayList<Music>()
                    for (item in originalList) {
                        if (item.title.toLowerCase(Locale.ROOT)
                                .contains(searchText.toLowerCase(Locale.ROOT)) ||
                            item.artist.toLowerCase(Locale.ROOT)
                                .contains(searchText.toLowerCase(Locale.ROOT))
                        ) {
                            resultList.add(item)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = filterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                submitList(results?.values as ArrayList<Music>)
                notifyDataSetChanged()
            }
        }
    }
}

object MusicDiffCallback : DiffUtil.ItemCallback<Music>() {
    override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
        return oldItem.musicId == newItem.musicId
    }
}