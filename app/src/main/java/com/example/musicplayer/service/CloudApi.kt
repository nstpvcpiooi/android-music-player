package com.example.musicplayer.service

import com.example.musicplayer.model.CloudinaryResponse

// Interface định nghĩa các hàm của Cloud API
interface CloudApi {
    fun uploadFile(
        filePath: String,
        publicId: String, // Thêm publicId vào interface
        onStart: () -> Unit,
        onProgress: (bytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (response: CloudinaryResponse?) -> Unit,
        onError: (error: String) -> Unit
    )
}