package com.example.musicplayer.service

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CloudinaryApi : CloudApi {
    override fun uploadFile(
        filePath: String,
        onStart: () -> Unit,
        onProgress: (bytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (response: CloudinaryResponse?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        try {
            MediaManager.get().upload(filePath)
                .option("resource_type", "auto")
                .option("folder", "music-player-kotlin")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) = onStart()
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) =
                        onProgress(bytes, totalBytes)
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val cloudinaryResponse = CloudinaryResponse(
                            secureUrl = resultData?.get("secure_url")?.toString(),
                            publicId = resultData?.get("public_id")?.toString(),
                            format = resultData?.get("format")?.toString(),
                            bytes = resultData?.get("bytes") as? Long // Ép kiểu
                        )
                        // Gọi callback onSuccess với đối tượng response
                        onSuccess(cloudinaryResponse)
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e("CloudinaryUpload", "Error response raw: ${error?.description}")
                        onError(error?.description ?: "Unknown error")
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) =
                        onError("Upload rescheduled: ${error?.description}")
                }).dispatch()
        } catch (e: Exception) {
            onError("Cloudinary error: ${e.message}")
        }
    }
}
