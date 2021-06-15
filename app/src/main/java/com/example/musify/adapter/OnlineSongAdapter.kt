package com.example.musify.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.musify.R
import com.example.musify.data.entities.Song
import com.example.musify.durationFormat
import com.wang.avi.AVLoadingIndicatorView
import javax.inject.Inject

private const val TAG = "ONLINESONGADAPTER"
class OnlineSongAdapter  @Inject constructor(
    private val glide : RequestManager
) : ListAdapter<Song, OnlineSongAdapter.SongViewHolder>(SongAdapterDiffUtilCallback()){

    var listener: SongAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item,parent,false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song,glide,listener)
    }

    class SongViewHolder (itemView: View): RecyclerView.ViewHolder(itemView){


        private val tvName = itemView.findViewById<TextView>(R.id.tvSongName)
        private val tvArtist = itemView.findViewById<TextView>(R.id.tvSongArtist)
        private val tvDuration = itemView.findViewById<TextView>(R.id.tvSongDuration)
        private val ivIsPlaying = itemView.findViewById<AVLoadingIndicatorView>(R.id.ivIsPlaying)
        private val ivSongImage = itemView.findViewById<ImageView>(R.id.ivItemImage)
        private val ivOptionMenu = itemView.findViewById<ImageView>(R.id.ivOptionMenu)

        fun bind(song: Song,glide: RequestManager,listener: SongAdapterListener?){
            itemView.setOnClickListener {
                listener?.onItemClicked(song)
            }
            ivOptionMenu.setOnClickListener {
                listener?.onMenuClicked(song,ivOptionMenu)
            }
            tvName.text = song.title
            tvArtist.text = song.subtitle
            tvDuration.text = durationFormat(song.duration)
            if (song.isPlaying) ivIsPlaying.smoothToShow() else ivIsPlaying.smoothToHide()
//            glide.load(if(song.imageUrl == "") R.drawable.music else song.imageUrl).into(ivSongImage)
            glide.load(R.drawable.music).into(ivSongImage)
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
        fun onMenuClicked(song: Song,view:View)
    }
}