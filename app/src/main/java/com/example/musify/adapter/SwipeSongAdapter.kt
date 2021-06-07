package com.example.musify.adapter

import androidx.recyclerview.widget.AsyncListDiffer
import com.example.musify.R
import kotlinx.android.synthetic.main.swipe_item.view.*

//Declare provider in AppModule or declare an empty constructor like this for dagger-hilt
//class SwipeSongAdapter @Inject constructor() :BaseSongAdapter(R.layout.swipe_item) {
class SwipeSongAdapter :BaseSongAdapter(R.layout.swipe_item) {

    override val differ = AsyncListDiffer(this,SongAdapterDiffUtilCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            val text = "${song.title} - ${song.subtitle}"
            tvPrimary.text = text

            setOnClickListener{
                listener?.onItemClicked(song)
            }
        }
    }

}