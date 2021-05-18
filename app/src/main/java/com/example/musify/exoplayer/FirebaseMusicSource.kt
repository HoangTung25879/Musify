package com.example.musify.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.util.Log
import androidx.core.net.toUri
import com.example.musify.data.MusicDatabase
import com.example.musify.exoplayer.State.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
){ // list of song get from firebase

    //song list of type MediaMetaDataCompat to use in music service (contain metadata of song)
    var songs = emptyList<MediaMetadataCompat>()
    var songsLocal = emptyList<MediaMetadataCompat>()

    suspend fun fetchMediaData() = withContext(Dispatchers.IO){
        state = STATE_INITIALIZING
        val allSong = musicDatabase.getAllSongs()
        songs = allSong.map { song->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID,song.mediaId)
                .putString(METADATA_KEY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subtitle)
                .build()
        }
        state = STATE_INITIALIZED
    }
    fun fetchMediaDataFromLocal(){
        val allSong = musicDatabase.getAllAudioFromDevice()
        songsLocal = allSong.map { song->
            MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_ARTIST,song.subtitle)
                    .putString(METADATA_KEY_MEDIA_ID,song.mediaId)
                    .putString(METADATA_KEY_TITLE,song.title)
                    .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                    .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                    .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                    .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                    .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subtitle)
                    .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subtitle)
                    .build()
        }
    }
    //convert songs list to media source for exoplayer prepare in MusicService.kt (basically create playlist of song by concantenate song)
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory):ConcatenatingMediaSource{
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach{song->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }
    //convert to list media item for use in browsing/searching media
    fun asMediaItem() = songs.map { song->
        val description = MediaDescriptionCompat.Builder()
                .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
                .setTitle(song.description.title)
                .setSubtitle(song.description.subtitle)
                .setMediaId(song.description.mediaId)
                .setIconUri(song.description.iconUri)
                .build()
        MediaBrowserCompat.MediaItem(description,FLAG_PLAYABLE)
    }.toMutableList()

    private val onReadyListeners = mutableListOf<(Boolean)->Unit>()

    private var state:State = STATE_CREATED
        set(value) {
            //setter check if we set value to INITIALIZED OR ERROR
            // to sum up this block mean to check if state is initialized
            if (value == STATE_INITIALIZED || value == STATE_ERROR){
                // synchronized mean what happen in block {} can only be access in same thread
                synchronized(onReadyListeners){
                    //just assign value
                    field = value
                    //loop over lambda fun pass boolean(check state == Initialized)
                    onReadyListeners.forEach { listener->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }
    fun whenReady(action:(Boolean)->Unit):Boolean{
        if(state == STATE_CREATED || state == STATE_INITIALIZING){
            //not ready
            onReadyListeners += action
            return false
        } else {
            //ready and set state to initialized
            action(state == STATE_INITIALIZED)
            return true
        }
    }
}

enum class State{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}