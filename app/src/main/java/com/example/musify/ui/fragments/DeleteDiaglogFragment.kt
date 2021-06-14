package com.example.musify.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.URIPathHelper
import com.example.musify.data.Constants
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentCustomDialogBinding
import com.example.musify.randomNumber
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_custom_dialog.*
import kotlinx.coroutines.*
import java.io.File

private const val TAG = "DELETEDIALOGFRAGMENT"
class DeleteDiaglogFragment : DialogFragment() {
    private lateinit var binding: FragmentCustomDialogBinding
    interface Callback{
        fun onFinishDelete(isSuccess: Boolean)
    }
    var callback : Callback? = null
    var song = Config.currentSongSelect
    var songList = Config.currentSongList

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_custom_dialog,container,false)
        binding.apply {
            tvTitle.text = "Delete"
            tvDescribe.text = "Do you want to delete ${song?.title}?"
            btnAcceptDelete.isVisible = true
            btnCancel.setOnClickListener {
                dismiss()
            }
            btnAcceptDelete.setOnClickListener {
                btnCancel.isClickable = false
                btnAcceptDelete.startLoading()
                if(song?.isLocal == true) deleteLocal() else deleteOnline()
            }
        }
        return binding.root
    }
    private fun deleteLocal(){
        btnAcceptDelete.doResult(false)
        delayThenDismiss(false)
    }
    private fun deleteOnline(){
        val db = Firebase.firestore
        db.collection(Constants.SONG_COLLECTION).document("song${song?.mediaId}")
                .delete().addOnSuccessListener {
                    btnAcceptDelete.doResult(true)
                    delayThenDismiss(true)
                }.addOnFailureListener {e ->
                    Log.w(TAG,"Error deleting document",e)
                    btnAcceptDelete.doResult(false)
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
                if (isSuccess){
                    callback?.onFinishDelete(isSuccess)
                }
                dismiss()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            //width = 335dp in xxhdpi
            setLayout(1000, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}