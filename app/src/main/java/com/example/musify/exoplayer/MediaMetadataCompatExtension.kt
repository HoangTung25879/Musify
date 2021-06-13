package com.example.musify.exoplayer

import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.musify.data.Constants
import com.example.musify.data.Constants.DURATION
import com.example.musify.data.Constants.IS_LOCAL
import com.example.musify.data.entities.Song

//add function to class
fun MediaMetadataCompat.toSong():Song?{
    return description?.let {
        Song(
            mediaId = it.mediaId ?: "",
            title = it.title.toString(),
            subtitle = it.subtitle.toString(),
            songUrl =  it.mediaUri.toString(),
            imageUrl =  (it.iconUri ?: "").toString(),
            isLocal =  getString(IS_LOCAL).toBoolean(),
            duration = getString(DURATION) ?: ""
        )
    }
}