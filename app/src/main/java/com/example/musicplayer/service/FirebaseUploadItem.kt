package com.example.musicplayer.model

import com.google.firebase.database.ServerValue

data class FirebaseUploadItem(
    val url: String = "",
    val title: String = "",
    val singer: String = "",
    val album: String = "",
    val timestamp: Any = ServerValue.TIMESTAMP
)