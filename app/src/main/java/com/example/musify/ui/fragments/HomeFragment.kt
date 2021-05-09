package com.example.musify.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musify.R
import com.example.musify.adapter.SongAdapter
import com.example.musify.data.Status
import com.example.musify.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment :Fragment(R.layout.fragment_home){

    lateinit var mainViewModel: MainViewModel
    private lateinit var allSongsProgressBar : ProgressBar

    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        allSongsProgressBar = view.findViewById(R.id.allSongsProgressBar)
        setupRecyclerView(view)
        subscribeToObservers()
        songAdapter.setOnItemClickListener {
            Log.d("AAA","CLICKED")
            mainViewModel.playOrToggleSong(it)
        }

    }

    private fun setupRecyclerView(view: View) = view.findViewById<RecyclerView>(R.id.rvAllSongs).apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItem.observe(viewLifecycleOwner){result->
            when(result.status){
                Status.SUCCESS->{
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs->
                        songAdapter.songs = songs
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> allSongsProgressBar.isVisible = true
            }
        }
    }
}