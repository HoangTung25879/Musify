package com.example.musify.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.example.musify.data.entities.Song

//add function to class
fun MediaMetadataCompat.toSong():Song?{
    return description?.let {
        Song(
            it.mediaId ?: "",
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString()
        )
    }
}