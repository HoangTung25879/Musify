package com.example.musify.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.adapter.SwipeSongAdapter
import com.example.musify.adapter.ViewPagerAdapter
import com.example.musify.data.Status.*
import com.example.musify.data.entities.Song
import com.example.musify.databinding.ActivityMainBinding
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.imaginativeworld.oopsnointernet.callbacks.ConnectionCallback
import org.imaginativeworld.oopsnointernet.dialogs.signal.NoInternetDialogSignal
import javax.inject.Inject

private val TAG = "MAINACTIVITY"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels() //bind viewmodel to the lifecycle where we initialize this viewmodel
    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: ActivityMainBinding

    private var currPlayingSong: Song? = null

    private var playbackState : PlaybackStateCompat? = null

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"ONCREATE")
        NoInternetDialogSignal.Builder(this,lifecycle).apply {
            dialogProperties.apply {
                connectionCallback = object : ConnectionCallback{
                    override fun hasActiveConnection(hasActiveConnection: Boolean) {
                        Config.isConnected = hasActiveConnection
                    }
                }
                cancelable = false
                noInternetConnectionTitle = "No Internet"
                noInternetConnectionMessage =
                    "Check your Internet connection and try again."
                showInternetOnButtons = true
                pleaseTurnOnText = "Please turn on"
                wifiOnButtonText = "Wifi"
                mobileDataOnButtonText = "Mobile data"
            }
        }.build()
        subscribeToObservers()
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        binding.apply {
            setupViewPager()
            navController = (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
            vpSong.adapter = swipeSongAdapter
            vpSong.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    //if a song is playing and switch song we want to play it
                    //if a song is pause and switch song we update current playing song
                    if (playbackState?.isPlaying == true){
                        mainViewModel.playOrToggleSong(swipeSongAdapter.getSong(position))
                    } else {
                        currPlayingSong = swipeSongAdapter.getSong(position)
                    }
                }
            })
            ivPlayPause.setOnClickListener {
                currPlayingSong?.let {
                    mainViewModel.playOrToggleSong(it,toggle = true)
                }
            }
            swipeSongAdapter.listener = object :SwipeSongAdapter.SongAdapterListener{
                override fun onItemClicked(song: Song) {
                    navController.navigate(R.id.globalActionToSongFragment)
                }
            }
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id){
                    R.id.detailSongFragment -> {
                        hideBottomBar()
                        userViewPager.isVisible = false
                    }
                    else -> if(Config.isInitial) showBottomBar() else {
                        showBottomBar()
                        viewSong.isVisible = true
                        userViewPager.isVisible = true
                    }
                }
            }
        }

    }
    private fun setupViewPager(){
        binding.apply {
            userViewPager.adapter = ViewPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
            bottomNavigation.setupWithViewPager(userViewPager)
        }
    }
    private fun hideBottomBar(){
        binding.apply {
            viewSong.isVisible = false
            bottomNavigation.isVisible = false
        }
    }

    private fun showBottomBar(){
        binding.bottomNavigation.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.currentList.indexOf(song)
        //if song dont exist in list will return -1
        if (newItemIndex != -1) {
            binding.vpSong.currentItem = newItemIndex
            currPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    SUCCESS -> {
                        binding.apply {
                            viewSong.isVisible = true
                            Config.isInitial = false
                            result.data?.let { songs ->
                                Log.d(TAG,"RELOAD")
                                swipeSongAdapter.submitList(songs)
                                //because if songlist empty and we want to display image from first song app will crash
                                if (songs.isNotEmpty()) {
                                    glide.load(R.drawable.music).into(ivCurSongImage)
                                }
                                switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
                            }
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
            Log.d("MUSICSOURCE", "currplaysong $currPlayingSong")
            binding.apply {
                glide.load(R.drawable.music).into(ivCurSongImage)
            }
            switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
        }
        //change play pause icon
        mainViewModel.playbackState.observe(this){
            playbackState = it
            binding.ivPlayPause.setImageResource(
                 if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
        mainViewModel.isConnected.observe(this){
            it?.getContentIfNotHandled()?.let { result->
                when(result.status){
                    ERROR-> Snackbar.make(
                        binding.rootLayout,
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
                        binding.rootLayout,
                        result.message ?: "Unknown error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
}