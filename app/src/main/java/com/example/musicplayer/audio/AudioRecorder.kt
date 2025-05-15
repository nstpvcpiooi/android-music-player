package com.example.musicplayer.audio

import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val outputFile: File) {
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    fun startRecording() {
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            if (isRecording) {
                recorder?.apply {
                    stop()
                    release()
                }
                isRecording = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
    }
}
