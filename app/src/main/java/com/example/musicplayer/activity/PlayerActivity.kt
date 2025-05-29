package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.onprg.PlayNext
import com.example.musicplayer.onprg.PlaylistDetails
import com.example.musicplayer.R
import com.example.musicplayer.audio.AudioMixer
import com.example.musicplayer.audio.AudioRecorder
import com.example.musicplayer.audio.AudioSaver
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.example.musicplayer.databinding.AudioBoosterBinding
import com.example.musicplayer.fragment.PlayerMoreFeaturesBottomSheet
import com.example.musicplayer.model.toFile
import com.example.musicplayer.onprg.PlaylistActivity
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.favouriteChecker
import com.example.musicplayer.utils.formatDuration
import com.example.musicplayer.utils.getImgArt
import com.example.musicplayer.utils.setDialogBtnBackground
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    companion object {
        lateinit var binding: ActivityPlayerBinding // Moved and made public
        lateinit var musicListPA: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null
        var repeat: Boolean = false
        var min5: Boolean = false
        var min10: Boolean = false
        var min30: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        lateinit var loudnessEnhancer: LoudnessEnhancer
        lateinit var audioRecorder: AudioRecorder
        lateinit var voiceFile: File
        lateinit var mixedFile: File
        var isRecording: Boolean = false
    }

    private lateinit var pagerState: PagerState
    private lateinit var composableScope: CoroutineScope
    private var preparingSongId: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.data?.scheme.contentEquals("content")) {
            songPosition = 0
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))
            binding.songNamePA.text = musicListPA[songPosition].title
        } else {
            initializeLayout()
        }

        setupAlbumArtPager()

        voiceFile = File(cacheDir, "recorded_voice.m4a")
        mixedFile = File(cacheDir, "mixed_output.mp3")

        binding.boosterBtnPA.setOnClickListener {
            val customDialogB = LayoutInflater.from(this).inflate(R.layout.audio_booster, binding.root, false)
            val bindingB = AudioBoosterBinding.bind(customDialogB)
            val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                .setOnCancelListener { playMusic() }
                .setPositiveButton("OK") { self, _ ->
                    loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                    playMusic()
                    self.dismiss()
                }
                .setBackground(0x803700B3.toInt().toDrawable())
                .create()
            dialogB.show()
            bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt() / 100
            bindingB.progressText.text = "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt() / 10} %"
            bindingB.verticalBar.setOnProgressChangeListener {
                bindingB.progressText.text = "Audio Boost\n\n${it * 10} %"
            }
            setDialogBtnBackground(this, dialogB)
        }

        binding.backBtnPA.setOnClickListener { finish() }
        binding.playPauseBtnPA.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if (isPlaying) R.drawable.pause_icon else R.drawable.play_icon)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            } else {
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
        }

        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivity(eqIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Bad Android version: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.white))
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                setDialogBtnBackground(this, customDialog)
            }
        }

        binding.recordingBtnPA.setOnClickListener {
            if (!isRecording) {
                audioRecorder = AudioRecorder(voiceFile)
                audioRecorder.startRecording()
                Toast.makeText(this, "Đang ghi âm...", Toast.LENGTH_SHORT).show()
                isRecording = true
            } else {
                audioRecorder.stopRecording()
                try {
                    AudioSaver.saveToRecordings(this, voiceFile, "my_voice_${System.currentTimeMillis()}.m4a")
                } catch (e: Exception) {
                    Toast.makeText(this, "loi: ${e.message}", Toast.LENGTH_LONG).show()
                }
                var outputFile = File(getExternalFilesDir(null), "mixed_audio_${System.currentTimeMillis()}.mp3")
                var musicFile = musicListPA[songPosition].toFile()
                AudioMixer.mixAudio(this, musicFile, voiceFile, outputFile) { success ->
                    if (success) {
                        val savedUri = AudioSaver.saveToRecordings(this, outputFile)
                        if (savedUri != null) {
                            Toast.makeText(this, "File đã được lưu thành công!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Lưu file thất bại!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Mix thất bại!", Toast.LENGTH_SHORT).show()
                    }
                }
                isRecording = false
                Toast.makeText(this, "Dừng ghi âm", Toast.LENGTH_SHORT).show()
            }
        }

        binding.stoprecordingBtnPA.setOnClickListener {
            Toast.makeText(this, "Disabled!", Toast.LENGTH_SHORT).show()
        }

        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if (isFavourite) {
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else {
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
            FavouriteActivity.favouritesChanged = true
        }

        binding.moreInfoButtonPA.setOnClickListener {
            val currentSongId = nowPlayingId
            if (currentSongId.isNotEmpty()) {
                val bottomSheet = PlayerMoreFeaturesBottomSheet.newInstance(currentSongId)
                bottomSheet.show(supportFragmentManager, PlayerMoreFeaturesBottomSheet.TAG)
            } else {
                Toast.makeText(this, "Song ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun setupAlbumArtPager() {
        binding.albumArtPager.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val initialPageToShow = if (musicListPA.isNotEmpty()) {
                    songPosition.coerceIn(0, musicListPA.size - 1)
                } else 0

                val rememberedPagerState = rememberPagerState(
                    initialPage = initialPageToShow
                ) {
                    musicListPA.size
                }
                pagerState = rememberedPagerState
                composableScope = rememberCoroutineScope()

                AlbumArtPagerComposable(
                    musicList = musicListPA,
                    pagerState = pagerState,
                    onPageChange = { newPage ->
                        if (songPosition != newPage) {
                            songPosition = newPage
                            if (songPosition < musicListPA.size && songPosition >= 0) {
                                setLayout()
                                createMediaPlayer()
                            }
                        }
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AlbumArtPagerComposable(
        musicList: ArrayList<Music>,
        pagerState: PagerState,
        onPageChange: (Int) -> Unit
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .padding(25.dp)
            .clip(RoundedCornerShape(16.dp))

        if (musicList.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.music_player_icon_slash_screen),
                contentDescription = "Album Art Placeholder",
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
            return
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentMusic = musicList.getOrNull(page)
            if (currentMusic != null) {
                AsyncImage(
                    model = currentMusic.artUri.takeIf { it != "Unknown" && it.isNotEmpty() } ?: getImgArt(currentMusic.path),
                    placeholder = painterResource(id = R.drawable.music_player_icon_slash_screen),
                    error = painterResource(id = R.drawable.music_player_icon_slash_screen),
                    contentDescription = "Album Art for ${currentMusic.title}",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.music_player_icon_slash_screen),
                    contentDescription = "Album Art Placeholder",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (musicList.isNotEmpty() && page < musicList.size) {
                    onPageChange(page)
                }
            }
        }
    }

    private fun initializeLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "NowPlaying" -> {
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) binding.playPauseImgPA.setImageResource(R.drawable.pause_icon)
                else binding.playPauseImgPA.setImageResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch" -> initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter" -> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            "MainActivity" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = true)
            "FavouriteShuffle" -> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
            "PlaylistDetailsAdapter" ->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = false)
            "PlaylistDetailsShuffle" ->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = true)
            "PlayNext" -> initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)
        }
        if (musicService != null && !isPlaying) playMusic()
    }

    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        binding.songNamePA.text = musicListPA[songPosition].title
        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext, R.color.yellow))
        if (min5 || min10 || min30) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext, R.color.yellow))
        if (isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

        val img = getImgArt(musicListPA[songPosition].path)
        val image = if (img != null) {
            BitmapFactory.decodeByteArray(img, 0, img.size)
        } else {
            BitmapFactory.decodeResource(
                resources,
                R.drawable.music_player_icon_slash_screen
            )
        }

        Palette.from(image).generate { palette ->
            val defaultColor = Color.LTGRAY
            var tempStart = palette?.getVibrantColor(defaultColor) ?: defaultColor
            var dominantSwatch: Palette.Swatch? = null
            palette?.swatches?.forEach { swatch ->
                if (swatch != null) {
                    if (dominantSwatch == null || swatch.population > dominantSwatch!!.population) {
                        dominantSwatch = swatch
                    }
                }
            }
            var tempEnd = dominantSwatch?.rgb ?: palette?.getDominantColor(defaultColor) ?: defaultColor
            val brightnessThreshold = 200
            val darkeningFactor = 0.8f
            var r = Color.red(tempStart)
            var g = Color.green(tempStart)
            var b = Color.blue(tempStart)
            if ((r + g + b) / 3 > brightnessThreshold) {
                tempStart = Color.argb(
                    Color.alpha(tempStart),
                    (r * darkeningFactor).toInt(),
                    (g * darkeningFactor).toInt(),
                    (b * darkeningFactor).toInt()
                )
            }
            r = Color.red(tempEnd)
            g = Color.green(tempEnd)
            b = Color.blue(tempEnd)
            if ((r + g + b) / 3 > brightnessThreshold) {
                tempEnd = Color.argb(
                    Color.alpha(tempEnd),
                    (r * darkeningFactor).toInt(),
                    (g * darkeningFactor).toInt(),
                    (b * darkeningFactor).toInt()
                )
            }
            val start = tempStart
            var end = tempEnd
            val endAlpha = if (Color.alpha(end) > 200 && (start == end || Color.alpha(end) == 255)) 180 else Color.alpha(end)
            end = Color.argb(endAlpha, Color.red(end), Color.green(end), Color.blue(end))
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(start, end)
            )
            binding.root.background = gradient
            window?.statusBarColor = Color.TRANSPARENT
            window?.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) {
                musicService!!.mediaPlayer = MediaPlayer()
            }
            musicService!!.mediaPlayer!!.reset()

            if (songPosition < 0 || songPosition >= musicListPA.size) {
                Toast.makeText(this, "Invalid song position: $songPosition", Toast.LENGTH_SHORT).show()
                return
            }

            val songToPrepare = musicListPA[songPosition]
            preparingSongId = songToPrepare.id
            nowPlayingId = songToPrepare.id

            musicService!!.mediaPlayer!!.setDataSource(songToPrepare.path)
            musicService!!.mediaPlayer!!.setOnPreparedListener(this)
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            musicService!!.mediaPlayer!!.prepareAsync()

        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing media: ${e.message}", Toast.LENGTH_LONG).show()
            preparingSongId = null
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (musicService?.mediaPlayer != mp || musicListPA.getOrNull(songPosition)?.id != preparingSongId) {
            return
        }
        preparingSongId = null

        try {
            binding.tvSeekBarStart.text = formatDuration(mp!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(mp.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = mp.duration

            playMusic()

            loudnessEnhancer = LoudnessEnhancer(mp.audioSessionId)
            loudnessEnhancer.enabled = true

        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Error during onPrepared (IllegalState): ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: RuntimeException) {
            Toast.makeText(this, "Error during onPrepared (Runtime): ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic() {
        try {
            if (musicService?.mediaPlayer?.isPlaying == false) {
                musicService!!.mediaPlayer!!.start()
            }
            isPlaying = true
            binding.playPauseImgPA.setImageResource(R.drawable.pause_icon)
            musicService!!.showNotification(R.drawable.pause_icon)
            binding.songNamePA.isSelected = true
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Error playing music: ${e.message}", Toast.LENGTH_SHORT).show()
            isPlaying = false
            binding.playPauseImgPA.setImageResource(R.drawable.play_icon)
        }
    }

    private fun pauseMusic() {
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseImgPA.setImageResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
        binding.songNamePA.isSelected = false
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun prevNextSong(increment: Boolean) {
        if (musicListPA.isEmpty() || !::pagerState.isInitialized || !::composableScope.isInitialized) return

        val currentPage = pagerState.currentPage
        val newPage = if (increment) {
            if (currentPage == musicListPA.size - 1) 0 else currentPage + 1
        } else {
            if (currentPage == 0) musicListPA.size - 1 else currentPage - 1
        }

        if (currentPage != newPage) {
            composableScope.launch {
                pagerState.animateScrollToPage(newPage)
            }
        } else if (musicListPA.size == 1) {
            createMediaPlayer()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (musicService == null) {
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        createMediaPlayer()
        musicService!!.seekBarSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCompletion(mp: MediaPlayer?) {
        if (repeat) {
            createMediaPlayer()
        } else {
            val nextPage = if (pagerState.currentPage == musicListPA.size - 1) 0 else pagerState.currentPage + 1
            composableScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 5 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            min5 = true
            Thread {
                Thread.sleep((5 * 60000).toLong())
                if (min5) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 10 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            min10 = true
            Thread {
                Thread.sleep((10 * 60000).toLong())
                if (min10) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }

    private fun getMusicDetails(contentUri: Uri): Music {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            return Music(
                id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
                artUri = "Unknown", path = path.toString()
            )
        } finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }

    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false) {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if (shuffle) musicListPA.shuffle()
        setLayout()
        if (!playNext) PlayNext.playNextList = ArrayList()
    }
}
