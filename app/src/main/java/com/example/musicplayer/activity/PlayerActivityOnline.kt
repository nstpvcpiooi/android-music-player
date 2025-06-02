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
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.PlayerActivity.Companion
import com.example.musicplayer.activity.PlayerActivity.Companion.min10
import com.example.musicplayer.activity.PlayerActivity.Companion.min5
import com.example.musicplayer.activity.PlayerActivity.Companion.min30
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.example.musicplayer.databinding.TimerBottomSheetBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.OnlineMusicService
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
    private var musicService: OnlineMusicService? = null
    private var playlist: ArrayList<Music> = arrayListOf()
    private var songPosition: Int = 0
    private var isPlaying: Boolean = false
    private var seekBarJob: Job? = null
    var repeat: Boolean = false

    @SuppressLint("SetTextI18s")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy trực tiếp từ static bên DownloadActivity
        playlist = ArrayList(DownloadActivity.downloadPlaylist)
        songPosition = DownloadActivity.downloadIndex

        binding.backBtnPA.setOnClickListener { finish() }

        // Bind & start service
        Intent(this, OnlineMusicService::class.java).also {
            bindService(it, this, Context.BIND_AUTO_CREATE)
            startService(it)
        }

        // Play/Pause
        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic() else resumeMusic()
        }
        // Prev/Next
        binding.previousBtnPA.setOnClickListener { changeTrack(false) }
        binding.nextBtnPA.setOnClickListener { changeTrack(true) }

        // Download
        binding.favouriteBtnPA.setImageResource(R.drawable.download_icon)
        binding.favouriteBtnPA.setOnClickListener {
            downloadCurrentTrack()
        }

        // Hide recording buttons if they exist
        try {
            if (binding::class.java.getDeclaredField("recordingBtnPA").get(binding) != null) {
                binding.recordingBtnPA.visibility = View.GONE
            }
        } catch (e: Exception) {
            // View may not exist in layout
        }

        // Timer button
        binding.timerBtnPA.setOnClickListener {
            val timer = min5 || min10 || min30
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes") { _, _ ->
                        min5 = false
                        min10 = false
                        min30 = false
                        binding.timerBtnPA.setColorFilter(
                            ContextCompat.getColor(this,
                                R.color.black_level2
                            )
                        )
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                setDialogBtnBackground(this, customDialog)
            }
        }

        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            } else {
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
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
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
        try {
            // Get our service instance
            musicService = (binder as OnlineMusicService.MyBinder).currentService()

            // Initialize audio manager
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(
                musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )

            // Don't directly set PlayerActivity's static variables, as they might be used by other components
            // Instead, rely on our own playlist and songPosition variables
            prepareAndPlay()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi kết nối dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            finish() // Close activity if we can't connect to the service
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    private fun prepareAndPlay() {
        stopSeekBarUpdates()

        try {
            // Safely release the previous media player if it exists
            musicService?.mediaPlayer?.let { oldMp ->
                try {
                    oldMp.setOnPreparedListener(null)
                    oldMp.setOnCompletionListener(null)
                    oldMp.setOnErrorListener(null)
                    if (oldMp.isPlaying) {
                        oldMp.stop()
                    }
                    oldMp.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Create a new MediaPlayer
            val mp = MediaPlayer().also { musicService!!.mediaPlayer = it }

            // Configure audio attributes based on API level
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

            // Safety check for playlist and current song position
            if (songPosition < 0 || songPosition >= playlist.size) {
                songPosition = 0
                if (playlist.isEmpty()) {
                    Toast.makeText(this, "Danh sách phát trống", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Get URL for the current song
            val url = playlist[songPosition].path
            if (url.isBlank() || !url.startsWith("http")) {
                Toast.makeText(this, "URL không hợp lệ: $url", Toast.LENGTH_SHORT).show()
                return
            }

            // Set data source and prepare
            mp.setDataSource(this@PlayerActivityOnline, Uri.parse(url))

            mp.setOnPreparedListener { player ->
                player.start()
                isPlaying = true
                updateUI(player)
                startSeekBarUpdates()
                musicService?.showNotification(R.drawable.pause_icon)
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

            // Start preparing the media player asynchronously
            mp.prepareAsync()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi chuẩn bị phát nhạc: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
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
        val bottomSheetBinding = TimerBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(bottomSheetBinding.root)
        dialog.show()

        bottomSheetBinding.timerOptionsNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.timer_5_min -> {
                    Toast.makeText(baseContext, "Music will stop after 5 minutes", Toast.LENGTH_SHORT).show()
                    PlayerActivity.Companion.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
                    min5 = true
                    Thread {
                        Thread.sleep((5 * 60000).toLong())
                        if (min5) exitApplication()
                    }.start()
                    dialog.dismiss()
                    true
                }
                R.id.timer_10_min -> {
                    Toast.makeText(baseContext, "Music will stop after 10 minutes", Toast.LENGTH_SHORT).show()
                    PlayerActivity.Companion.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
                    min10 = true
                    Thread {
                        Thread.sleep((10 * 60000).toLong())
                        if (min10) exitApplication()
                    }.start()
                    dialog.dismiss()
                    true
                }
                R.id.timer_30_min -> {
                    Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
                    PlayerActivity.Companion.binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
                    min30 = true
                    Thread {
                        Thread.sleep((30 * 60000).toLong())
                        if (min30) exitApplication()
                    }.start()
                    dialog.dismiss()
                    true
                }
                else -> false
            }
        }
    }
}
