package com.example.musify.ui

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.example.musify.R
import com.example.musify.adapter.SwipeSongAdapter
import com.example.musify.data.Status.*
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.MusicSource
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels() //bind viewmodel to the lifecycle where we initialize this viewmodel

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private var currPlayingSong: Song? = null

    private var playbackState : PlaybackStateCompat? = null

    private var musicSource = MusicSource()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //setup permission
        requestPermission(this)
        //
        subscribeToObservers()
        vpSong.adapter = swipeSongAdapter
        vpSong.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //if a song is playing and switch song we want to play it
                //if a song is pause and switch song we update current playing song
                if (playbackState?.isPlaying == true){
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                } else {
                    currPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        ivPlayPause.setOnClickListener {
            currPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,toggle = true)
            }
        }

        swipeSongAdapter.setItemClickListener {
            navHostFragment.findNavController().navigate(R.id.globalActionToSongFragment)
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id){
                R.id.detailSongFragment -> hideBottomBar()
                R.id.firebaseSongFragment -> showBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun hideBottomBar(){
        ivCurSongImage.isVisible = false
        vpSong.isVisible = false
        ivPlayPause.isVisible = false
        bottom_navigation.isVisible = false
    }

    private fun showBottomBar(){
        ivCurSongImage.isVisible = true
        vpSong.isVisible = true
        ivPlayPause.isVisible = true
        bottom_navigation.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        //if song dont exist in list will return -1
        if (newItemIndex != -1) {
            vpSong.currentItem = newItemIndex
            currPlayingSong = song
        }
    }

    private fun requestPermission(context: Context){
        Dexter.withContext(context)
            .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
            .withListener(object:MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }
            })
            .check()
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
                            //because if songlist empty and we want to display image from first song app will crash
                            if (songs.isNotEmpty()) {
                                glide.load((currPlayingSong ?: songs[0]).imageUrl)
                                    .into(ivCurSongImage)
                            }
                            switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
                        }
                    }
                    ERROR -> Unit
                    LOADING -> Unit
                }
            }
        }
        mainViewModel.currPlayingSong.observe(this) {
            if (it == null) return@observe

            currPlayingSong = it.toSong()
            glide.load(currPlayingSong?.imageUrl).into(ivCurSongImage)
            switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
        }
        //change play pause icon
        mainViewModel.playbackState.observe(this){
            playbackState = it
            ivPlayPause.setImageResource(
                 if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
        mainViewModel.isConnected.observe(this){
            it?.getContentIfNotHandled()?.let { result->
                when(result.status){
                    ERROR-> Snackbar.make(
                        rootLayout,
                        result.message ?: "Unknown error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
        mainViewModel.networkError.observe(this){
            it?.getContentIfNotHandled()?.let { result->
                when(result.status){
                    ERROR-> Snackbar.make(
                        rootLayout,
                        result.message ?: "Unknown error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
}