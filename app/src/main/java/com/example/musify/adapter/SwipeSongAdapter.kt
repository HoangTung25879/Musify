package com.example.musify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musify.R
import com.example.musify.data.entities.Song
import com.google.android.material.textview.MaterialTextView

class SwipeSongAdapter : ListAdapter<Song, SwipeSongAdapter.SongViewHolder>(
    SongAdapterDiffUtilCallback()
) {

    var listener: SongAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.swipe_item,parent,false)
        return SongViewHolder(view)
    }

    fun getSong(position: Int):Song{
        return getItem(position)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song,listener)
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<MaterialTextView>(R.id.tvPrimary)
        fun bind(song: Song,listener: SongAdapterListener?){
            val text = "${song.title} - ${song.subtitle}"
            tvTitle.text = text
            tvTitle.isSelected = true
            itemView.setOnClickListener {
                listener?.onItemClicked(song)
            }
        }
    }
    class SongAdapterDiffUtilCallback: DiffUtil.ItemCallback<Song>(){
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }

    interface SongAdapterListener{
        fun onItemClicked(song:Song)
    }
}