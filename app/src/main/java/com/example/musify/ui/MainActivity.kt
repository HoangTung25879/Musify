package com.example.musify.ui

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
import com.example.musify.data.Status
import com.example.musify.data.Status.*
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private var currPlayingSong: Song? = null

    private var playbackState : PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObservers()
        vpSong.adapter = swipeSongAdapter
        vpSong.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
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
                R.id.songFragment -> hideBottomBar()
                R.id.homeFragment -> showBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun hideBottomBar(){
        ivCurSongImage.isVisible = false
        vpSong.isVisible = false
        ivPlayPause.isVisible = false
    }

    private fun showBottomBar(){
        ivCurSongImage.isVisible = true
        vpSong.isVisible = true
        ivPlayPause.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1) {
            vpSong.currentItem = newItemIndex
            currPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
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