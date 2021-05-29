package com.example.musify.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musify.R
import com.example.musify.adapter.SongAdapter
import com.example.musify.data.Status
import com.example.musify.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_firebase_song.*
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseSongFragment :Fragment(R.layout.fragment_firebase_song){

    lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        setupRecyclerView(view)
        subscribeToObservers()
        songAdapter.setItemClickListener {
            //click on a song is playing we dont want to pause it so no pass toggle true here
            mainViewModel.playOrToggleSong(it)
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
                        songAdapter.songs = songs
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> allSongsProgressBar.isVisible = true
            }
        }
    }
}