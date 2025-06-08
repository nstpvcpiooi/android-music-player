package com.example.musicplayer.model

// Data class để chứa dữ liệu trả về từ Cloudinary
data class CloudinaryResponse(
    val secureUrl: String?,
    val publicId: String?,
    val format: String?,
    val bytes: Long?
)