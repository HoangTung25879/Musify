package com.example.musify.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentStatePagerAdapter
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
import com.example.musify.databinding.ActivityMainTestBinding
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private val TAG = "MAINACTIVITY"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels() //bind viewmodel to the lifecycle where we initialize this viewmodel
    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: ActivityMainTestBinding

    private var currPlayingSong: Song? = null

    private var playbackState : PlaybackStateCompat? = null

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeToObservers()
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main_test)
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
            bottomNavigation.setOnNavigationItemSelectedListener {
                when(it.itemId){
                    R.id.online_song ->{
                        userViewPager.currentItem = ViewPagerAdapter.ONLINE_PAGE
                        true
                    }
                    R.id.offline_song ->{
                        userViewPager.currentItem = ViewPagerAdapter.OFFLINE_PAGE
                        true
                    }
                    else -> false
                }
            }
        }

    }
    private fun setupViewPager(){
        binding.apply {
            val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager,FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
            userViewPager.adapter = viewPagerAdapter
            userViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    when(position){
                        ViewPagerAdapter.ONLINE_PAGE -> bottomNavigation.menu.findItem(R.id.online_song).isChecked = true
                        ViewPagerAdapter.OFFLINE_PAGE -> bottomNavigation.menu.findItem(R.id.offline_song).isChecked = true
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
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
                                swipeSongAdapter.submitList(songs)
                                //because if songlist empty and we want to display image from first song app will crash
                                if (songs.isNotEmpty()) {
//                                glide.load((currPlayingSong ?: songs[0]).imageUrl)
//                                    .into(ivCurSongImage)
                                    glide.load(R.drawable.music)
                                            .into(ivCurSongImage)
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
//            glide.load(currPlayingSong?.imageUrl).into(ivCurSongImage)
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