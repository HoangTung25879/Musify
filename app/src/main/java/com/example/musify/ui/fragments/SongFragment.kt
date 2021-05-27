package com.example.musify.ui.fragments

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.musify.R
import com.example.musify.data.Status
import com.example.musify.data.Status.SUCCESS
import com.example.musify.data.entities.Song
import com.example.musify.exoplayer.*
import com.example.musify.ui.viewmodels.MainViewModel
import com.example.musify.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_song.*
import kotlinx.android.synthetic.main.fragment_song.ivPlayPauseDetail
import kotlinx.android.synthetic.main.fragment_song.ivSkip
import kotlinx.android.synthetic.main.fragment_song.ivSkipPrevious
import kotlinx.android.synthetic.main.fragment_song.ivSongImage
import kotlinx.android.synthetic.main.fragment_song.seekBar
import kotlinx.android.synthetic.main.fragment_song.tvCurTime
import kotlinx.android.synthetic.main.fragment_song.tvSongDuration
import kotlinx.android.synthetic.main.fragment_song.tvSongName
import me.bogerchan.niervisualizer.NierVisualizerManager
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment:Fragment(R.layout.fragment_song ) {
    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var currPlayingSong: Song? = null
    private var playbackState :PlaybackStateCompat? = null
    private var shouldUpdateSeekbar : Boolean = true

    private var mVisualizerManager: NierVisualizerManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        //requireActivity because this viewmodel is bound to activity lifecycle not fragment lifecycle
        subscribeToObservers()
        songVisualizer.setZOrderOnTop(true)
        songVisualizer.holder.setFormat(PixelFormat.TRANSLUCENT)
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
        ivBackBtn.setOnClickListener {
            navHostFragment.findNavController().popBackStack()
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
        mainViewModel.audioSessionId.observe(viewLifecycleOwner){
            if (it != -1){
                createNewVisualizeManager(it)
            }
        }
    }
    private fun createNewVisualizeManager(audioSessionId: Int){
        mVisualizerManager?.release()
        mVisualizerManager = NierVisualizerManager().apply {
            init(audioSessionId)
        }
        mVisualizerManager?.start(songVisualizer,arrayOf(CircleBarRenderer(
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 15f
                color = Color.parseColor("#70DBF0")
            },
            amplification = 1.5f
        )))
    }

    override fun onDestroy() {
        super.onDestroy()
        mVisualizerManager?.release()
        mVisualizerManager = null
    }
    private fun setCurrPlayerTimeToTextView(ms:Long){
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        tvCurTime.text = dateFormat.format(ms)
    }
}