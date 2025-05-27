package com.example.musicplayer.audio

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object AudioSaver {
    fun saveToRecordings(context: Context, sourceFile: File, fileName: String = "recorded_audio_${System.currentTimeMillis()}.m4a"): Uri? {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4") // Hoặc "audio/mpeg" nếu là .mp3
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Recordings")
        }

        val audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val audioUri = resolver.insert(audioCollection, contentValues)

        if (audioUri != null) {
            try {
                resolver.openOutputStream(audioUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return audioUri
            } catch (e: IOException) {

                // Nếu ghi thất bại thì xóa luôn entry đã insert
                resolver.delete(audioUri, null, null)
            }
        }

        return null
    }
}
