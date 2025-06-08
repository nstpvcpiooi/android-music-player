package com.example.musicplayer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivitySelectionBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.activity.PlaylistDetailsActivity

class SelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectionBinding
    private lateinit var adapter: MusicAdapter
    private lateinit var allSongsList: ArrayList<Music>
    private lateinit var songsToSelectForPlaylist: ArrayList<Music> // Songs selected in this UI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        setContentView(binding.root)

        // Initialize lists
        allSongsList = ArrayList(MainActivity.MusicListMA) // All available songs to display

        // Create a mutable copy of the songs in the current playlist being edited.
        // This list ('songsToSelectForPlaylist') will be modified by user interactions.
        songsToSelectForPlaylist = try {
            ArrayList(PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist)
        } catch (e: IndexOutOfBoundsException) {
            // Handle cases where currentPlaylistPos might be invalid or list doesn't exist
            ArrayList()
        }

        // Setup Toolbar
        setSupportActionBar(binding.toolbarSA)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSA.setNavigationOnClickListener {
            // Back button pressed: DO NOT SAVE. Just finish.
            finish()
        }

        // Setup RecyclerView and Adapter
        binding.selectionRV.setItemViewCacheSize(30)
        binding.selectionRV.setHasFixedSize(true)
        binding.selectionRV.layoutManager = LinearLayoutManager(this)
        // The adapter displays ALL songs (allSongsList)
        // and uses songsToSelectForPlaylist to determine which ones are currently "checked"
        adapter = MusicAdapter(
            this,
            allSongsList, // Display all songs from MainActivity.MusicListMA
            selectionActivity = true,
            currentSelectedSongsForPlaylist = songsToSelectForPlaylist // Pass the list of currently selected songs
        )
        binding.selectionRV.adapter = adapter

        // Setup FAB to save changes
        binding.saveSelectionFab.setOnClickListener {
            try {
                PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist.clear()
                PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist.addAll(songsToSelectForPlaylist)
                Toast.makeText(this, "Playlist updated", Toast.LENGTH_SHORT).show()
                setResult(AppCompatActivity.RESULT_OK) // Set result to OK
                finish()
            } catch (e: IndexOutOfBoundsException) {
                Toast.makeText(this, "Error saving playlist", Toast.LENGTH_SHORT).show()
            }
        }

        //for search View
        binding.searchViewSA.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val searchResultList = ArrayList<Music>()
                if (!newText.isNullOrEmpty()) {
                    val userInput = newText.lowercase()
                    for (song in allSongsList) { // Search in allSongsList
                        if (song.title.lowercase().contains(userInput)) {
                            searchResultList.add(song)
                        }
                    }
                    adapter.updateMusicList(searchList = searchResultList)
                } else {
                    adapter.updateMusicList(searchList = allSongsList) // Show all songs if query is empty
                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        //for black theme checking
        if(MainActivity.themeIndex == 4) {
            // For Material 3, SearchView styling is typically handled by themes.
        }
    }
}

