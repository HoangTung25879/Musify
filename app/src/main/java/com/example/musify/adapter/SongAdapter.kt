package com.example.musify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.musify.R
import com.example.musify.data.entities.Song
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide : RequestManager
):RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item,parent,false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            findViewById<TextView>(R.id.tvPrimary).text = song.title
            findViewById<TextView>(R.id.tvSecondary).text = song.subtitle
            glide.load(song.imageUrl).into(findViewById(R.id.ivItemImage))

            setOnClickListener{
                onItemClickListener?.let { click->
                    click(song)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    private var onItemClickListener: ((Song)->Unit)? = null

    fun setOnItemClickListener(listener: (Song)->Unit){
        onItemClickListener = listener
    }
    class SongViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

    }
    private val SongAdapterDiffUtilCallback = object : DiffUtil.ItemCallback<Song>(){
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)
    var songs : List<Song>
        get() = differ.currentList
        set(value) = differ.submitList(value)
}