package com.example.musify.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.Resource
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.MusicServiceConnection
import com.example.musify.exoplayer.isPlayEnabled
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.isPrepared

class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems : LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val currPlayingSong = musicServiceConnection.currPlayingSong
    val playbackState = musicServiceConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object: MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items = children.map {
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
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