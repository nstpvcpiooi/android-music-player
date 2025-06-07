package com.example.musicplayer.service

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.musicplayer.model.CloudinaryResponse

// Class này implement interface CloudApi
class CloudinaryApi : CloudApi {
    override fun uploadFile(
        filePath: String,
        publicId: String, // Nhận publicId từ Activity
        onStart: () -> Unit,
        onProgress: (bytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (response: CloudinaryResponse?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        try {
            MediaManager.get().upload(filePath)
                .option("resource_type", "video") // Audio được coi là video
                .option("folder", "music-player-kotlin") // Tên thư mục trên Cloudinary
                .option("public_id", publicId) // Đặt tên file trên Cloudinary
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) = onStart()

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) =
                        onProgress(bytes, totalBytes)

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        // Chuyển đổi Map thành đối tượng CloudinaryResponse
                        val cloudinaryResponse = resultData?.let {
                            CloudinaryResponse(
                                secureUrl = it["secure_url"]?.toString(),
                                publicId = it["public_id"]?.toString(),
                                format = it["format"]?.toString(),
                                bytes = it["bytes"] as? Long
                            )
                        }
                        onSuccess(cloudinaryResponse)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        val errorMessage = error?.description ?: "Unknown Cloudinary error"
                        Log.e("CloudinaryUpload", "Error: $errorMessage")
                        onError(errorMessage)
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        val errorMessage = "Upload rescheduled: ${error?.description}"
                        Log.w("CloudinaryUpload", errorMessage)
                        onError(errorMessage)
                    }
                }).dispatch()
        } catch (e: Exception) {
            val errorMessage = "Cloudinary dispatch error: ${e.message}"
            Log.e("CloudinaryUpload", errorMessage, e)
            onError(errorMessage)
        }
    }
}