package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.musicplayer.model.Music
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.R
import com.example.musicplayer.NowPlaying
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.fragment.AccountFragment
import com.example.musicplayer.fragment.LibraryFragment
import com.example.musicplayer.fragment.PlaylistFragment
import com.example.musicplayer.fragment.SearchFragment
import com.google.gson.GsonBuilder
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.model.MusicPlaylist
import com.example.musicplayer.utils.PlayNext
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.utils.exitApplication
import com.google.gson.reflect.TypeToken
import java.io.File

class MainActivity : AppCompatActivity(), MusicAdapter.OnMusicItemClickListener, ServiceConnection {
    private lateinit var binding: ActivityMainBinding
    lateinit var musicAdapter: MusicAdapter

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.ACTION_PLAYBACK_STATE_CHANGED) {
                val nowPlayingFragment = supportFragmentManager.findFragmentById(R.id.nowPlaying) as? NowPlaying
                nowPlayingFragment?.refreshUIContent()
            }
        }
    }

    companion object {
        var MusicListMA: ArrayList<Music> = ArrayList() // Initialize to empty list
        var musicListSearch: ArrayList<Music> = ArrayList() // Initialize to empty list
        var search: Boolean = false
        var themeIndex: Int = 0
        val currentTheme = arrayOf(
            R.style.coolPink,
            R.style.coolBlue,
            R.style.coolPurple,
            R.style.coolGreen,
            R.style.coolBlack
        )
        val currentThemeNav = arrayOf(
            R.style.coolPinkNav, R.style.coolBlueNav, R.style.coolPurpleNav, R.style.coolGreenNav,
            R.style.coolBlackNav
        )
        val currentGradient = arrayOf(
            R.drawable.gradient_pink,
            R.drawable.gradient_blue,
            R.drawable.gradient_purple,
            R.drawable.gradient_green,
            R.drawable.gradient_black
        )
        var sortOrder: Int = 0
        val sortingList = arrayOf(
            MediaStore.Audio.Media.DATE_ADDED + " DESC", MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE + " DESC"
        )

        fun addSongToMusicList(song: Music) {
            // Kiểm tra để tránh thêm trùng lặp
            if (!MusicListMA.any { it.path == song.path }) {
                MusicListMA.add(0, song) // Thêm vào đầu danh sách
            }
        }
        var musicListHasBeenLoaded = false // <-- GIỮ LẠI BIẾN NÀY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved theme mode
        val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
        val nightMode = appSettingPrefs.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        // Load player settings early
        PlayerActivity.loadSettings(applicationContext)

        val themeEditor = getSharedPreferences("THEMES", MODE_PRIVATE)
        themeIndex = themeEditor.getInt("themeIndex", 0)
        setTheme(currentThemeNav[themeIndex])
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation
        setupBottomNavigation()

        // Load default fragment immediately
        if (savedInstanceState == null) {
            loadFragment(LibraryFragment())
        }

        //checking for dark theme
        if (themeIndex == 4 && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)
            Toast.makeText(this, "Black Theme Works Best in Dark Mode!!", Toast.LENGTH_LONG).show()

        if (requestRuntimePermission()) {
            // Chỉ gọi initializeLayout nếu đã có quyền từ trước
            initializeLayout()
            loadFavouritesAndPlaylists() // Tách logic này ra cho sạch
        }
        // Initially hide NowPlaying fragment container
        binding.nowPlaying.visibility = View.GONE

        LocalBroadcastManager.getInstance(this).registerReceiver(playbackStateReceiver, IntentFilter(MusicService.ACTION_PLAYBACK_STATE_CHANGED))
    }

    private fun loadFavouritesAndPlaylists() {
        FavouriteActivity.favouriteSongs = ArrayList()
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
        val jsonString = editor.getString("FavouriteSongs", null)
        val typeToken = object : TypeToken<ArrayList<Music>>() {}.type
        if (jsonString != null) {
            val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
            FavouriteActivity.favouriteSongs.addAll(data)
        }
        PlaylistManager.musicPlaylist = MusicPlaylist()
        val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
        if (jsonStringPlaylist != null) {
            val dataPlaylist: MusicPlaylist =
                GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
            PlaylistManager.musicPlaylist = dataPlaylist
        }
    }

    private fun setupBottomNavigation() {
        // Set up bottom navigation view item selection listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_library -> {
                    loadFragment(LibraryFragment(), addToBackStack = false) // Don't add to backstack for primary tabs
                    true
                }
                R.id.navigation_playlist -> { // Added Playlist case
                    loadFragment(PlaylistFragment(), addToBackStack = false)
                    true
                }
                R.id.navigation_account -> {
                    loadFragment(AccountFragment(), addToBackStack = false) // Don't add to backstack for primary tabs
                    true
                }
                else -> false
            }
        }

        // Set Library tab as default selected tab on app start
        binding.bottomNavigation.selectedItemId = R.id.navigation_library
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) { // Added addToBackStack parameter
        val transaction = supportFragmentManager.beginTransaction()
        // Add custom animations for fragment transitions
        transaction.setCustomAnimations(
            R.anim.fade_in_fast, // enter
            R.anim.fade_out_fast, // exit
            R.anim.fade_in_fast, // popEnter
            R.anim.fade_out_fast // popExit
        )
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) { // Conditionally add to back stack
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    // Function to open SearchFragment
    fun openSearchFragment() {
        loadFragment(SearchFragment())
    }

    // These functions will be used by the LibraryFragment
    fun openShufflePlayer() {
        // Enable auto-play when using shuffle
        val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
        val editor = appSettingPrefs.edit()
        editor.putBoolean("AutoPlay", true)
        editor.apply()

        // Start PlayerActivity with special flag for 30-song shuffle
        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
        intent.putExtra("index", 0)
        intent.putExtra("class", "MainActivityLimited")
        startActivity(intent)
    }

    fun openFavorites() {
        startActivity(Intent(this@MainActivity, FavouriteActivity::class.java))
    }

    fun openPlaylist() {
        // Switch to playlist tab instead of opening PlaylistActivity
        binding.bottomNavigation.selectedItemId = R.id.navigation_playlist
    }

    fun openPlayNext() {
    }

    //For requesting permission
    private fun requestRuntimePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    13
                )
                return false
            }
        } else {
            //android 13 or Higher permission request
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                    13
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                initializeLayout()
                loadFavouritesAndPlaylists()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeLayout() {
        search = false
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        sortOrder = sortEditor.getInt("sortOrder", 0)


        if (!musicListHasBeenLoaded) {

            MusicListMA = getAllAudio()
            musicListHasBeenLoaded = true
        }

        // Initialize adapter for use by fragments
        musicAdapter = MusicAdapter(this@MainActivity, MusicListMA)
        musicAdapter.setOnMusicItemClickListener(this) // Set the click listener

        // Load the default fragment (Library)
        loadFragment(LibraryFragment())

        //for refreshing layout on swipe from top
        binding.refreshLayout.setOnRefreshListener {
            // KHI REFRESH, CHÚNG TA SẼ QUÉT LẠI HOÀN TOÀN

            MusicListMA = getAllAudio()
            musicAdapter.updateMusicList(MusicListMA)

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is LibraryFragment) {
                // Thay vì load lại fragment, ta chỉ cần cập nhật adapter của nó

            }

            binding.refreshLayout.isRefreshing = false
        }

        //for retrieving favourites data using shared preferences
        FavouriteActivity.favouriteSongs = ArrayList()
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
        val jsonString = editor.getString("FavouriteSongs", null)
        val typeToken = object : TypeToken<ArrayList<Music>>() {}.type
        if (jsonString != null) {
            val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
            FavouriteActivity.favouriteSongs.addAll(data)
        }
        PlaylistManager.musicPlaylist = MusicPlaylist()
        val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
        if (jsonStringPlaylist != null) {
            val dataPlaylist: MusicPlaylist =
                GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
            PlaylistManager.musicPlaylist = dataPlaylist
        }
    }

    @SuppressLint("Recycle", "Range")
    fun getAllAudio(): ArrayList<Music> {
        val tempList = ArrayList<Music>()

        // Filter Only Music or Audio Files
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        // Thêm MediaStore.Audio.Media.DISPLAY_NAME vào projection để lấy tên file
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME // <-- THÊM DÒNG NÀY
        )

        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortingList[sortOrder], null
        )

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    // Lấy các cột dữ liệu
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)) ?: "Unknown"
                    val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) ?: "Unknown"
                    val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()

                    // --- BẮT ĐẦU PHẦN SỬA ĐỔI ---

                    // 1. Lấy TITLE từ metadata trước
                    var titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))

                    // 2. Lấy DISPLAY_NAME (tên file)
                    val displayNameC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))

                    // 3. Kiểm tra nếu title từ metadata không hợp lệ
                    //    "<unknown>" là một giá trị phổ biến mà Android trả về khi không có metadata.
                    if (titleC.isNullOrEmpty() || titleC == "<unknown>") {
                        // Nếu không hợp lệ, dùng tên file (bỏ phần mở rộng .mp3, .m4a, ...)
                        titleC = displayNameC.substringBeforeLast('.')
                    }

                    // --- KẾT THÚC PHẦN SỬA ĐỔI ---


                    // Chỉ thêm bài hát nếu có thời lượng hợp lệ
                    if (durationC > 0) {
                        val music = Music(
                            id = idC,
                            title = titleC, // Sử dụng title đã được xử lý
                            album = albumC,
                            artist = artistC,
                            path = pathC,
                            duration = durationC,
                            artUri = artUriC
                        )

                        if (File(music.path).exists()) tempList.add(music)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return tempList
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playbackStateReceiver)
        if (!PlayerActivity.isPlaying && PlayerActivity.musicService != null) {
            exitApplication()
        }
    }

    override fun onResume() {
        super.onResume()

        //for storing favourites data using shared preferences
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs)
        editor.putString("FavouriteSongs", jsonString)
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistManager.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()

        //for sorting
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        val sortValue = sortEditor.getInt("sortOrder", 0)
        if (sortOrder != sortValue) {
            sortOrder = sortValue
            MusicListMA = getAllAudio()
            musicAdapter.updateMusicList(MusicListMA)
        }

        if (::musicAdapter.isInitialized) {
            musicAdapter.updateMusicList(MusicListMA)
        }


        // Control visibility of NowPlaying based on service and music list
        if (PlayerActivity.musicService != null && PlayerActivity.musicListPA.isNotEmpty()) {
            binding.nowPlaying.visibility = View.VISIBLE
            val nowPlayingFragment = supportFragmentManager.findFragmentById(R.id.nowPlaying) as? NowPlaying
            nowPlayingFragment?.refreshUIContent()
        } else {
            binding.nowPlaying.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Removing search_view_menu inflation as search is now handled by HomeFragment

        //for setting gradient
//        findViewById<LinearLayout>(R.id.linearLayoutNav)?.setBackgroundResource(currentGradient[themeIndex])

        return true
    }

    fun setRefreshLayoutEnabled(enabled: Boolean) {
        binding.refreshLayout.isEnabled = enabled
    }

    // Implementation of MusicAdapter.OnMusicItemClickListener
    override fun onSongClicked(position: Int, isSearch: Boolean) {
        val selectedSong = if (isSearch) musicListSearch[position] else MusicListMA[position]

        // Create a new playlist with only the selected song
        val singleSongPlaylist = ArrayList<Music>()
        singleSongPlaylist.add(selectedSong)

        // Set the playlist and position
        PlayerActivity.songPosition = 0 // Always 0 since we're playing just one song
        PlayerActivity.musicListPA = singleSongPlaylist
        PlayerActivity.nowPlayingId = selectedSong.id

        // Clear the PlayNext queue and add only the selected song
        PlayNext.playNextList.clear()
        PlayNext.playNextList.add(selectedSong)

        if (PlayerActivity.musicService != null) {
            // Service is already bound and active
            PlayerActivity.musicService!!.createMediaPlayer() // Prepare the new song based on updated PlayerActivity.songPosition
            PlayerActivity.musicService!!.playMusic()      // Start playing it (this also sends broadcast for UI update)

            // Ensure the NowPlaying container is visible.
            // The NowPlayingFragment itself will update its content via the broadcast receiver.
            binding.nowPlaying.visibility = View.VISIBLE
        } else {
            // Service is not yet bound or not active, bind and start it.
            // onServiceConnected will handle preparing and playing the first song.
            val intent = Intent(this, MusicService::class.java)
            bindService(intent, this, BIND_AUTO_CREATE)
            startService(intent) // Ensures service keeps running even if activity unbinds and rebinds
        }
    }

    // ServiceConnection methods
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val isInitialConnection = PlayerActivity.musicService == null
        if (isInitialConnection) {
            val binder = service as MusicService.MyBinder
            PlayerActivity.musicService = binder.currentService()
            PlayerActivity.musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
            PlayerActivity.musicService!!.audioManager.requestAudioFocus(PlayerActivity.musicService, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)
        }

        // Prepare and play the song. MusicService.playMusic() will handle isPlaying, notification, and broadcast.
        PlayerActivity.musicService!!.createMediaPlayer()
        PlayerActivity.musicService!!.playMusic() // This will set isPlaying and send broadcast

        // The broadcast receiver will trigger nowPlayingFragment.refreshUIContent().
        // We just need to ensure the container is visible.
        if (PlayerActivity.musicService != null && PlayerActivity.musicListPA.isNotEmpty() &&
            PlayerActivity.songPosition >= 0 && PlayerActivity.songPosition < PlayerActivity.musicListPA.size) {
            binding.nowPlaying.visibility = View.VISIBLE
        } else {
            binding.nowPlaying.visibility = View.GONE
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        PlayerActivity.musicService = null
        binding.nowPlaying.visibility = View.GONE
        val nowPlayingFragment = supportFragmentManager.findFragmentById(R.id.nowPlaying) as? NowPlaying
        nowPlayingFragment?.refreshUIContent()
    }
}
