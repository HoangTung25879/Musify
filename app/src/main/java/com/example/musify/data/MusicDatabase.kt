package com.example.musify.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.musify.data.Constants.SONG_COLLECTION
import com.example.musify.data.entities.Song
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getAllSongs():List<Song>{
        FirebaseFirestore.setLoggingEnabled(true)
        return try{
           songCollection.get().await().toObjects(Song::class.java)
        } catch (e:Exception){
            emptyList<Song>()
        }
    }
    fun getCoverArt(path: String): Bitmap? {
        val mmr : MediaMetadataRetriever = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val data : ByteArray? = mmr.embeddedPicture
        if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.size)
        return null
    }
    fun getAllAudioFromDevice(context: Context):List<Song>{
        val musicList = mutableListOf<Song>()
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
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )
        // Show only audio that are less than 10 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} <= ?"
        val selectionArgs = arrayOf(
                TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES).toString()
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
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val genreColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val albumArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getString(idColumn)
                val name = cursor.getString(nameColumn).replace(".mp3","").replace(".wav","")
                val album = cursor.getString(albumColumn)
                val artist= cursor.getString(artistColumn)
                val contentUri: String = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toLong()
                ).toString()
                val duration = cursor.getLong(durationColumn)
                val title = cursor.getString(titleColumn)
                val genre = cursor.getString(genreColumn)
                val albumArtist = cursor.getString(albumArtistColumn)
                val albumID = cursor.getLong(albumIdColumn)
                val path = cursor.getString(dataColumn)
                //29 - NangTho-HoangDung-6413381 - Nàng Thơ (Single) - Hoàng Dũng - content://media/external/audio/media/29 - 254511 - Nàng Thơ - Nhạc Trẻ - null
                //30 - SaiGonDauLongQua-HuaKimTuyenHoangDuyen-6992977 - NhacCuaTui.com - Hứa Kim Tuyền, Hoàng Duyên - content://media/external/audio/media/30 - 308736 - Sài Gòn Đau Lòng Quá - Nhạc Trẻ - null
                //28 - ThanhXuan-DaLAB-5773854 - Thanh Xuan - Da LAB - content://media/external/audio/media/28 - 220552 - Thanh Xuan - R&B/Hip Hop/Rap - null
                // 27 - ThichQuaRoiNa-TlinhTrungTranWxrdieNgerPacman-6413849 - NhacCuaTui.com - Tlinh, Trung Trần, Wxrdie, Nger, Pacman - content://media/external/audio/media/27 - 177450 - Thích Quá Rồi Nà - Nhạc Trẻ - null
                Log.d("AAAA", "${id} - ${name} - $album - $artist - $contentUri - $duration - $title - $genre - $albumArtist - $")
               musicList += Song(mediaId = id,subtitle = artist,title=title,songUrl = contentUri,imageUrl = "")
            }
        }
        Log.d("TAG",musicList.size.toString())
        return musicList
    }
}