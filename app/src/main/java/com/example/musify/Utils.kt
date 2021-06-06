package com.example.musify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

inline fun <T> sdk29AndUp(onSdk29: () -> T): T? {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29()
    } else null
}
object Config{
    var isLocalSong = false
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