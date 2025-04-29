package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.model.Music
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.PlayNext
import com.example.musicplayer.R
import com.google.gson.GsonBuilder
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.utils.exitApplication
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var musicAdapter: MusicAdapter

    companion object{
        lateinit var MusicListMA : ArrayList<Music>
        lateinit var musicListSearch : ArrayList<Music>
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
        var sortOrder: Int = 0 //m
        val sortingList = arrayOf(MediaStore.Audio.Media.DATE_ADDED + " DESC", MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.SIZE + " DESC")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeEditor = getSharedPreferences("THEMES", MODE_PRIVATE)
        themeIndex = themeEditor.getInt("themeIndex", 0)
        setTheme(currentThemeNav[themeIndex])

        //chuyen doi file xml -> view object
        binding = ActivityMainBinding.inflate(layoutInflater)


        setContentView(binding.root)


        //side bar
//        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
//        binding.root.addDrawerListener(toggle)
//        toggle.syncState()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        //kiem tra theme dark
        if(themeIndex == 4 &&  resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)
            Toast.makeText(this, "Black Theme Works Best in Dark Mode!!", Toast.LENGTH_LONG).show()

        //yeu cau quyen truy cap cua ung dung
        if(requestRuntimePermission()){
            initializeLayout()
            //for retrieving favourites data using shared preferences
//            FavouriteActivity.favouriteSongs = ArrayList()
//            val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
//            val jsonString = editor.getString("FavouriteSongs", null)
//            val typeToken = object : TypeToken<ArrayList<Music>>(){}.type
//            if(jsonString != null){
//                val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
//                FavouriteActivity.favouriteSongs.addAll(data)
//            }
//            PlaylistActivity.musicPlaylist = MusicPlaylist()
//            val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
//            if(jsonStringPlaylist != null){
//                val dataPlaylist: MusicPlaylist = GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
//                PlaylistActivity.musicPlaylist = dataPlaylist
//            }
        }


        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "MainActivity")
            startActivity(intent)
        }
        binding.favouriteBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, FavouriteActivity::class.java))
        }
        binding.playlistBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlaylistActivity::class.java))
        }
        binding.playNextBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlayNext::class.java))
        }
        binding.accountButton.setOnClickListener{
            startActivity(Intent(this, AccountActivity::class.java))
        }
        binding.downloadButton.setOnClickListener{
            startActivity(Intent(this, DownloadActivity::class.java))
        }
//        binding.navView.setNavigationItemSelectedListener{
//            when(it.itemId)
//            {
////                R.id.navFeedback -> startActivity(Intent(this@MainActivity, FeedbackActivity::class.java))
//                R.id.navSettings -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
//                R.id.navAbout -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
//                R.id.navExit -> {
//                    val builder = MaterialAlertDialogBuilder(this)
//                    builder.setTitle("Exit")
//                        .setMessage("Do you want to close app?")
//                        .setPositiveButton("Yes"){ _, _ ->
//                            exitApplication()
//                        }
//                        .setNegativeButton("No"){dialog, _ ->
//                            dialog.dismiss()
//                        }
//                    val customDialog = builder.create()
//                    customDialog.show()
//
//                    setDialogBtnBackground(this, customDialog)
//                }
//            }
//            true
//        }
    }
    //For requesting permission
    private fun requestRuntimePermission() :Boolean{
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 13)
                return false
            }
        }else{
            //android 13 or Higher permission request
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO), 13)
                return false
            }
        }
        return true
    }


//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(requestCode == 13){
//            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Permission Granted",Toast.LENGTH_SHORT).show()
//                initializeLayout()
//            }
////            else ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
//        }
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if(toggle.onOptionsItemSelected(item))
//            return true
//        return super.onOptionsItemSelected(item)
//    }

    @SuppressLint("SetTextI18n")
    private fun initializeLayout(){
        search = false
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        sortOrder = sortEditor.getInt("sortOrder", 0)

        MusicListMA = getAllAudio()

        //render song list, musicRV is the recycle in xml file
        binding.musicRV.setHasFixedSize(true)
        binding.musicRV.setItemViewCacheSize(13)
        binding.musicRV.layoutManager = LinearLayoutManager(this@MainActivity)

        //xu ly all about this recycle trong 1 lop rieng
        musicAdapter = MusicAdapter(this@MainActivity, MusicListMA)
        binding.musicRV.adapter = musicAdapter


        binding.totalSongs.text  = "Total Songs : "+musicAdapter.itemCount

        //for refreshing layout on swipe from top
        binding.refreshLayout.setOnRefreshListener {
            MusicListMA = getAllAudio()
            musicAdapter.updateMusicList(MusicListMA)

            binding.refreshLayout.isRefreshing = false
        }
    }

    @SuppressLint("Recycle", "Range")
    private fun getAllAudio(): ArrayList<Music> {
        val tempList = ArrayList<Music>()

        // loc ra va get cac file audio
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.MIME_TYPE + " LIKE 'audio/%'"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortingList[sortOrder], null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val getTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                    val getId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)) ?: "Unknown"
                    val getAlbum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) ?: "Unknown"
                    val getArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                    val getPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val getDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val getAlbumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val getUri = Uri.parse("content://media/external/audio/albumart")
                    val getArtUri = Uri.withAppendedPath(getUri, getAlbumId).toString()

                    // Only add the music file if the duration is greater than 0
                    if (getDuration > 0) {
                        val music = Music(
                            id = getId,
                            title = getTitle,
                            album = getAlbum,
                            artist = getArtist,
                            path = getPath,
                            duration = getDuration,
                            artUri = getArtUri
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
        //uncomment neu muon xoa tab ung dung nhung nhac van chay
//        if(!PlayerActivity.isPlaying && PlayerActivity.musicService != null){
//           exitApplication()
//        }
        exitApplication()
    }

    override fun onResume() {
        super.onResume()
        //check behaviour
        Toast.makeText(this, "check: Resume main activity", Toast.LENGTH_SHORT).show()

        //store fav and playlist info so when exit app, it wont lose, can add fav attribute do music data class and
        // perform, but slower and cant make use of free resources like shared preferences
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs)
        editor.putString("FavouriteSongs", jsonString)
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()

        //for sorting
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        val sortValue = sortEditor.getInt("sortOrder", 0)
        if(sortOrder != sortValue){
            sortOrder = sortValue
            MusicListMA = getAllAudio()
            musicAdapter.updateMusicList(MusicListMA)
        }
        if(PlayerActivity.musicService != null) binding.nowPlaying.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.search_view_menu, menu)
//        //for setting gradient
//        findViewById<LinearLayout>(R.id.linearLayoutNav)?.setBackgroundResource(currentGradient[themeIndex])
//
//        val searchView = menu?.findItem(R.id.searchView)?.actionView as SearchView
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
//            override fun onQueryTextSubmit(query: String?): Boolean = true
//            override fun onQueryTextChange(newText: String?): Boolean {
//                musicListSearch = ArrayList()
//                if(newText != null){
//                    val userInput = newText.lowercase()
//                    for (song in MusicListMA)
//                        if(song.title.lowercase().contains(userInput))
//                            musicListSearch.add(song)
//                    search = true
//                    musicAdapter.updateMusicList(searchList = musicListSearch)
//                }
//                return true
//            }
//        })
        return super.onCreateOptionsMenu(menu)
    }
}