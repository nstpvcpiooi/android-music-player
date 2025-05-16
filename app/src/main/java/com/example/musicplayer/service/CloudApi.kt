package com.example.musicplayer.service

interface CloudApi {
    fun uploadFile(
        filePath: String,
        onStart: () -> Unit,
        onProgress: (bytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (response: CloudinaryResponse?) -> Unit,
        onError: (error: String) -> Unit
    )
}
