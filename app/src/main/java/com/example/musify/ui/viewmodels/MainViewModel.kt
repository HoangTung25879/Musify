package com.example.musify.ui.viewmodels

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.Config
import com.example.musify.data.Constants
import com.example.musify.data.Constants.DURATION
import com.example.musify.data.Constants.IS_LOCAL
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.Resource
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MAINVIEWMODEL"
class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems : LiveData<Resource<List<Song>>> = _mediaItems
    private val _currSongDuration = MutableLiveData<Long>()
    val currSongDuration: LiveData<Long> = _currSongDuration
    private val _currPlayerPosition = MutableLiveData<Long>()
    val currPlayerPosition : LiveData<Long> = _currPlayerPosition

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
                val items = children.map {
                    val bundle = it.description.extras
                    val isLocal = bundle?.getString(IS_LOCAL).toBoolean()
                    val duration = bundle?.getString(DURATION)
                    Song(
                        mediaId = it.mediaId!!,
                        title = it.description.title.toString(),
                        subtitle = it.description.subtitle.toString(),
                        songUrl = it.description.mediaUri.toString(),
                        imageUrl = (it.description.iconUri ?: "").toString(),
                        isLocal = isLocal,
                        isPlaying = false,
                        duration = duration!!
                    )
                }
                Config.currentSongList = items
                _mediaItems.postValue(Resource.success(items))
            }
        })
        updateCurrentPlayerPosition()
    }
    fun fetchSongs(){
        _mediaItems.postValue(Resource.loading(null))
        val args = Bundle()
        args.putInt("nRecNo",2)
        musicServiceConnection.sendCommand("Add Songs",args)
    }
    fun skipToNextSong(){
        Log.d(TAG,"skipToNext")
        if (Config.isShuffle){
            playOrToggleSong(randomSong()!!)
        } else musicServiceConnection.transportControls.skipToNext()
    }
    fun skipToPreviousSong(){
        musicServiceConnection.transportControls.skipToPrevious()
    }
    //jump to time
    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }
    fun pause(){
        musicServiceConnection.transportControls.pause()
    }
    //toggle to true to change play state
    fun playOrToggleSong(mediaItem:Song,toggle:Boolean = false){
        Config.isLocal = mediaItem.isLocal
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
    private fun handleRepeat(){
        if(Config.isRepeat){
            seekTo(0L)
        }
    }
    private fun handleShuffle(){
        if(Config.isShuffle){
            if (Config.isRepeat){
                seekTo(0L)
            } else {
                playOrToggleSong(randomSong()!!)
            }
        }
    }
    private fun randomSong() : Song?{
        val offlineSong = mediaItems.value?.data?.filter { it.isLocal == true }
        val onlineSong = mediaItems.value?.data?.filter { it.isLocal == false }
        if (Config.isLocal) return offlineSong?.random() else return onlineSong?.random()
    }
    //create coroutine bound to this viewmodel lifecycle and continuous update player position
    //and song duration
    private fun updateCurrentPlayerPosition(){
        //coroutine cancel when viewmodel destroy
        viewModelScope.launch {
            while (true){
                val pos = playbackState.value?.currentPlaybackPosition
                if (pos != null && currSongDuration.value != null){
                    if (pos > (currSongDuration.value!! - 1000L) && pos < currSongDuration.value!!){
                        handleRepeat()
                        handleShuffle()
                    }
                }
                if (currPlayerPosition.value != pos){
                    _currSongDuration.postValue(MusicService.currSongDuration)
                    _currPlayerPosition.postValue(pos!!)
                }
                delay(Constants.UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){})
    }

}