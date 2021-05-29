package com.example.musify.data.entities

data class Song(
        val mediaId:String = "",
        val subtitle:String = "",
        val title:String = "",
        val songUrl:String = "",
        val imageUrl:String = "") {
}

//data class Song(
//        val id: Long,
//        val url: String,
//        val songName: String,
//        val artistName: String,
//        val albumName: String
//)