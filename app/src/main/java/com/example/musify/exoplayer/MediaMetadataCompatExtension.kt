package com.example.musify.exoplayer

import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.musify.data.Constants
import com.example.musify.data.Constants.IS_LOCAL
import com.example.musify.data.entities.Song

//add function to class
fun MediaMetadataCompat.toSong():Song?{
    return description?.let {
        Song(
            it.mediaId ?: "",
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString(),
                isLocal =  getString(IS_LOCAL).toBoolean()
        )
    }
}