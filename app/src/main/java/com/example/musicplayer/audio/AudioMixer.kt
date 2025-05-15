package com.example.musicplayer.audio

import android.content.Context
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

object AudioMixer {
    fun mixAudio(
        context: Context,
        musicFile: File,
        voiceFile: File,
        outputFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        // Câu lệnh FFmpeg để mix
        val command = "-i ${musicFile.absolutePath} -i ${voiceFile.absolutePath} " +
                "-filter_complex \"[0:a]volume=0.3[a0];[1:a][a0]amix=inputs=2:duration=longest\" " +
                "-c:a libmp3lame -q:a 2 ${outputFile.absolutePath}"

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            val success = returnCode.isValueSuccess

            if (success) {
                // Hiển thị thông báo khi mix thành công
                Toast.makeText(context, "Mix thành công!", Toast.LENGTH_SHORT).show()

                // Lưu file vào bộ nhớ khi mix thành công
                val savedUri = AudioSaver.saveToRecordings(context, outputFile)
                if (savedUri != null) {
                    Toast.makeText(context, "File đã được lưu thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lưu file thất bại!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Thông báo nếu mix thất bại
                Toast.makeText(context, "Mix thất bại!", Toast.LENGTH_SHORT).show()
            }

            // Gọi callback sau khi hoàn thành
            onComplete(success)
        }
    }
}
