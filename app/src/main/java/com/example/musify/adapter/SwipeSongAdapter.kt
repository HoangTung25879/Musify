package com.example.musify.adapter

import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.example.musify.R

class SwipeSongAdapter :BaseSongAdapter(R.layout.list_item) {

    override val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            val text = "${song.title} - ${song.subtitle}"
            findViewById<TextView>(R.id.tvPrimary).text = text

            setOnClickListener{
                onItemClickListener?.let { click->
                    click(song)
                }
            }
        }
    }

}