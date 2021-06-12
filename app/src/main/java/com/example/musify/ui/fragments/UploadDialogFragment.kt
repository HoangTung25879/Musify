package com.example.musify.ui.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.musify.Config
import com.example.musify.R
import com.example.musify.data.entities.Song
import com.example.musify.databinding.FragmentCustomDialogBinding
import kotlinx.android.synthetic.main.fragment_custom_dialog.*
import kotlinx.android.synthetic.main.fragment_custom_dialog.view.*

class UploadDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentCustomDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_custom_dialog,container,false)
        binding.apply {
            tvTitle.text = "Upload"
            tvDescribe.text = "Do you want to upload ${Config.currentSongSelect?.title}?"
            btnCancel.setOnClickListener { dismiss() }
            btnAccept.setOnClickListener { dismiss() }
        }
        return binding.root
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