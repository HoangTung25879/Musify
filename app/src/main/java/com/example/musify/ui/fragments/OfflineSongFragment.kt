package com.example.musify.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.adapter.BaseSongAdapter
import com.example.musify.adapter.OfflineSongAdapter
import com.example.musify.data.Status
import com.example.musify.data.entities.Song
import com.example.musify.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_offline_song.*
import javax.inject.Inject

private val TAG = "OFFLINESONGFRAGMENT"
@AndroidEntryPoint
class OfflineSongFragment : Fragment(R.layout.fragment_offline_song) {

    lateinit var mainViewModel: MainViewModel
    @Inject
    lateinit var songAdapter: OfflineSongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        setupRecyclerView(view)
        subscribeToObservers()
        songAdapter.listener = object : BaseSongAdapter.SongAdapterListener{
            override fun onItemClicked(song: Song) {
                mainViewModel.playOrToggleSong(song)
            }
        }
    }

    private fun setupRecyclerView(view: View) = rvAllSongs.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result->
            when(result.status){
                Status.SUCCESS->{
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs->
                        //display list song to view
                        songAdapter.songs = songs.filter {
                            it.isLocal == true
                        }
                        if(songAdapter.itemCount == 0){
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
    }
}