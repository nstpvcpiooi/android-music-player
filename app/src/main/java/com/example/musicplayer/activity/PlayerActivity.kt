package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.Bitmap // Added import for Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable // Added import for KTX extension
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.NowPlaying
import com.example.musicplayer.utils.PlayNext
import com.example.musicplayer.R
import com.example.musicplayer.adapter.AlbumCoverPagerAdapter
import com.example.musicplayer.audio.AudioMixer
import com.example.musicplayer.audio.AudioRecorder
import com.example.musicplayer.audio.AudioSaver
import com.example.musicplayer.transformer.AlbumCoverPageTransformer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.example.musicplayer.databinding.TimerBottomSheetBinding
import com.example.musicplayer.databinding.BottomSheetQueueBinding
import com.example.musicplayer.fragment.PlayerMoreFeaturesBottomSheet
import com.example.musicplayer.model.toFile
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.adapter.QueueAdapter
import com.example.musicplayer.adapter.RecommendedSongsAdapter
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.favouriteChecker
import com.example.musicplayer.utils.formatDuration
import com.example.musicplayer.utils.getImgArt
import com.example.musicplayer.utils.setDialogBtnBackground
import com.example.musicplayer.utils.setSongPosition
import jp.wasabeef.glide.transformations.BlurTransformation
import java.io.File

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicListPA : ArrayList<Music>

        var songPosition: Int = 0
        var isPlaying:Boolean = false

        var musicService: MusicService? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding

        var repeat: Boolean = false
        var autoPlay: Boolean = false // Added for auto-play feature

        var min5: Boolean = false
        var min10: Boolean = false
        var min30: Boolean = false

        var nowPlayingId: String = ""
        var currentPlaylistOrigin: String? = null // Added to track playlist source
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        lateinit var loudnessEnhancer: LoudnessEnhancer

        lateinit var audioRecorder: AudioRecorder
        lateinit var voiceFile: File
        lateinit var mixedFile: File
        var isRecording: Boolean = false

        // Add the album cover pager adapter
        lateinit var albumCoverAdapter: AlbumCoverPagerAdapter

        // Initialize settings from shared preferences when app starts
        fun loadSettings(context: Context) {
            try {
                val appSettingPrefs = context.getSharedPreferences("APP_SETTINGS_PREFS", Context.MODE_PRIVATE)
                autoPlay = appSettingPrefs.getBoolean("AutoPlay", false)
            } catch (e: Exception) {
                // Fallback in case of any issues
                autoPlay = false
            }
        }
    }

    private var userIsSwiping = false

    // ViewPager page change callback to update song position when swiping
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (songPosition != position && !userIsSwiping) {
                songPosition = position
                createMediaPlayer()
                setLayout()
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            when (state) {
                ViewPager2.SCROLL_STATE_DRAGGING -> userIsSwiping = true
                ViewPager2.SCROLL_STATE_IDLE -> {
                    // Only change song if user actually swiped to a different position
                    if (userIsSwiping && binding.albumCoverViewPager.currentItem != songPosition) {
                        songPosition = binding.albumCoverViewPager.currentItem
                        createMediaPlayer()
                        setLayout()
                    }
                    userIsSwiping = false
                }
                ViewPager2.SCROLL_STATE_SETTLING -> { /* Handle settling state if needed */ }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Allow drawing behind system bars

        //chon trung theme ma main actitivy dang dung
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])

        //inflate
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize album cover adapter
        albumCoverAdapter = AlbumCoverPagerAdapter(ArrayList())
        binding.albumCoverViewPager.adapter = albumCoverAdapter
        binding.albumCoverViewPager.setPageTransformer(AlbumCoverPageTransformer())
        binding.albumCoverViewPager.registerOnPageChangeCallback(pageChangeCallback)

        // Reduce page transition sensitivity to avoid accidental swipes
        binding.albumCoverViewPager.apply {
            // Offscreen page limit for smoother transitions
            offscreenPageLimit = 1
        }

        //để nghe file nhạc từ trong file điện thoại
        if(intent.data?.scheme.contentEquals("content")){
            PlayerActivity.currentPlaylistOrigin = "OpenFile" // Set origin for file opening
            songPosition = 0

            //connect to music service
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)

            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))

            // Set up album cover pager
            albumCoverAdapter.updateMusicList(musicListPA)
            binding.albumCoverViewPager.setCurrentItem(songPosition, false)

            // For backward compatibility
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgPA)

            binding.songNamePA.text = musicListPA[songPosition].title
        } else initializeLayout()


        //karaoke
        voiceFile = File(cacheDir, "recorded_voice.m4a")
        mixedFile = File(cacheDir, "mixed_output.mp3")

        //utils buttons
        binding.backBtnPA.setOnClickListener { finish() }
        binding.playPauseBtnPA.setOnClickListener{ if(isPlaying) pauseMusic() else playMusic() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if(isPlaying) R.drawable.pause_icon else R.drawable.play_icon)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.repeatBtnPA.setOnClickListener {
            if(!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
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
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,
                            R.color.white
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

        binding.recordingBtnPA.setOnClickListener {

            if (!isRecording) { // Simplified condition
                audioRecorder = AudioRecorder(voiceFile)
                audioRecorder.startRecording()
                Toast.makeText(this, "��ang ghi âm...", Toast.LENGTH_SHORT).show()
                isRecording = true
            } else {
                audioRecorder.stopRecording()

                try {
                    AudioSaver.saveToRecordings(this, voiceFile, "my_voice_${System.currentTimeMillis()}.m4a") // Removed unused val uri
                } catch (_:Exception) { // Changed e to _
                    Toast.makeText(this, "loi", Toast.LENGTH_LONG).show()
                }

                // Tạo file kết quả lưu vào bộ nhớ ngoài
                val outputFile = File(getExternalFilesDir(null), "mixed_audio_${System.currentTimeMillis()}.mp3") // val instead of var

                val musicFile = musicListPA[songPosition].toFile() // Removed redundant semicolon
                // Gọi hàm mix audio
                AudioMixer.mixAudio(this, musicFile, voiceFile, outputFile) { success ->
                    if (success) {
                        // N���u mix thành công, lưu vào thư mục Recordings
                        val savedUri = AudioSaver.saveToRecordings(this, outputFile)
                        if (savedUri != null) {
                            // Thông báo hoặc mở file vừa lưu
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

        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if(isFavourite){
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else{
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

        binding.queueBtnPA.setOnClickListener {
            showQueueBottomSheet()
        }

        // Initialize auto-play setting from preferences
        val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
        autoPlay = appSettingPrefs.getBoolean("AutoPlay", false)
    }

    //handles intents aka data comes from other resources
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index", 0)
        val cls = intent.getStringExtra("class")
        PlayerActivity.currentPlaylistOrigin = cls // Set current playlist origin

        when(cls){
            "NowPlaying"->{

                // Sync PlayerActivity.isPlaying with the actual state from MusicService
                if (musicService?.mediaPlayer != null) {
                    isPlaying = musicService!!.mediaPlayer!!.isPlaying // Removed redundant qualifier
                }
                // else: isPlaying retains its value. If service/mediaPlayer isn't ready,
                // it relies on the existing state, which should be accurate from previous interactions.

                setLayout()
                // Ensure musicService and mediaPlayer are available before accessing them
                if (musicService?.mediaPlayer != null) {
                    binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                    binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                    binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                    binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                    // Call seekBarSetup if service is available to start progress updates
                    musicService?.seekBarSetup()
                }
                // Update play/pause button based on the (potentially updated) isPlaying state
                if (isPlaying) binding.playPauseImgPA.setImageResource(R.drawable.pause_icon) // Removed redundant qualifier
                else binding.playPauseImgPA.setImageResource(R.drawable.play_icon)

                // Đảm bảo albumCoverAdapter được khởi tạo với danh sách nhạc hiện tại
                albumCoverAdapter.updateMusicList(musicListPA)
                // Đặt ViewPager hiển thị bài hát hiện tại
                binding.albumCoverViewPager.setCurrentItem(songPosition, false)
            }
            "MusicAdapterSearch"-> initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            // "FavouriteAdapter"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false) // Commented out or remove old/unused case
            "FavouritePlayAll" -> { // Added case for playing all favourite songs
                // songPosition is already set from intent's "index" (should be 0)
                initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            }
            "FavouriteSongClick" -> { // Added case for playing a specific favourite song
                // songPosition is already set from intent's "index"
                initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            }
            "MainActivity"-> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = true)
            "MainActivityLimited"-> {
                // Create a copy of music list with maximum 30 songs
                val limitedList = if (MainActivity.MusicListMA.size <= 20) {
                    ArrayList(MainActivity.MusicListMA)
                } else {
                    ArrayList(MainActivity.MusicListMA.take(20))
                }
                initServiceAndPlaylist(limitedList, shuffle = true)

                // Make sure auto-play is enabled
                autoPlay = true
            }
            "FavouriteShuffle"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
            "PlaylistDetailsAdapter"->
                initServiceAndPlaylist(PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist, shuffle = false)
            "PlaylistDetailsShuffle"->
                initServiceAndPlaylist(PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist, shuffle = true)
            "PlayNext"->initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)


            // THÊM TRƯỜNG HỢP MỚI NÀY VÀO
            "OnlineSong" -> {
                // Danh sách nhạc (musicListPA) đã được gán từ DownloadActivity
                // Chúng ta chỉ cần khởi tạo service và giao diện
                initServiceAndPlaylist(musicListPA, shuffle = false)
            }
        }

        // Set viewpager to current song position
        binding.albumCoverViewPager.setCurrentItem(songPosition, false)
    }

    private fun setLayout(){
        fIndex = favouriteChecker(musicListPA[songPosition].id)

        // Update ViewPager to show current song position
        if (binding.albumCoverViewPager.currentItem != songPosition) {
            binding.albumCoverViewPager.setCurrentItem(songPosition, true)
        }

        // Keep the old image view updated for backward compatibility
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(binding.songImgPA)

        binding.songNamePA.text = musicListPA[songPosition].title
        if(repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.yellow
        ))
        if(min5 || min10 || min30) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.yellow
        ))
        if(isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
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

        // Define smaller dimensions for the bitmap that will be blurred.
        // Processing a smaller bitmap is much faster.
        // Adjust these values based on the visual requirements and performance.
        val blurTargetWidth = 240 // Example: smaller width in pixels
        val blurTargetHeight = 360 // Example: smaller height in pixels

        // New background logic using Glide for blurring and LayerDrawable
        Glide.with(applicationContext)
            .asBitmap()
            .load(image) // Load the original album art bitmap
            .apply(
                RequestOptions()
                    // Downsample the image to these dimensions BEFORE transformations.
                    // This is a key optimization for speed.
                    .override(blurTargetWidth, blurTargetHeight)
                    // Apply CenterCrop to the downsampled image, then blur.
                    // Reduced blur radius and sampling for speed.
                    // Original was (70, 10), previous suggestion (25,4), now further reduced for speed.
                    .transform(CenterCrop(), BlurTransformation(70, 2))
            )
            // The CustomTarget will receive a bitmap of blurTargetWidth x blurTargetHeight.
            .into(object : CustomTarget<Bitmap>(blurTargetWidth, blurTargetHeight) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val blurredBitmapDrawable = BitmapDrawable(resources, resource)

                    // Gradient from transparent at the top to solid black at the bottom
                    val gradientDrawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.TRANSPARENT, Color.BLACK)
                    )

                    // LayerDrawable: Blurred image as the base, gradient overlay on top
                    val layers = arrayOf<Drawable>(blurredBitmapDrawable, gradientDrawable)
                    val layerDrawable = LayerDrawable(layers)

                    binding.root.background = layerDrawable

                    // Ensure status bar and navigation bar remain transparent
                    window?.statusBarColor = Color.TRANSPARENT
                    window?.navigationBarColor = Color.TRANSPARENT
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder state, e.g., set a solid background
                    binding.root.background = Color.BLACK.toDrawable() // KTX extension // Fallback
                    window?.statusBarColor = Color.TRANSPARENT
                    window?.navigationBarColor = Color.TRANSPARENT
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    // Optional: Handle error state, e.g., set a default solid or gradient background
                    binding.root.background = Color.BLACK.toDrawable() // KTX extension // Fallback
                    window?.statusBarColor = Color.TRANSPARENT
                    window?.navigationBarColor = Color.TRANSPARENT
                }
            })
    }

    private fun createMediaPlayer(){
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
            playMusic()
            loudnessEnhancer = LoudnessEnhancer(musicService!!.mediaPlayer!!.audioSessionId)
            loudnessEnhancer.enabled = true
        }catch (e: Exception){Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()}
    }

    private fun playMusic(){
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
        binding.playPauseImgPA.setImageResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
        binding.songNamePA.isSelected = true // Start marquee when playing
    }

    private fun pauseMusic(){
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseImgPA.setImageResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
        binding.songNamePA.isSelected = false // Stop marquee when paused
    }

    private fun prevNextSong(increment: Boolean){
        if(increment) {
            // Handle next song behavior
            if (songPosition == musicListPA.size - 1) {
                // At the end of the playlist
                if (autoPlay) {
                    // With autoplay: Add new song and play it
                    val newSongAdded = addRandomSongToQueue()
                    if (newSongAdded) {
                        setSongPosition(increment = true)
                        setLayout()
                        createMediaPlayer()
                    } else {
                        // If no new song could be added, go to first song and stop
                        songPosition = 0
                        setLayout()
                        createMediaPlayer()
                        pauseMusic()
                    }
                } else {
                    // Without autoplay: Go to the first song and stop
                    songPosition = 0
                    setLayout()
                    createMediaPlayer()
                    pauseMusic()
                }
            } else {
                // Not at the end, normal behavior
                setSongPosition(increment = true)
                setLayout()
                createMediaPlayer()
            }
        } else {
            // Previous song - no changes needed
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if(musicService == null){
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

    override fun onCompletion(mp: MediaPlayer?) {
        // First determine if we need to add a new song for auto-play
        if (autoPlay && songPosition >= musicListPA.size - 1) {
            // We're at the last song and auto-play is enabled
            // Add a random song from the main music library to the queue
            addRandomSongToQueue()
        }

        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        // For refreshing now playing image & text on song completion
        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }

    // Function to add the next song to the queue when auto-play is enabled
    private fun addRandomSongToQueue(): Boolean {
        if (MainActivity.MusicListMA.isEmpty()) {
            Toast.makeText(this, "No music available for auto-play", Toast.LENGTH_SHORT).show()
            return false
        }

        val allSongs = ArrayList(MainActivity.MusicListMA)

        // Filter out songs that are already in the queue
        val currentSongIds = musicListPA.map { it.id }
        val filteredSongs = allSongs.filter { !currentSongIds.contains(it.id) }

        // Sort songs by title to ensure a consistent order
        val sortedSongs = filteredSongs.sortedBy { it.title }

        // Get the first song from the sorted list
        val newSong = if (sortedSongs.isNotEmpty()) {
            sortedSongs[0]
        } else if (allSongs.isNotEmpty()) {
            // If all songs are already in queue, just pick the first alphabetically sorted one
            allSongs.sortedBy { it.title }[0]
        } else {
            null
        }

        // Add the selected song to the queue
        if (newSong != null) {
            musicListPA.add(newSong)
            PlayNext.playNextList.add(newSong)

            // Update the album cover adapter with the new song
            albumCoverAdapter.updateMusicList(musicListPA)

            // Toast.makeText(this, "Auto-Play: Added ${newSong.title}", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        val bottomSheetBinding = TimerBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(bottomSheetBinding.root)
        dialog.show()

        bottomSheetBinding.timerOptionsNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.timer_5_min -> {
                    Toast.makeText(baseContext, "Music will stop after 5 minutes", Toast.LENGTH_SHORT).show()
                    binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
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
                    binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
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
                    binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
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
            return Music(id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
                artUri = "Unknown", path = path.toString())
        }finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.albumCoverViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        if(musicListPA.isNotEmpty() && songPosition < musicListPA.size && musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }

    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false){
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if(shuffle) musicListPA.shuffle()

        // Update album cover adapter with new playlist
        albumCoverAdapter.updateMusicList(musicListPA)
        binding.albumCoverViewPager.setCurrentItem(songPosition, false)

        setLayout()
        if(!playNext) PlayNext.playNextList = ArrayList()
    }

    // Function to get recommended songs for auto-play that are similar to current song
    private fun getRecommendedSongsForAutoPlay(count: Int): ArrayList<Music> {
        val recommendedList = ArrayList<Music>()

        // Source for recommendations is the main music library
        if (MainActivity.MusicListMA.isEmpty()) {
            return recommendedList
        }

        val allSongs = ArrayList(MainActivity.MusicListMA)

        // Get current song info to make better recommendations
        val currentSong = if (songPosition >= 0 && songPosition < musicListPA.size) {
            musicListPA[songPosition]
        } else null

        // Filter out songs that are already in the queue
        val currentSongIds = musicListPA.map { it.id }
        val filteredSongs = allSongs.filter { !currentSongIds.contains(it.id) }

        // Sort algorithm for recommendations
        val sortedSongs = if (currentSong != null) {
            // Prioritize songs from the same album or artist as the current song
            filteredSongs.sortedWith(compareBy(
                // First by same album (highest priority)
                { if (it.album != currentSong.album) 1 else 0 },
                // Then by same artist
                { if (it.artist != currentSong.artist) 1 else 0 },
                // Then alphabetically by title
                { it.title }
            ))
        } else {
            // Default sort if no current song information
            filteredSongs.sortedBy { it.title }
        }

        // Add up to 'count' songs to the recommendations
        val songsToAdd = sortedSongs.take(count)
        recommendedList.addAll(songsToAdd)

        return recommendedList
    }

    // Function to refresh recommendations when needed
    private fun refreshRecommendations() {
        // This will be called when user changes songs or queue is modified
        // The actual implementation is in showQueueBottomSheet where recommendations are loaded
    }

    private fun showQueueBottomSheet() {
        val dialog = BottomSheetDialog(this@PlayerActivity)
        val bottomSheetBinding = BottomSheetQueueBinding.inflate(layoutInflater)
        dialog.setContentView(bottomSheetBinding.root)

        // Apply the same background effect to the queue bottom sheet
        applyBackgroundToQueue(dialog, bottomSheetBinding)

        // Get fresh recommended songs for autoplay based on current song
        val recommendedSongs = getRecommendedSongsForAutoPlay(5)

        // Set up the auto-play switch with the current setting
        bottomSheetBinding.autoPlaySwitch.isChecked = autoPlay
        bottomSheetBinding.autoPlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            autoPlay = isChecked

            // Save the auto-play setting to shared preferences
            val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
            val editor = appSettingPrefs.edit()
            editor.putBoolean("AutoPlay", isChecked)
            editor.apply()

            // Update recommended songs section visibility
            bottomSheetBinding.recommendedSongsContainer.visibility =
                if (isChecked) View.VISIBLE else View.GONE

            Toast.makeText(
                this,
                if (isChecked) "Auto-Play enabled" else "Auto-Play disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Setup recommended songs section
        if (recommendedSongs.isNotEmpty()) {
            // Initialize the recommended songs RecyclerView
            bottomSheetBinding.recommendedSongsRV.layoutManager = LinearLayoutManager(this)
            val recommendedAdapter = RecommendedSongsAdapter(this, recommendedSongs)

            // Set click listener for adding songs to queue
            recommendedAdapter.setOnRecommendedItemClickListener(object : RecommendedSongsAdapter.OnRecommendedItemClickListener {
                override fun onAddToQueueClick(position: Int, song: Music) {
                    // Add song to current playlist and PlayNext list
                    musicListPA.add(song)
                    PlayNext.playNextList.add(song)

                    // Update album cover adapter
                    albumCoverAdapter.updateMusicList(musicListPA)

                    // Update queue adapter if it exists
                    val queueAdapter = bottomSheetBinding.queueRV.adapter as? QueueAdapter
                    queueAdapter?.updateQueue(ArrayList(musicListPA))

                    Toast.makeText(
                        this@PlayerActivity,
                        "Added ${song.title} to queue",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Update the recommended songs list (remove the added song)
                    recommendedSongs.removeAt(position)
                    recommendedAdapter.updateList(recommendedSongs)

                    // If no more recommendations, hide the section
                    if (recommendedSongs.isEmpty()) {
                        bottomSheetBinding.recommendedSongsContainer.visibility = View.GONE
                    }
                }
            })

            bottomSheetBinding.recommendedSongsRV.adapter = recommendedAdapter

            // Show or hide recommended songs section based on autoplay setting
            bottomSheetBinding.recommendedSongsContainer.visibility =
                if (autoPlay) View.VISIBLE else View.GONE
        } else {
            // No recommendations available
            bottomSheetBinding.recommendedSongsContainer.visibility = View.GONE
        }

        // Use the current playing list as the queue, instead of only PlayNext.playNextList
        val queueMusicList = if (PlayNext.playNextList.isEmpty()) {
            musicListPA
        } else {
            PlayNext.playNextList
        }

        // Synchronize PlayNext.playNextList with current playlist if it's empty
        if (PlayNext.playNextList.isEmpty() && musicListPA.isNotEmpty()) {
            PlayNext.playNextList.clear()
            PlayNext.playNextList.addAll(musicListPA)
        }

        if (queueMusicList.isEmpty()) {
            bottomSheetBinding.queueRV.visibility = View.GONE
            bottomSheetBinding.emptyQueueText.visibility = View.VISIBLE
        } else {
            bottomSheetBinding.queueRV.visibility = View.VISIBLE
            bottomSheetBinding.emptyQueueText.visibility = View.GONE

            // Set up the RecyclerView
            bottomSheetBinding.queueRV.setHasFixedSize(true)
            bottomSheetBinding.queueRV.layoutManager = LinearLayoutManager(this)

            // Create the ItemTouchHelper callback for drag & drop
            val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Enable vertical drag direction
                0 // No swipe directions
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition

                    // Get adapter and call its onItemMove method
                    val adapter = recyclerView.adapter as? QueueAdapter
                    return adapter?.onItemMove(fromPosition, toPosition) ?: false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // Not implemented as we're not enabling swipe
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        // Give visual feedback by changing alpha
                        viewHolder?.itemView?.alpha = 0.7f
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    // Reset alpha when drag ends
                    viewHolder.itemView.alpha = 1.0f
                }

                // Enable drag handle functionality
                override fun isLongPressDragEnabled(): Boolean {
                    // Disable long press drag so only the drag handle can be used
                    return false
                }
            }

            // Create the ItemTouchHelper and attach it to the RecyclerView
            val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
            itemTouchHelper.attachToRecyclerView(bottomSheetBinding.queueRV)

            // Create the adapter with the ItemTouchHelper
            val queueAdapter = QueueAdapter(this, ArrayList(queueMusicList), itemTouchHelper)

            // Set the current position for highlighting
            queueAdapter.setCurrentPosition(songPosition)

            // Set the queue update listener with enhanced position tracking
            queueAdapter.setQueueUpdateListener(object : QueueAdapter.QueueUpdateListener {
                override fun onQueueUpdated(fromPosition: Int, toPosition: Int, isRemoval: Boolean, removedPosition: Int) {
                    // Handle empty queue case
                    if (PlayNext.playNextList.isEmpty()) {
                        bottomSheetBinding.queueRV.visibility = View.GONE
                        bottomSheetBinding.emptyQueueText.visibility = View.VISIBLE
                        return
                    }

                    // Synchronize with PlayerActivity state
                    synchronized(musicListPA) {
                        // Update the player's music list to match the updated queue
                        val newList = ArrayList(PlayNext.playNextList)

                        // Update PlayerActivity's position tracking based on the queue adapter's current position
                        val adapterCurrentPos = queueAdapter.getCurrentPosition()
                        if (adapterCurrentPos >= 0 && adapterCurrentPos < newList.size) {
                            songPosition = adapterCurrentPos
                        }

                        // Important: First update the musicListPA with the new order
                        musicListPA = newList

                        // Then update the ViewPager adapter with the same list
                        albumCoverAdapter.updateMusicList(newList)

                        // Important: Set the ViewPager to the correct position without animation
                        // to avoid visual glitches
                        binding.albumCoverViewPager.setCurrentItem(songPosition, false)

                        // Refresh recommendations if a song was removed
                        if (isRemoval) {
                            // Update recommendations to show new options
                            val updatedRecommendations = getRecommendedSongsForAutoPlay(5)
                            val recommendedAdapter = bottomSheetBinding.recommendedSongsRV.adapter as? RecommendedSongsAdapter
                            recommendedAdapter?.updateList(updatedRecommendations)

                            // Show or hide the container based on recommendations availability
                            bottomSheetBinding.recommendedSongsContainer.visibility =
                                if (updatedRecommendations.isNotEmpty() && autoPlay) View.VISIBLE else View.GONE
                        }
                    }
                }
            })

            // Set the adapter to the RecyclerView
            bottomSheetBinding.queueRV.adapter = queueAdapter

            // Scroll to current song position
            if (songPosition >= 0 && songPosition < queueMusicList.size) {
                bottomSheetBinding.queueRV.scrollToPosition(songPosition)
            }

            // Make the instruction text visible
            if (bottomSheetBinding.queueInstructionText != null) {
                bottomSheetBinding.queueInstructionText.visibility = View.VISIBLE
            }
        }

        dialog.show()
    }

    // Function to apply the same background effect to the queue dialog as the player
    private fun applyBackgroundToQueue(dialog: BottomSheetDialog, binding: BottomSheetQueueBinding) {
        // Get the background of the player activity
        val currentBackground = binding.root.background

        // Make the dialog window background transparent
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // Make navigation bar transparent with light icons
            navigationBarColor = Color.TRANSPARENT

            // Set system UI flags if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        // Apply the same background effect to the queue
        val img = getImgArt(musicListPA[songPosition].path)
        val image = if (img != null) {
            BitmapFactory.decodeByteArray(img, 0, img.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_slash_screen)
        }

        // Define smaller dimensions for the bitmap that will be blurred
        val blurTargetWidth = 240
        val blurTargetHeight = 360

        // Get the bottom sheet container to apply background
        val bottomSheetInternal = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        // Use Glide to apply the blurred background
        Glide.with(applicationContext)
            .asBitmap()
            .load(image)
            .apply(
                RequestOptions()
                    .override(blurTargetWidth, blurTargetHeight)
                    .transform(CenterCrop(), BlurTransformation(70, 2))
            )
            .into(object : CustomTarget<Bitmap>(blurTargetWidth, blurTargetHeight) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val blurredBitmapDrawable = BitmapDrawable(resources, resource)

                    // Create gradient overlay
                    val gradientDrawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.TRANSPARENT, Color.BLACK)
                    )

                    // LayerDrawable: Blurred image as base, gradient on top
                    val layers = arrayOf<Drawable>(blurredBitmapDrawable, gradientDrawable)
                    val layerDrawable = LayerDrawable(layers)

                    // Apply the background to the bottom sheet
                    bottomSheetInternal?.background = layerDrawable
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    bottomSheetInternal?.background = Color.BLACK.toDrawable()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    bottomSheetInternal?.background = Color.BLACK.toDrawable()
                }
            })
    }
}
