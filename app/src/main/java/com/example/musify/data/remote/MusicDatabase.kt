package com.example.musify.data.remote

import com.example.musify.data.Constants.SONG_COLLECTION
import com.example.musify.data.entities.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getAllSongs():List<Song>{
        return try{
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e:Exception){
            emptyList<Song>()
        }
    }
}