package com.example.musify.ui.viewmodels

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musify.data.Constants.IS_LOCAL
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.Resource
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.*

class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems : LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val currPlayingSong = musicServiceConnection.currPlayingSong
    val playbackState = musicServiceConnection.playbackState
    val audioSessionId = MusicService.audioSessId

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object: MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                Log.d("MAINVIEWMODEL","${children.size}")
                val items = children.map {
                    val bundle = it.description.extras
                    val isLocal = bundle?.getString(IS_LOCAL).toBoolean()
                    Log.d("MAINVIEWMODEL","${it.mediaId!!} - ${it.description.title} - ${it.description.subtitle} - ${it.description.mediaUri} - ${isLocal}")
                    Song(
                        mediaId = it.mediaId!!,
                        title = it.description.title.toString(),
                        subtitle = it.description.subtitle.toString(),
                        songUrl = it.description.mediaUri.toString(),
                        imageUrl = it.description.iconUri.toString(),
                        isLocal = isLocal,
                        isPlaying = false
                    )
                }
                _mediaItems.postValue(Resource.success(items))
            }
        })
    }

    fun skipToNextSong(){
        musicServiceConnection.transportControls.skipToNext()
    }
    fun skipToPreviousSong(){
        musicServiceConnection.transportControls.skipToPrevious()
    }
    //jump to time
    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }
    //toggle to true to change play state
    fun playOrToggleSong(mediaItem:Song,toggle:Boolean = false){
        val isPrepared = playbackState.value?.isPrepared ?: false //playbackState.value : get value from live data object
        if (isPrepared && mediaItem.mediaId == currPlayingSong?.value?.getString(METADATA_KEY_MEDIA_ID)){
            //toggle play pause current song
            playbackState.value?.let { playbackState ->
                when{
                    playbackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else {
            //play new song
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId,null)
        }
    }
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){})
    }

}