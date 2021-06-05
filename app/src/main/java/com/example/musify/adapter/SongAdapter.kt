package com.example.musify.adapter

import androidx.recyclerview.widget.AsyncListDiffer
import com.bumptech.glide.RequestManager
import com.example.musify.R
import kotlinx.android.synthetic.main.list_item.view.*
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide : RequestManager
):BaseSongAdapter(R.layout.list_item) {

    override val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{

            tvPrimary.text = song.title
            tvSecondary.text = song.subtitle
//            glide.load(if(song.imageUrl == "") R.drawable.albumart else song.imageUrl).into(ivItemImage)
            glide.load(R.drawable.music).into(ivItemImage)

            setOnClickListener{
                onItemClickListener?.let { click->
                    click(song)
                }
            }
        }
    }

}