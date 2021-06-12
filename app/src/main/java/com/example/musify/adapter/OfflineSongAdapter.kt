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
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

private const val TAG = "OFFLINESONGADAPTER"
class OfflineSongAdapter  @Inject constructor(
    private val glide : RequestManager
) : ListAdapter<Song, OfflineSongAdapter.SongViewHolder>(SongAdapterDiffUtilCallback()){

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
        private val ivSongImage = itemView.findViewById<CircleImageView>(R.id.ivItemImage)
        private val ivDownload = itemView.findViewById<ImageView>(R.id.ivDownload)


        fun bind(song: Song,glide: RequestManager,listener: SongAdapterListener?){
            itemView.setOnClickListener {
                listener?.onItemClicked(song)
            }
            ivDownload.setOnClickListener {
                listener?.onUploadClicked(song)
            }
            tvName.text = song.title
            tvArtist.text = song.subtitle
            tvDuration.text = durationFormat(song.duration)
            if (song.isPlaying) ivIsPlaying.smoothToShow() else ivIsPlaying.smoothToHide()
            glide.load(song.imageUrl).into(ivSongImage)
            glide.load(R.drawable.ic_baseline_cloud_upload_24).into(ivDownload)
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
        fun onUploadClicked(song: Song)
    }
}