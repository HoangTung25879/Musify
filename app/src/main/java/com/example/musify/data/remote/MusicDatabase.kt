package com.example.musify.data.remote

import android.util.Log
import com.example.musify.data.Constants.SONG_COLLECTION
import com.example.musify.data.entities.Song
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getAllSongs():List<Song>{
        FirebaseFirestore.setLoggingEnabled(true)
        return try{
           songCollection.get().await().toObjects(Song::class.java)
//            songCollection.get().addOnSuccessListener { result->
//                for (document in result){
//                    Log.d("AAAA", "${document.id} => ${document.data}")
//                }
//            }.addOnFailureListener { exception->
//                Log.w("AAAA", "Error getting documents.", exception)
//            }
        } catch (e:Exception){
            Log.d("AAAA", "CATCH")
            emptyList<Song>()
        }
    }
}