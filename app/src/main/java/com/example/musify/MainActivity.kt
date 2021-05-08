package com.example.musify

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musify.MusicList.MusicListAdapter
import com.example.musify.Util.getAllAudioFromDevice
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class MainActivity : AppCompatActivity() {
    private lateinit var musicList:List<Music>
    private lateinit var musicListAdapter: MusicListAdapter
    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission(this)
        musicListAdapter = MusicListAdapter()
        musicListAdapter.submitList(musicList)
        val rcListMusic = findViewById<RecyclerView>(R.id.rcListMusic)
        rcListMusic.layoutManager = LinearLayoutManager(this)
        rcListMusic.adapter = musicListAdapter
        initPlayer()
    }

    fun requestPermission(context: Context){
        Dexter.withContext(context)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener{
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        musicList = getAllAudioFromDevice(context)
//                        for(item in musicList){
//                            Log.d("AAA","${item.path.toString()}-@@-${item.name}-@@-${item.album}-@@-${item.artist}")
//                        }
                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        TODO("Not yet implemented")
                    }

                    override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                        p1?.continuePermissionRequest()
                    }

                })
                .check()
    }

    fun initPlayer(){
        playerView = findViewById(R.id.playerview)
        playerView.controllerShowTimeoutMs = 0
        playerView.cameraDistance = 30f
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        playerView.player = simpleExoPlayer
        val datasourceFactory: DataSource.Factory = DefaultDataSourceFactory(this,Util.getUserAgent(this,"musify"))
        val audioSource: MediaSource = ProgressiveMediaSource.Factory(datasourceFactory).createMediaSource(musicList[1].path)
        simpleExoPlayer.prepare(audioSource)
        simpleExoPlayer.playWhenReady = true

    }

}