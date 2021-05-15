package com.example.musify.adapter

import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.example.musify.R
import kotlinx.android.synthetic.main.swipe_item.view.*

class SwipeSongAdapter :BaseSongAdapter(R.layout.swipe_item) {

    override val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            val text = "${song.title} - ${song.subtitle}"
            tvPrimary.text = text

            setOnClickListener{
                onItemClickListener?.let { click->
                    click(song)
                }
            }
        }
    }

}