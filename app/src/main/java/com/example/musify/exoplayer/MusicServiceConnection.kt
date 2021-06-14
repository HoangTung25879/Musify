package com.example.musify.exoplayer

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musify.data.Constants.NETWORK_ERROR
import com.example.musify.data.Event
import com.example.musify.data.Resource
//class sit between service and fragment(activity)
private val TAG = "MUSICSERVICECONNECTION"
class MusicServiceConnection(
        context: Context
) {
    //check if music service is connected with view
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _currPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val currPlayingSong: LiveData<MediaMetadataCompat?> = _currPlayingSong

    lateinit var mediaController : MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(
                    context,
                    MusicService::class.java
            ),
            mediaBrowserConnectionCallback,
            null
    ).apply { connect() } // call connect() to trigger onConnected function in MediaBrowserConnectionCallback

    //cant assign equal because need to wait to pass token to media controller
    //using to skip pause play resume song
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun sendCommand(command: String, parameters: Bundle?) =
            sendCommand(command, parameters) { _, _ -> }

    private fun sendCommand(
            command: String,
            parameters: Bundle?,
            resultCallback: ((Int, Bundle?) -> Unit)
    ) = if (mediaBrowser.isConnected) {
        mediaController.sendCommand(command, parameters, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                resultCallback(resultCode, resultData)
            }
        })
        true
    } else {
        false
    }
    //call to subscribe playlist or single media item
    fun subscribe(parentId:String,callback:MediaBrowserCompat.SubscriptionCallback){
        //SubscriptionCallback - this is used to update your UI so that you can show the user content (from the MusicService) that they can browse for playback.
        mediaBrowser.subscribe(parentId,callback)
    }
    //call to unsubscribe playlist or single media item
    fun unsubscribe(parentId:String,callback:MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.unsubscribe(parentId,callback)
    }
    //this is used to get the MediaController using the MediaBrowser’s MediaSession token.
    // You can then get the TransportControls that you will use to actually initiate playback, pause, stop, skip, etc.
    private inner class MediaBrowserConnectionCallback(
            private val context: Context
    ):MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            //set mediacontroller token give us access to everything
            mediaController = MediaControllerCompat(context,mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            //connected
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            _isConnected.postValue(Event(Resource.error(
                    "The connection was suspended",false
            )))
        }

        override fun onConnectionFailed() {
            _isConnected.postValue(Event(Resource.error(
                    "Couldn't connect to media browser",false
            )))
        }
    }
//this is used to update the of your app with the current playback state, and what media is currently loaded.
    private inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        //call when user STOP PAUSE PLAY SKIP ...etc
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
           _playbackState.postValue(state)
        }
        //When song change (call when player skip to next song or previous song)
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if(metadata != null){
                _currPlayingSong.postValue(metadata)
            }
        }
        //notify when network error
        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event){
                NETWORK_ERROR -> _networkError.postValue(
                        Event(
                                Resource.error(
                                        "Couldn't connect to server. Check your internet connection",
                                        null
                                )
                        )
                )
            }
        }
        //if session destroy postvalue suspend status
        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

}