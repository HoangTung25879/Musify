package com.example.musify.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.Constants
import com.example.musify.exoplayer.MusicService
import com.example.musify.exoplayer.MusicServiceConnection
import com.example.musify.exoplayer.currentPlaybackPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongViewModel @ViewModelInject constructor(
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val playbackState = musicServiceConnection.playbackState

    private val _currSongDuration = MutableLiveData<Long>()
    val currSongDuration: LiveData<Long> = _currSongDuration

    private val _currPlayerPosition = MutableLiveData<Long>()
    val currPlayerPosition : LiveData<Long> = _currPlayerPosition

    init{
        updateCurrentPlayerPosition()
    }
    private fun updateCurrentPlayerPosition(){
        viewModelScope.launch {
            while (true){
                val pos = playbackState.value?.currentPlaybackPosition
                if (currPlayerPosition.value != pos){
                    _currPlayerPosition.postValue(pos)
                    _currSongDuration.postValue(MusicService.currSongDuration)
                }
                delay(Constants.UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }
}