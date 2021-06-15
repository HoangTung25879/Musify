package com.example.musify.ui.fragments

import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentCustomDialogBinding
import com.example.musify.sdk29AndUp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_custom_dialog.*
import kotlinx.android.synthetic.main.fragment_custom_dialog.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.lang.Exception

private const val TAG = "DOWNLOADDIALOGFRAGMENT"
class DownloadDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentCustomDialogBinding

    interface Callback{
        fun onFinishDownload(isSuccess: Boolean)
    }
    var callback : Callback? = null
    var song = Config.currentSongSelect
    var songList = Config.currentSongList

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_custom_dialog,container,false)
        binding.apply {
            tvTitle.text = "Download"
            tvDescribe.text = "Do you want to download ${song?.title}?"
            btnAcceptDownload.isVisible = true
            btnCancel.setOnClickListener { dismiss() }
            btnAcceptDownload.setOnClickListener {
                btnCancel.isClickable = false
                btnAcceptDownload.startLoading()
                lifecycleScope.launch(Dispatchers.IO) {
                    downloadSong()
                }
            }
        }
        return binding.root
    }
    suspend fun downloadSong(){
        val httpsRef = Firebase.storage.getReferenceFromUrl(song?.songUrl!!)
        val audioCollection = sdk29AndUp {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME,"${song?.title}.mp3")
            put(MediaStore.Audio.Media.MIME_TYPE,"audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH,Environment.DIRECTORY_MUSIC)
            put(MediaStore.Audio.Media.IS_PENDING,1)
        }
        try {
            val uri = requireContext().contentResolver.insert(audioCollection,contentValues)!!
            requireContext().contentResolver.openOutputStream(uri)?.use { opStream ->
                httpsRef.stream.await().stream.use { ipStream ->
//                val buffer = ByteArray(1024)
//                while (true){
//                    val bytes = ipStream.read(buffer)
//                    if (bytes == -1) break
//                    opStream.write(buffer,0,bytes)
//                }
                    ipStream.copyTo(opStream)
                }
            }
            contentValues.apply {
                clear()
                put(MediaStore.Audio.Media.IS_PENDING,0)
            }
            requireContext().contentResolver.update(uri,contentValues,null)
            activity?.runOnUiThread {
                binding.btnAcceptDownload.doResult(true)
                delayThenDismiss(true)
            }
        }catch (e:Exception){
            activity?.runOnUiThread {
                binding.btnAcceptDownload.doResult(false)
                delayThenDismiss(false)
            }
        }
    }
    private fun delayThenDismiss(isSuccess: Boolean = false){
        binding.apply {
            btnAcceptDownload.isClickable = false
            btnAcceptUpload.isClickable = false
            btnAcceptDelete.isClickable = false
            btnCancel.isClickable = false
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            withContext(Dispatchers.Main){
                callback?.onFinishDownload(isSuccess)
                dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            //width = 335dp in xxhdpi
            setLayout(1000,ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}