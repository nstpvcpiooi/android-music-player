// PlayerActivityOnline.kt
package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity.Companion
import com.example.musicplayer.activity.PlayerActivity.Companion.min10
import com.example.musicplayer.activity.PlayerActivity.Companion.min30
import com.example.musicplayer.activity.PlayerActivity.Companion.min5
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.formatDuration
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity phát nhạc online từ DownloadActivity.downloadPlaylist
 */
class PlayerActivityOnline : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    private lateinit var binding: ActivityPlayerBinding
    private var musicService: MusicService? = null
    private var playlist: ArrayList<Music> = arrayListOf()
    private var songPosition: Int = 0
    private var isPlaying: Boolean = false
    private var seekBarJob: Job? = null
    var repeat: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy trực tiếp từ static bên DownloadActivity
        playlist     = DownloadActivity.downloadPlaylist
        songPosition = DownloadActivity.downloadIndex

        binding.backBtnPA.setOnClickListener { finish() }

        // Bind & start service
        Intent(this, MusicService::class.java).also {
            bindService(it, this, Context.BIND_AUTO_CREATE)
            startService(it)
        }

        // Play/Pause
        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic() else resumeMusic()
        }
        // Prev/Next
        binding.previousBtnPA.setOnClickListener { changeTrack(false) }
        binding.nextBtnPA.setOnClickListener     { changeTrack(true) }

        // Download
        binding.favouriteBtnPA.setImageResource(R.drawable.download_icon)
        binding.favouriteBtnPA.setOnClickListener {
            downloadCurrentTrack()
        }

        // Hide
        binding.recordingBtnPA.visibility      = View.GONE
        binding.stoprecordingBtnPA.visibility  = View.GONE

        // same Player Offline
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, PlayerActivity.musicService!!.mediaPlayer!!.audioSessionId)
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 13)
            }catch (e: Exception){Toast.makeText(this,  "Bad Android version", Toast.LENGTH_SHORT).show()}



        }
        binding.timerBtnPA.setOnClickListener {
            val timer = min5 || min10 || min30
            if(!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes"){ _, _ ->
                        min5 = false
                        min10 = false
                        min30 = false
                        PlayerActivity.binding.timerBtnPA.setColorFilter(
                            ContextCompat.getColor(this,
                            R.color.black_level2
                        ))
                    }
                    .setNegativeButton("No"){dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                setDialogBtnBackground(this, customDialog)
            }
        }

        binding.repeatBtnPA.setOnClickListener {
            if(!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.black_level2))
            }
        }

        // SeekBar
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, prog: Int, fromUser: Boolean) {
                if (fromUser) musicService?.mediaPlayer?.seekTo(prog)
            }
            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar)  = Unit
        })
    }

    /**
     * Tải bài đang phát về thư mục Music của thiết bị
     */
    private fun downloadCurrentTrack() {
        val music = playlist.getOrNull(songPosition)
        if (music == null) {
            Toast.makeText(this, "Không có bài nào để tải", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val url = music.path
            val title = music.title
            // Lấy đuôi file từ URL, nếu không có thì .mp3
            val ext = MimeTypeMap.getFileExtensionFromUrl(url)
                ?.takeIf { it.isNotBlank() }
                ?.let { ".$it" }
                ?: ".mp3"

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Đang tải: $title")
                setDescription("Download nhạc trực tuyến…")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "$title$ext")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(this, "Bắt đầu tải “$title”…", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi tải: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {

        PlayerActivity.musicListPA = playlist
        PlayerActivity.songPosition = songPosition

        musicService = (binder as MusicService.MyBinder).currentService()
        musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        musicService!!.audioManager.requestAudioFocus(
            musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        prepareAndPlay()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    private fun prepareAndPlay() {
        stopSeekBarUpdates()

        musicService?.mediaPlayer?.let { oldMp ->
            oldMp.setOnPreparedListener(null)
            oldMp.setOnCompletionListener(null)
            oldMp.setOnErrorListener(null)
            oldMp.release()
        }

        val mp = MediaPlayer().also { musicService!!.mediaPlayer = it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }

        val url = playlist.getOrNull(songPosition)?.path ?: return
        mp.setDataSource(this@PlayerActivityOnline, Uri.parse(url))

        mp.setOnPreparedListener { player ->
            player.start()
            isPlaying = true
            updateUI(player)
            startSeekBarUpdates()
            musicService?.showNotification(
                if (isPlaying) R.drawable.pause_icon else R.drawable.play_icon
            )
        }

        mp.setOnCompletionListener(this)

        mp.setOnErrorListener { _, what, extra ->
            Toast.makeText(
                this@PlayerActivityOnline,
                "Lỗi phát nhạc (code=$what, extra=$extra)",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        mp.prepareAsync()
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI(mp: MediaPlayer) {
        val m = playlist[songPosition]
        binding.songNamePA.text        = m.title
        binding.tvSeekBarEnd.text      = formatDuration(mp.duration.toLong())
        binding.seekBarPA.max          = mp.duration
        binding.tvSeekBarStart.text    = formatDuration(mp.currentPosition.toLong())
    }

    private fun changeTrack(next: Boolean) {
        if (playlist.isEmpty()) return
        songPosition = if (next) (songPosition + 1) % playlist.size
        else (songPosition - 1).let { if (it < 0) playlist.lastIndex else it }
        // Đồng bộ static index
        DownloadActivity.downloadIndex = songPosition
        prepareAndPlay()
    }

    private fun startSeekBarUpdates() {
        seekBarJob?.cancel()
        seekBarJob = lifecycleScope.launch {
            while (isPlaying) {
                musicService?.mediaPlayer?.let {
                    binding.tvSeekBarStart.text = formatDuration(it.currentPosition.toLong())
                    binding.seekBarPA.progress  = it.currentPosition
                }
                delay(500)
            }
        }
    }

    private fun stopSeekBarUpdates() {
        seekBarJob?.cancel()
        seekBarJob = null
    }

    private fun resumeMusic() {
        musicService?.mediaPlayer?.start()
        isPlaying = true
        binding.playPauseImgPA.setImageResource(R.drawable.pause_icon)
        musicService?.showNotification(R.drawable.pause_icon)
        startSeekBarUpdates()
    }

    private fun pauseMusic() {
        musicService?.mediaPlayer?.pause()
        isPlaying = false
        binding.playPauseImgPA.setImageResource(R.drawable.play_icon)
        musicService?.showNotification(R.drawable.play_icon)
        stopSeekBarUpdates()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (repeat) {
            // Lặp lại bài hiện tại
            prepareAndPlay()
        } else {
            // Chuyển bài tiếp theo
            changeTrack(next = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
        stopSeekBarUpdates()
        musicService?.mediaPlayer?.release()
    }

    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivityOnline)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 5 minutes", Toast.LENGTH_SHORT).show()
            PlayerActivity.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min5 = true
            Thread{Thread.sleep((5 * 60000).toLong())
                if(min5) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 10 minutes", Toast.LENGTH_SHORT).show()
            PlayerActivity.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min10 = true
            Thread{Thread.sleep((10 * 60000).toLong())
                if(min10) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            PlayerActivity.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if(min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }
}
