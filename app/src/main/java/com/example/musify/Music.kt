package com.example.musify

import android.net.Uri

data class Music(
        val path:Uri,
        val name:String,
        val album:String,
        val artist:String,
)
