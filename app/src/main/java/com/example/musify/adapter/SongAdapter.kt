package com.example.musify.adapter

import android.util.Log
import androidx.recyclerview.widget.AsyncListDiffer
import com.bumptech.glide.RequestManager
import com.example.musify.Config
import com.example.musify.R
import kotlinx.android.synthetic.main.list_item.view.*
import javax.inject.Inject

private const val TAG = "SONGADAPTER"
class SongAdapter @Inject constructor(
    private val glide : RequestManager
):BaseSongAdapter(R.layout.list_item) {

    override val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            tvPrimary.text = song.title
            tvSecondary.text = song.subtitle
            ivIsPlaying.hide()
//            glide.load(if(song.imageUrl == "") R.drawable.albumart else song.imageUrl).into(ivItemImage)
            glide.load(R.drawable.music).into(ivItemImage)
            setOnClickListener{
                Config.isInitial = false
                onItemClickListener?.let { click->
                    click(song)
                }
            }
        }
    }


}