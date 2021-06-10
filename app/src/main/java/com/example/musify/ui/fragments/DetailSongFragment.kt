package com.example.musify.ui.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.data.Status.SUCCESS
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentSongBinding
import com.example.musify.exoplayer.*
import com.example.musify.ui.viewmodels.MainViewModel
import com.example.musify.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_song.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private val TAG = "DETAILSONGFRAGMENT"
@AndroidEntryPoint
class DetailSongFragment:Fragment() {
    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentSongBinding
    private val songViewModel: SongViewModel by viewModels()

    private var currPlayingSong: Song? = null
    private var playbackState :PlaybackStateCompat? = null
    private var shouldUpdateSeekbar : Boolean = true
    private var job: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupViewModel(inflater,container)
            val view = binding.root
            return view
    }
    private fun setupViewModel(inflater: LayoutInflater,container: ViewGroup?){
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_song,container,false)
        binding.lifecycleOwner = this
        binding.detailSongViewModel = mainViewModel
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //requireActivity because this viewmodel is bound to activity lifecycle not fragment lifecycle
        binding.apply {
            ivPlayPauseDetail.setOnClickListener{
                currPlayingSong?.let {
                    mainViewModel.playOrToggleSong(it,toggle = true)
                }
            }
            ivPreviousSong.setOnClickListener{
                mainViewModel.skipToPreviousSong()
            }
            ivNextSong.setOnClickListener {
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
        subscribeToObservers()
    }
    private fun togglePreviousSongBtn(disable:Boolean){
        binding.apply {
            if(disable){
                ivPreviousSong.setOnClickListener(null)
                ivPreviousSong.setBackgroundResource(R.drawable.next_previous_button_background_disable)
            } else {
                ivPreviousSong.setOnClickListener{
                    mainViewModel.skipToPreviousSong()
                }
                ivPreviousSong.setBackgroundResource(R.drawable.next_previous_button_background_enable)
            }
        }
    }
    private fun toggleNextSongBtn(disable: Boolean){
        binding.apply {
            if(disable){
                ivNextSong.setOnClickListener(null)
                ivNextSong.setBackgroundResource(R.drawable.next_previous_button_background_disable)
            } else {
                ivNextSong.setOnClickListener {
                    mainViewModel.skipToNextSong()
                }
                ivNextSong.setBackgroundResource(R.drawable.next_previous_button_background_enable)
            }
        }
    }
    private fun updateTitleAndSongImage(song: Song){
        binding.apply {
            tvSongName.text = song.title
            tvSongArtist.text = song.subtitle
//            glide.load(R.drawable.music).into(ivSongImage)
            glide.asBitmap().load(R.drawable.music).into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    ivSongImage.setCoverImage(resource!!)
                    ivSongImageDefault.setImageBitmap(resource!!)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })
        }
    }

    private fun newFloatArray(size: Int): FloatArray {
        val random = Random()
        val array = FloatArray(size)
        for (i in 0 until size) {
            array[i] = random.nextInt(75).toFloat()
        }
        return array
    }

    private fun startSpinAnimation(){
        binding.apply {
            ivSongImage.isVisible = true
            ivSongImageDefault.isVisible = false
        }
        job = GlobalScope.launch(Dispatchers.Default){
            while(playbackState?.state == STATE_PLAYING){
                val array = newFloatArray(binding.ivSongImage.getNumberOfBars())
                withContext(Dispatchers.Main){
                    binding.ivSongImage.setWaveHeights(array)
                }
                delay(250)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpinAnimation()
    }
    private fun stopSpinAnimation(){
        binding.apply {
            ivSongImage.isVisible = false
            ivSongImageDefault.isVisible = true
        }
        job?.cancel()
        binding.ivSongImage.showFullCover()
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
            binding.ivPlayPauseDetail.setImageResource(
                if(playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            when(it?.state){
                STATE_PLAYING -> startSpinAnimation()
                STATE_PAUSED -> stopSpinAnimation()
                else -> Unit
            }
            binding.seekBar.progress = it?.position?.toInt() ?: 0
        }
        songViewModel.currSongDuration.observe(viewLifecycleOwner){
            binding.apply {
                seekBar.max = it.toInt()
                val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
                tvSongDuration.text = dateFormat.format(it)
            }
        }
        songViewModel.currPlayerPosition.observe(viewLifecycleOwner){
            if(shouldUpdateSeekbar){
                binding.apply {
                    seekBar.progress = it.toInt()
                    setCurrPlayerTimeToTextView(it)
                }
            }
        }

        mainViewModel.audioSessionId.observe(viewLifecycleOwner){
            if (it != -1){
            }
        }
    }

    private fun setCurrPlayerTimeToTextView(ms:Long){
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.tvCurTime.text = dateFormat.format(ms)
    }
}