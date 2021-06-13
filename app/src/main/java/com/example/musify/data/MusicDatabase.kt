package com.example.musify.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.musify.Config
import com.example.musify.data.Constants.SONG_COLLECTION
import com.example.musify.data.entities.Song
import com.example.musify.sdk29AndUp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

private const val TAG = "MUSICDATABASE"
class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getFirebaseSongs(): List<Song> {
        if(Config.isConnected == false){
            return mutableListOf<Song>()
        }
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e: Exception) {
            mutableListOf<Song>()
        }
    }
//    private val firestore = Firebase.firestore
//    private val songCollection = firestore.collection(SONG_COLLECTION)
//    suspend fun getFirebaseSongs(): List<Song>{
//        val songList = mutableListOf<Song>()
//        songCollection.get().addOnSuccessListener { documents ->
//            for (document in documents){
//                songList.add(document.toObject(Song::class.java))
//            }
//        }
//    }

    fun getLocalSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val collection = sdk29AndUp {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + "=1 OR " + MediaStore.Audio.Media.IS_PODCAST + "=1"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val query = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
        )
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val artist = cursor.getString(artistColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getString(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toLong()
                ).toString()
                //29 - NangTho-HoangDung-6413381 - Nàng Thơ (Single) - Hoàng Dũng - content://media/external/audio/media/29 - 254511 - Nàng Thơ - Nhạc Trẻ - null
                //30 - SaiGonDauLongQua-HuaKimTuyenHoangDuyen-6992977 - NhacCuaTui.com - Hứa Kim Tuyền, Hoàng Duyên - content://media/external/audio/media/30 - 308736 - Sài Gòn Đau Lòng Quá - Nhạc Trẻ - null
                //28 - ThanhXuan-DaLAB-5773854 - Thanh Xuan - Da LAB - content://media/external/audio/media/28 - 220552 - Thanh Xuan - R&B/Hip Hop/Rap - null
                // 27 - ThichQuaRoiNa-TlinhTrungTranWxrdieNgerPacman-6413849 - NhacCuaTui.com - Tlinh, Trung Trần, Wxrdie, Nger, Pacman - content://media/external/audio/media/27 - 177450 - Thích Quá Rồi Nà - Nhạc Trẻ - null
//                Log.d(TAG, "$id - $artist - $contentUri - $title - $duration")
                songList += Song(mediaId = id, subtitle = artist, title=title, songUrl = contentUri,isLocal = true,duration = duration)
            }
        }
        return songList.toList()
    }
}