package com.example.musify.exoplayer

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.util.Log
import androidx.core.net.toUri
import com.example.musify.data.Constants.DURATION
import com.example.musify.data.Constants.IS_LOCAL
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.MusicDatabase
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.State.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "MUSICSOURCE"
class MusicSource { // list of song get from firebase
    private val musicDatabase = MusicDatabase()
    //song list of type MediaMetaDataCompat to use in music service (contain metadata of song)
    var songs = emptyList<MediaMetadataCompat>()
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

    suspend fun fetchMediaData(context: Context) = withContext(Dispatchers.Main){
        state = STATE_INITIALIZING
        val firebaseSongs = musicDatabase.getFirebaseSongs()
        val localSongs = musicDatabase.getLocalSongs(context)
        val allSong = mutableListOf<Song>()
        allSong.addAll(firebaseSongs)
        allSong.addAll(localSongs)
        songs = allSong.map { song->
            Log.d(TAG,"fetch ${song}")
            Builder()
                .putString(METADATA_KEY_ARTIST,song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID,song.mediaId)
                .putString(METADATA_KEY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subtitle)
                .putString(DURATION,song.duration)
                    .putString(IS_LOCAL,song.isLocal.toString())
                .build()
        }
        MusicService.musicServiceInstance?.notifyChildrenChanged(MEDIA_ROOT_ID)
        state = STATE_INITIALIZED
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
        val bundle = Bundle()
        bundle.putString(IS_LOCAL,song.getString(IS_LOCAL))
        bundle.putString(DURATION,song.getString(DURATION))
        val description = MediaDescriptionCompat.Builder()
                .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
                .setTitle(song.description.title)
                .setSubtitle(song.description.subtitle)
                .setMediaId(song.description.mediaId)
                .setIconUri(song.description.iconUri)
                .setExtras(bundle)
                .build()
        MediaBrowserCompat.MediaItem(description,FLAG_PLAYABLE)
    }.toMutableList()

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