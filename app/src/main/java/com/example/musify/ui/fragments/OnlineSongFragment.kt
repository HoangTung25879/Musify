package com.example.musify.ui.fragments

import android.os.Bundle
import android.util.Log
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
import com.example.musify.adapter.OnlineSongAdapter
import com.example.musify.data.Status
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentOnlineSongBinding
import com.example.musify.exoplayer.isPlayEnabled
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
        }
        binding.rvAllSongs.layoutManager = LinearLayoutManager(requireContext())
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result->
            when(result.status){
                Status.SUCCESS->{
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs->
                        //display list song to view
                        songAdapter.submitList(
                                songs.filter { it.isLocal == false }
                            )
                        if(songs.size == 0){
                            tvEmptyOnline.isVisible = false
                        } else {
                            tvEmptyOnline.visibility = View.GONE
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
            if (it?.isPlaying == true) {
                val cloneList = songAdapter.currentList.map { it.copy() }
                val songPos = cloneList.indexOf(currPlayingSong)
                if (songPos != -1 ){
                    cloneList.mapIndexed { index, song ->
                        song.isPlaying = index == songPos
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