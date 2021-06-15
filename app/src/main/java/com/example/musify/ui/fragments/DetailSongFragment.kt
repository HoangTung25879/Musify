package com.example.musify.ui.fragments

import android.content.DialogInterface
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bullhead.equalizer.DialogEqualizerFragment
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
import com.google.android.material.snackbar.Snackbar
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

    private var currPlayingSong: Song? = null
    private var playbackState :PlaybackStateCompat? = null
    private var shouldUpdateSeekbar : Boolean = true
    private var job: Job? = null
    private var equalizerFragment : DialogEqualizerFragment? = null
    private lateinit var navHostFragment : Fragment


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
            ivRepeat.setImageResource(if(Config.isRepeat) R.drawable.ic_repeat_on else R.drawable.ic_repeat)
            ivRepeat.setOnClickListener {
                Config.isRepeat = !Config.isRepeat
                if (Config.isRepeat){
                    ivRepeat.setImageResource(R.drawable.ic_repeat_on)
                } else {
                    ivRepeat.setImageResource(R.drawable.ic_repeat)
                }
            }
            ivShuffle.setImageResource(if(Config.isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
            ivShuffle.setOnClickListener {
                Config.isShuffle = !Config.isShuffle
                if (Config.isShuffle){
                    ivShuffle.setImageResource(R.drawable.ic_shuffle_on)
                } else {
                    ivShuffle.setImageResource(R.drawable.ic_shuffle)
                }
            }
            tvEqualizer.setOnClickListener {
                equalizerFragment?.show(childFragmentManager,"EQ")
            }
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
            navHostFragment = parentFragmentManager.findFragmentById(R.id.navHostFragment)!!
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

    private fun updateTitleAndSongImage(song: Song){
        binding.apply {
            tvSongName.text = song.title
            tvSongArtist.text = song.subtitle
            glide.asBitmap().load(if(song.imageUrl == "") R.drawable.music else song.imageUrl).into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    ivSongImage.setCoverImage(resource)
                    ivSongImageDefault.setImageBitmap(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    TODO("Not yet implemented")
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

    private fun startAnimation(){
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
        stopAnimation()
    }
    private fun stopAnimation(){
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
                STATE_PLAYING -> startAnimation()
                STATE_PAUSED -> stopAnimation()
                else -> Unit
            }
            binding.seekBar.progress = it?.position?.toInt() ?: 0
        }
        mainViewModel.currSongDuration.observe(viewLifecycleOwner){
            binding.apply {
                seekBar.max = it.toInt()
                val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
                tvSongDuration.text = dateFormat.format(it)
            }
        }
        mainViewModel.currPlayerPosition.observe(viewLifecycleOwner){
            if(shouldUpdateSeekbar){
                binding.apply {
                    seekBar.progress = it.toInt()
                    setCurrPlayerTimeToTextView(it)
                }
            }
        }

        mainViewModel.audioSessionId.observe(viewLifecycleOwner){
            if (it != -1){
                equalizerFragment = DialogEqualizerFragment.Builder().setAudioSessionId(it)
                    .themeColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
                    .textColor(ContextCompat.getColor(requireContext(), R.color.textColor))
                    .accentAlpha(ContextCompat.getColor(requireContext(), R.color.playingCardColor))
                    .darkColor(ContextCompat.getColor(requireContext(), R.color.primaryDarkColor))
                    .setAccentColor(ContextCompat.getColor(requireContext(), R.color.secondaryColor))
                    .build()
            }
        }
    }

    private fun setCurrPlayerTimeToTextView(ms:Long){
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.tvCurTime.text = dateFormat.format(ms)
    }
}