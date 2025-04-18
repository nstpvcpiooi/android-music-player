package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.NowPlaying
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
import com.example.musicplayer.model.toFile
import com.example.musicplayer.onprg.PlaylistActivity
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.favouriteChecker
import com.example.musicplayer.utils.formatDuration
import com.example.musicplayer.utils.getImgArt
import com.example.musicplayer.utils.getMainColor
import com.example.musicplayer.utils.setDialogBtnBackground
import com.example.musicplayer.utils.setSongPosition
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //chon trung theme ma main actitivy dang dung
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])

        //inflate
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //để nghe file nhạc từ trong file điện thoại
        if(intent.data?.scheme.contentEquals("content")){
            songPosition = 0

            //connect to music service
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)

            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgPA)
            binding.songNamePA.text = musicListPA[songPosition].title
        } else initializeLayout()


        //karaoke
        voiceFile = File(cacheDir, "recorded_voice.m4a")
        mixedFile = File(cacheDir, "mixed_output.mp3")


        //tăng âm lượng custom
        binding.boosterBtnPA.setOnClickListener {
            val customDialogB = LayoutInflater.from(this).inflate(R.layout.audio_booster, binding.root, false)
            val bindingB = AudioBoosterBinding.bind(customDialogB)
            val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                .setOnCancelListener { playMusic() }
                .setPositiveButton("OK"){self, _ ->
                    loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                    playMusic()
                    self.dismiss()
                }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialogB.show()

            bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt()/100
            bindingB.progressText.text = "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt()/10} %"
            bindingB.verticalBar.setOnProgressChangeListener {
                bindingB.progressText.text = "Audio Boost\n\n${it*10} %"
            }
            setDialogBtnBackground(this, dialogB)
        }

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
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }


        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
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
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,
                            R.color.cool_pink
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
        binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))





        }

        binding.recordingBtnPA.setOnClickListener {

            if (isRecording == false) {
                audioRecorder = AudioRecorder(voiceFile)
                audioRecorder.startRecording()
                Toast.makeText(this, "Đang ghi âm...", Toast.LENGTH_SHORT).show()
                isRecording = true
            } else {
                audioRecorder.stopRecording()

                try {


                    val uri = AudioSaver.saveToRecordings(this, voiceFile, "my_voice_${System.currentTimeMillis()}.m4a")
                } catch (e:Exception) {
                    Toast.makeText(this, "loi", Toast.LENGTH_LONG).show()
                }
                //Toast.makeText(this, "Đã lưu file tại: $uri", Toast.LENGTH_LONG).show()


                // Tạo file kết quả lưu vào bộ nhớ ngoài
                var outputFile = File(getExternalFilesDir(null), "mixed_audio_${System.currentTimeMillis()}.mp3")

                var musicFile = musicListPA[songPosition].toFile();
                // Gọi hàm mix audio
                AudioMixer.mixAudio(this, musicFile, voiceFile, outputFile) { success ->
                    if (success) {
                        // Nếu mix thành công, lưu vào thư mục Recordings
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
            }

        }

        binding.stoprecordingBtnPA.setOnClickListener {

//            audioRecorder.stopRecording()
//
//            try {
//
//
//                val uri = AudioSaver.saveToRecordings(this, voiceFile, "my_voice_${System.currentTimeMillis()}.m4a")
//            } catch (e:Exception) {
//                Toast.makeText(this, "loi", Toast.LENGTH_LONG).show()
//            }
//            //Toast.makeText(this, "Đã lưu file tại: $uri", Toast.LENGTH_LONG).show()
//
//
//            // Tạo file kết quả lưu vào bộ nhớ ngoài
//            var outputFile = File(getExternalFilesDir(null), "mixed_audio_${System.currentTimeMillis()}.mp3")
//
//            var musicFile = musicListPA[songPosition].toFile();
//            // Gọi hàm mix audio
//            AudioMixer.mixAudio(this, musicFile, voiceFile, outputFile) { success ->
//                if (success) {
//                    // Nếu mix thành công, lưu vào thư mục Recordings
//                    val savedUri = AudioSaver.saveToRecordings(this, outputFile)
//                    if (savedUri != null) {
//                        // Thông báo hoặc mở file vừa lưu
//                        Toast.makeText(this, "File đã được lưu thành công!", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this, "Lưu file thất bại!", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Mix thất bại!", Toast.LENGTH_SHORT).show()
//                }
//            }
            Toast.makeText(this, "Disabled!", Toast.LENGTH_SHORT).show()

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
    }

    //handles intents aka data comes from other resources
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index", 0)
        when(intent.getStringExtra("class")){
            "NowPlaying"->{
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"-> initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            "MainActivity"-> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = true)
            "FavouriteShuffle"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
            "PlaylistDetailsAdapter"->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = false)
            "PlaylistDetailsShuffle"->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = true)
            "PlayNext"->initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)
        }
    if (musicService != null && !isPlaying) playMusic()
    }

    private fun setLayout(){
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        if(repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.purple_500
        ))
        if(min5 || min10 || min30) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.purple_500
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
        val bgColor = getMainColor(image)
        val gradient = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xFFFFFF, bgColor))
        binding.root.background = gradient
        window?.statusBarColor = bgColor
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
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
    }

    private fun pauseMusic(){
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)


    }
    private fun prevNextSong(increment: Boolean){
        if(increment)
        {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        }
        else{
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }



    //sau khi kết nối tới musicservice thành công sẽ gọi hàm này
    //
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
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        //for refreshing now playing image & text on song completion
        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13 || resultCode == RESULT_OK)
            return
    }


    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 5 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min5 = true
            Thread{Thread.sleep((5 * 60000).toLong())
            if(min5) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 10 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min10 = true
            Thread{Thread.sleep((10 * 60000).toLong())
                if(min10) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if(min30) exitApplication()
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
            return Music(id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
            artUri = "Unknown", path = path.toString())
        }finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }
    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false){
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if(shuffle) musicListPA.shuffle()
        setLayout()
        if(!playNext) PlayNext.playNextList = ArrayList()
    }


}