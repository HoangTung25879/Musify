package com.example.musify.Util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.musify.Music
import java.util.concurrent.TimeUnit

fun getAllAudioFromDevice(context: Context):List<Music>{
    val musicList = mutableListOf<Music>()
    val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
    val projection =  arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST
    )
    // Show only audio that are at least 2 minutes in duration.
    val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
    val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES).toString()
    )
    // Display audios in alphabetical order based on their display name.
    val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
    val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
    )
    query?.use {cursor ->
        // Cache column indices.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val albumColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn).replace(".mp3","").replace(".wav","")
            val album = cursor.getString(albumColumn)
            val artist= cursor.getString(artistColumn)
            val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
            )
            // Stores column values and the contentUri in a local object
            // that represents the media file.
            musicList += Music(contentUri, name, album, artist)
        }
    }
    return musicList
}

// Checks if a volume containing external storage is available for read and write.
fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

// Checks if a volume containing external storage is available to at least read.
fun isExternalStorageReadable(): Boolean {
    return Environment.getExternalStorageState() in
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}
