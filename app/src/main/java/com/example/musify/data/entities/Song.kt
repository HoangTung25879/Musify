package com.example.musify.data.entities

data class Song(
        val mediaId:String = "",
        val subtitle:String = "",
        val title:String = "",
        val songUrl:String = "",
        val imageUrl:String = "",
        val isLocal:Boolean = false,
        var isPlaying:Boolean = false,
        val duration:String = "") {
    override fun toString(): String {
        return "${title} - ${isLocal} - ${isPlaying}"
    }
}

//data class Song(
//        val id: Long,
//        val url: String,
//        val songName: String,
//        val artistName: String,
//        val albumName: String
//)