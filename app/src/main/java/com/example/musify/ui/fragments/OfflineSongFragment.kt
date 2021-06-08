package com.example.musify.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.adapter.OfflineSongAdapter
import com.example.musify.data.Status
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentOfflineSongBinding
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_offline_song.*
import javax.inject.Inject

private val TAG = "OFFLINESONGFRAGMENT"
@AndroidEntryPoint
class OfflineSongFragment : Fragment(R.layout.fragment_offline_song) {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentOfflineSongBinding

    @Inject
    lateinit var songAdapter: OfflineSongAdapter

    private var currPlayingSong: Song? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_offline_song,container,false)
        binding.lifecycleOwner = this
        binding.offlineViewModel = mainViewModel
    }
    private fun setupRecyclerView() {
        binding.rvAllSongs.adapter = songAdapter
        binding.rvAllSongs.layoutManager = LinearLayoutManager(requireContext())
        songAdapter.listener = object : OfflineSongAdapter.SongAdapterListener {
            override fun onItemClicked(song: Song) {
                mainViewModel.playOrToggleSong(song)
            }
        }
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result->
            when(result.status){
                Status.SUCCESS->{
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs->
                        //display list song to view
                        songAdapter.submitList(
                            songs.filter { it.isLocal == true }
                        )
                        if(songs.size == 0){
                            tvEmptyOffline.isVisible = false
                        } else {
                            tvEmptyOffline.visibility = View.GONE
                        }
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> allSongsProgressBar.isVisible = true
            }
        }
        mainViewModel.currPlayingSong.observe(viewLifecycleOwner){
            if (it == null) return@observe
            currPlayingSong = it.toSong()
        }
        mainViewModel.playbackState.observe(viewLifecycleOwner){
            if (it?.isPlaying == true && currPlayingSong != null) {
                val cloneList = songAdapter.currentList.map { it.copy() }
                val songPos = cloneList.indexOf(currPlayingSong)
                if (songPos != -1 ){
                    cloneList.mapIndexed { index, song ->
                        song.isPlaying = index == songPos
                    }
//                    for ((index,song) in cloneList.withIndex()){
//                        song.isPlaying = index == songPos
//                    }
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