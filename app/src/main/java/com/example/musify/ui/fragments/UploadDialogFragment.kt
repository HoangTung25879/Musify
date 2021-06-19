package com.example.musify.ui.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.musify.*
import com.example.musify.data.Constants
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentCustomDialogBinding
import com.example.musify.ui.viewmodels.MainViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_custom_dialog.*
import kotlinx.android.synthetic.main.fragment_custom_dialog.view.*
import kotlinx.coroutines.*
import java.io.File

private const val TAG = "UPLOADDIALOGFRAGMENT"
class UploadDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentCustomDialogBinding
    interface Callback{
        fun onFinishUpload(isSuccess: Boolean)
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
            tvTitle.text = "Upload"
            tvDescribe.text = "Do you want to upload ${song?.title}?"
            btnAcceptUpload.isVisible = true
            btnCancel.setOnClickListener {
                dismiss()
            }
            btnAcceptUpload.setOnClickListener {
                btnCancel.isClickable = false
                btnAcceptUpload.startLoading()
                uploadSong()
            }
        }
        return binding.root
    }
    private fun uploadSong(){
        val mediaId = generateRandomString()
        val storageRef = Firebase.storage.reference
        var audioPath = URIPathHelper().getPath(requireContext(), Uri.parse(song?.songUrl))
        var file = Uri.fromFile(File(audioPath!!))
        val songRef = storageRef.child("song/${file.lastPathSegment}")
        var uploadTask = songRef.putFile(file)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            songRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                writeToFirestore(song,downloadUri,mediaId)
            } else {
                binding.btnAcceptUpload?.doResult(false)
                delayThenDismiss(false)
            }
        }
    }
    private fun writeToFirestore(song: Song?,downloadUri: Uri?,mediaId: String){
        val db = Firebase.firestore
        val songData = hashMapOf(
                "title" to "${song?.title}",
                "subtitle" to "${song?.subtitle}",
                "songUrl" to "$downloadUri",
                "mediaId" to "$mediaId",
                "imageUrl" to "",
                "duration" to "${song?.duration}"
        )
        db.collection(Constants.SONG_COLLECTION).document("song${mediaId}")
                .set(songData)
                .addOnSuccessListener {
                    Log.d(TAG,"Write document success")
                    binding.btnAcceptUpload?.doResult(true)
                    delayThenDismiss(true)
                }.addOnFailureListener { e ->
                    Log.w(TAG,"Error writing document",e)
                    binding.btnAcceptUpload.doResult(false)
                    delayThenDismiss(false)
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
                callback?.onFinishUpload(isSuccess)
                dismiss()
            }
        }
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