package com.example.musify.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.Constants.NETWORK_ERROR
import com.example.musify.data.Event
import com.example.musify.data.Resource
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.musify.exoplayer.callbacks.MusicPlayerEventListener
import com.example.musify.exoplayer.callbacks.MusicPlayerNotificationListener
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService: MediaBrowserServiceCompat(){
    companion object{
        var currSongDuration = 0L
            private set
        private val _audioSessId = MutableLiveData<Int>()
        val audioSessId : LiveData<Int> = _audioSessId
    }

    @Inject
    lateinit var dataSourceFactory:DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main+serviceJob) //Then we don't need to cancel our coroutines manually when the service stops because the scope cares about that

    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegoundService = false

    private var currPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener:MusicPlayerEventListener

    override fun onCreate() {
        super.onCreate()
        //coroutine fetch song
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
            firebaseMusicSource.fetchMediaDataFromLocal()
        }
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let{
            PendingIntent.getActivity(this,0,it,0)
        }
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
                this,
                mediaSession.sessionToken,
                MusicPlayerNotificationListener(this)
        ){
            //this is newSongCallback
            //check this to fix bug show time 47:59 in SongFragment
            if ( exoPlayer.duration != C.TIME_UNSET){
                currSongDuration = exoPlayer.duration
            }
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            currPlayingSong = it
            preparePlayer(
                    firebaseMusicSource.songs,
                    it,
                    true
            )

        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
        exoPlayer.addAnalyticsListener(object : AnalyticsListener{
            override fun onAudioSessionId(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                super.onAudioSessionId(eventTime, audioSessionId)
                Log.d("AAAAA",audioSessionId.toString())
                _audioSessId.postValue(audioSessionId)
            }
        })
    }

    private fun preparePlayer(
            songs:List<MediaMetadataCompat>,
            itemToPlay: MediaMetadataCompat?,
            playNow: Boolean //usually pass false for first time and let user choose to play when already play and switch song pass true
    ){
        Log.d("AAAAAA","PREPAREPLAYER")
        val currSongIndex = if (currPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currSongIndex,0L) // 0L = number 0 of type long // play from beginning
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID,null)
    }

    //load playlist
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //MEDIA_ROOT_ID is id of a playlist
        when(parentId){
            MEDIA_ROOT_ID ->{
                val resultsSent = firebaseMusicSource.whenReady { isInitialized->
                    if(isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItem())
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],playNow = false)
                            isPlayerInitialized = true
                        }
                    } else{
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        result.sendResult(null)
                    }
                }
                if(!resultsSent){
                    result.detach()
                }
            }
        }
    }

    private inner class MusicQueueNavigator:TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }
}