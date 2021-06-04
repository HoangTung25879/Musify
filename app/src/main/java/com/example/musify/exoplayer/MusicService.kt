package com.example.musify.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import com.example.musify.data.Constants.MEDIA_ROOT_ID
import com.example.musify.data.Constants.NETWORK_ERROR
import com.example.musify.exoplayer.callbacks.MusicPlayerNotificationListener
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"
//a Service that manages the player and handles preparing and playing media
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

    private val musicSource: MusicSource = MusicSource()

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
        //create a MediaSession and get it’s token.
        super.onCreate()
        //coroutine fetch song
        serviceScope.launch {
            musicSource.fetchMediaData()
        }
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let{
            PendingIntent.getActivity(this,0,it,0)
        }
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        //Pass this token to the MediaBrowserService by calling setSessionToken,
        // and this will connect the MediaBrowserService to the MediaSession,
        // and will allow the MediaBrowser (client to work with the MediaSession)
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

        val musicPlaybackPreparer = MusicPlaybackPreparer(){
            currPlayingSong = it
            preparePlayer(
                    musicSource.songs,
                    it,
                    true
            )

        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener()
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
        exoPlayer.addAnalyticsListener(object : AnalyticsListener{
            override fun onAudioSessionId(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                super.onAudioSessionId(eventTime, audioSessionId)
                _audioSessId.postValue(audioSessionId)
            }
        })
    }

    private fun preparePlayer(
            songs:List<MediaMetadataCompat>,
            itemToPlay: MediaMetadataCompat?,
            playNow: Boolean //usually pass false for first time and let user choose to play when already play and switch song pass true
    ){
        val currSongIndex = if (currPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(musicSource.asMediaSource(dataSourceFactory))
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
        // Returning null == no one can connect
        // so we’ll return something
        //must return a non-null BrowserRoot to allow connections to your MediaBrowserServiceCompat
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
                val resultsSent = musicSource.whenReady { isInitialized->
                    if(isInitialized){
                        //Each item returned is a MediaItem and each MediaItem consists of a MediaDescriptionCompat (a subset of metadata) and some combination of the two available flags:
                        //FLAG_BROWSABLE indicates that this MediaItem has children of its own (i.e., its media id can be passed to onLoadChildren() to get more MediaItems.
                        //FLAG_PLAYABLE should be used when this MediaItem can be directly played (i.e., passed to playFromMediaId() to start playback)
                        result.sendResult(musicSource.asMediaItem())
                        if(!isPlayerInitialized && musicSource.songs.isNotEmpty()){
                            preparePlayer(musicSource.songs,musicSource.songs[0],playNow = false)
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
            return musicSource.songs[windowIndex].description
        }
    }
    private inner class MusicPlaybackPreparer(private val playerPrepared: (MediaMetadataCompat?)->Unit) : MediaSessionConnector.PlaybackPreparer {
        //dont need
        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?) = false

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        //dont need
        override fun onPrepare(playWhenReady: Boolean) = Unit

        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            musicSource.whenReady {
                val itemToPlay = musicSource.songs.find { mediaId == it.description.mediaId }
                playerPrepared(itemToPlay)
            }
        }

        //dont need
        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit
        //dont need
        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
    }
    private inner class MusicPlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            if(playbackState == Player.STATE_READY && !playWhenReady){
                this@MusicService.stopForeground(false)
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            Toast.makeText(this@MusicService,"An unknown error occured", Toast.LENGTH_LONG).show()
        }
    }
}