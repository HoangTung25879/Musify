package com.example.musify.MusicList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musify.Music
import com.example.musify.R

class MusicListAdapter():ListAdapter<Music,MusicListAdapter.ViewHolder>(MusicListDiffUtilCallback()){

    interface MusicListAdapterListener{
        fun onClickItem(music: Music)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val music = getItem(position)
        holder.bind(music)
    }

    class ViewHolder(val view:View):RecyclerView.ViewHolder(view){
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvArtist = view.findViewById<TextView>(R.id.tvArtist)
        val ivMusic = view.findViewById<ImageView>(R.id.ivMusic)
        fun bind(music:Music){
            tvName.text = music.name
            tvArtist.text = music.artist
//            Glide.with(view.context).load.into
        }
    }
    class MusicListDiffUtilCallback:DiffUtil.ItemCallback<Music>(){
        override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem == newItem
        }
    }
}