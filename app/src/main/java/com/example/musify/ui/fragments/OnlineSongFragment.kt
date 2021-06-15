package com.example.musify.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.adapter.OnlineSongAdapter
import com.example.musify.data.Status
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentOnlineSongBinding
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_online_song.*
import javax.inject.Inject

private const val TAG = "ONLINESONGFRAGMENT"
@AndroidEntryPoint
class OnlineSongFragment :Fragment(){

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentOnlineSongBinding

    @Inject
    lateinit var songAdapter: OnlineSongAdapter

    private var currPlayingSong: Song? = null
    private var playbackState : PlaybackStateCompat? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupViewModel(inflater,container)
            val view = binding.root
            return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        subscribeToObservers()
    }
    private fun setupViewModel(inflater: LayoutInflater,container: ViewGroup?){
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_online_song,container,false)
        binding.lifecycleOwner = this
        binding.onlineViewModel = mainViewModel
    }
    private fun setupRecyclerView() {
        binding.rvAllSongs.adapter = songAdapter
        songAdapter.listener = object : OnlineSongAdapter.SongAdapterListener{
            override fun onItemClicked(song: Song) {
                mainViewModel.playOrToggleSong(song)
            }

            override fun onMenuClicked(song: Song, view: View) {
                openOptionMenu(song,view)
            }

        }
        binding.rvAllSongs.layoutManager = LinearLayoutManager(requireContext())
    }
    private fun openOptionMenu(song: Song,view: View){
        val popup = PopupMenu(context,view)
        popup.inflate(R.menu.option_download_menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.download->{
                    onDownloadClicked(song)
                    true
                }
                R.id.delete->{
                    onDeleteClicked(song)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun onDeleteClicked(song: Song){
        Config.currentSongSelect = song
        val deleteDialog = DeleteDiaglogFragment()
        deleteDialog.isCancelable = false
        deleteDialog.show(requireActivity().supportFragmentManager,"DELETE")
        deleteDialog.callback = object : DeleteDiaglogFragment.Callback{
            override fun onFinishDelete(isSuccess: Boolean) {
                if (isSuccess){
                    mainViewModel.pause()
                    mainViewModel.fetchSongs()
                }
            }
        }
    }
    private fun onDownloadClicked(song: Song){
        Config.currentSongSelect = song
        val downloadDialog = DownloadDialogFragment()
        downloadDialog.isCancelable = false
        downloadDialog.show(requireActivity().supportFragmentManager,"DOWNLOAD")
        downloadDialog.callback = object : DownloadDialogFragment.Callback{
            override fun onFinishDownload(isSuccess: Boolean) {
                if (isSuccess){
                    mainViewModel.pause()
                    mainViewModel.fetchSongs()
                }
            }
        }
    }
    private fun findSongPos(list: List<Song>,song:Song) : Int{
        list.forEachIndexed { index, item ->
            if(song.mediaId == item.mediaId &&
                    song.imageUrl == item.imageUrl &&
                    song.title == item.title &&
                    song.subtitle == item.subtitle &&
                    song.duration == item.duration &&
                    song.songUrl == item.songUrl &&
                    song.isLocal == item.isLocal) return index
        }
        return -1
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result->
            when(result.status){
                Status.SUCCESS->{
                    binding.apply {
                        rvAllSongs.isVisible = true
                        allSongsProgressBar.isVisible = false
                        result.data?.let { songs->
                            //display list song to view
                            val onlineSongList = songs.filter { it.isLocal == false }
                            songAdapter.submitList(onlineSongList)
                            tvEmptyOnline.isVisible = onlineSongList.size == 0
                        }
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> {
                    binding.apply {
                        allSongsProgressBar.isVisible = true
                        rvAllSongs.isVisible = false
                    }
                }
            }
        }
        mainViewModel.currPlayingSong.observe(viewLifecycleOwner){
            if (it == null) return@observe
            currPlayingSong = it.toSong()
        }
        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState = it
            if (it?.isPlaying == true && currPlayingSong != null) {
                val cloneList = songAdapter.currentList.map { it.copy() }
                val songPos = findSongPos(cloneList,currPlayingSong!!)
                if (songPos != -1 ){
                    cloneList.mapIndexed { index, song ->
                        song.isPlaying = index == songPos
                    }
                    songAdapter.submitList(cloneList)
                } else {
                    cloneList.map {
                        it.isPlaying = false
                    }
                    songAdapter.submitList(cloneList)
                }
            } else {
                val cloneList = songAdapter.currentList.map { it.copy() }
                cloneList.map {
                    it.isPlaying = false
                }
                songAdapter.submitList(cloneList)
            }
        }
    }
}