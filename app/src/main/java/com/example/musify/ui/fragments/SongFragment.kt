package com.example.musify.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.musify.R
import com.example.musify.data.Status
import com.example.musify.data.Status.SUCCESS
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.isPlaying
import com.example.musify.exoplayer.toSong
import com.example.musify.ui.viewmodels.MainViewModel
import com.example.musify.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_song_new.*
import kotlinx.android.synthetic.main.fragment_song_new.ivPlayPauseDetail
import kotlinx.android.synthetic.main.fragment_song_new.ivSkip
import kotlinx.android.synthetic.main.fragment_song_new.ivSkipPrevious
import kotlinx.android.synthetic.main.fragment_song_new.ivSongImage
import kotlinx.android.synthetic.main.fragment_song_new.seekBar
import kotlinx.android.synthetic.main.fragment_song_new.tvCurTime
import kotlinx.android.synthetic.main.fragment_song_new.tvSongDuration
import kotlinx.android.synthetic.main.fragment_song_new.tvSongName
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment:Fragment(R.layout.fragment_song_new ) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var currPlayingSong: Song? = null
    private var playbackState :PlaybackStateCompat? = null
    private var shouldUpdateSeekbar : Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        //requireActivity because this viewmodel is bound to activity lifecycle not fragment lifecycle
        subscribeToObservers()

        ivPlayPauseDetail.setOnClickListener{
            currPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,toggle = true)
            }
        }

        ivSkipPrevious.setOnClickListener{
            mainViewModel.skipToPreviousSong()
        }
        ivSkip.setOnClickListener {
            mainViewModel.skipToNextSong()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    setCurrPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar = true
                }
            }
        })
    }

    private fun updateTitleAndSongImage(song: Song){
        tvSongName.text = song.title
        tvSongArtist.text = song.subtitle
        glide.load(song.imageUrl).into(ivSongImage)
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){
            it?.let { result ->
                when(result.status){
                    SUCCESS ->{
                        result.data?.let { songs ->
                            if (currPlayingSong == null && songs.isNotEmpty()){ // mean just launch fragment
                                currPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
        mainViewModel.currPlayingSong.observe(viewLifecycleOwner){
            if (it == null) return@observe
            currPlayingSong = it.toSong()
            updateTitleAndSongImage(currPlayingSong!!)
        }
        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState = it
            ivPlayPauseDetail.setImageResource(
                if(playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            seekBar.progress = it?.position?.toInt() ?: 0
        }
        songViewModel.currPlayerPosition.observe(viewLifecycleOwner){
            if(shouldUpdateSeekbar){
                seekBar.progress = it.toInt()
                setCurrPlayerTimeToTextView(it)
            }
        }
        songViewModel.currSongDuration.observe(viewLifecycleOwner){
            seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            tvSongDuration.text = dateFormat.format(it)
        }
    }
    private fun setCurrPlayerTimeToTextView(ms:Long){
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        tvCurTime.text = dateFormat.format(ms)
    }
}