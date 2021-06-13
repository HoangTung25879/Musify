package com.example.musify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.musify.data.entities.Song
import com.example.musify.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

inline fun <T> sdk29AndUp(onSdk29: () -> T): T? {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29()
    } else null
}
object Config{
    var isInitial = true
    var isRepeat = false
    var isShuffle = false
    var isLocal = false
    var currentSongSelect : Song? = null
    var isConnected = false
    var currentSongList : List<Song>? = null
}
fun checkAllPermission(context: Context):Boolean{
    var result = false
    val hasReadPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (hasReadPermission && hasRecordPermission){
        result = true
    }
    return result
}
fun checkReadPermission(context: Context) : Boolean{
    var result = false
    val hasReadPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    if (hasReadPermission){
        result = true
    }
    return result
}
fun durationFormat(duration:String):String{
    val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
    return dateFormat.format(duration.toLong())
}
fun randomNumber(): Int{
    val start = 60
    val end = 500
    return (Math.random() * (end - start + 1)).toInt() + start
}
