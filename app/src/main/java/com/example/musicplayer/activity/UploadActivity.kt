package com.example.musicplayer.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityUploadBinding
import com.example.musicplayer.model.Music
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadActivity : AppCompatActivity(), MusicAdapter.OnMusicItemClickListener {
    private lateinit var binding: ActivityUploadBinding
    private var selectedMusic: Music? = null
    private lateinit var musicAdapter: MusicAdapter
    private var allMusicList = ArrayList<Music>()

    companion object {
        // List to store all selected/saved music
        var mySelectionList: ArrayList<Music> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load previously selected songs
        loadSavedSelections()

        setupToolbar()
        setupViews()
        setupMusicList()
        setupSearchView()
    }

    private fun loadSavedSelections() {
        // Load saved selections from SharedPreferences
        val sharedPrefs = getSharedPreferences("MY_SELECTIONS", MODE_PRIVATE)
        val jsonString = sharedPrefs.getString("selections", null)

        if (jsonString != null) {
            val type = object : TypeToken<ArrayList<Music>>() {}.type
            mySelectionList = try {
                Gson().fromJson(jsonString, type)
            } catch (e: Exception) {
                ArrayList()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        // Initially hide the loading overlay
        binding.loadingOverlay.visibility = View.GONE

        // Set initial state
        binding.songPreviewContainer.visibility = View.GONE
        binding.musicListContainer.visibility = View.VISIBLE

        // Setup save button (FAB)
        binding.saveButton.setOnClickListener {
            if (selectedMusic != null && validateInputs()) {
                saveSelectedMusic()
            } else {
                showToast("Vui lòng chọn bài hát và điền đầy đủ thông tin")
            }
        }

        // Setup the "Change Selection" button
        binding.browseButton.setOnClickListener {
            binding.songPreviewContainer.visibility = View.GONE
            binding.musicListContainer.visibility = View.VISIBLE
            binding.topAppBar.visibility = View.VISIBLE
        }
    }

    private fun setupMusicList() {
        // Get music list from MainActivity
        allMusicList = ArrayList(MainActivity.MusicListMA)

        // Setup RecyclerView
        binding.musicListRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.musicListRecyclerView.setHasFixedSize(true)
        musicAdapter = MusicAdapter(this, allMusicList)
        musicAdapter.setOnMusicItemClickListener(this)
        binding.musicListRecyclerView.adapter = musicAdapter

        // Show message if list is empty
        if (allMusicList.isEmpty()) {
            binding.emptyMusicListText.visibility = View.VISIBLE
            binding.musicListRecyclerView.visibility = View.GONE
        } else {
            binding.emptyMusicListText.visibility = View.GONE
            binding.musicListRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupSearchView() {
        binding.searchViewUA.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    val searchResultList = ArrayList<Music>()
                    val userInput = newText.lowercase()

                    for (song in allMusicList) {
                        if (song.title.lowercase().contains(userInput) ||
                            song.artist.lowercase().contains(userInput) ||
                            song.album.lowercase().contains(userInput)) {
                            searchResultList.add(song)
                        }
                    }

                    musicAdapter.updateMusicList(searchResultList)
                } else {
                    musicAdapter.updateMusicList(allMusicList)
                }
                return true
            }
        })
    }

    override fun onSongClicked(position: Int, isSearch: Boolean) {
        // Update selected music - the isSearch parameter is not relevant here
        // as we're using our own filtered list
        val currentList = musicAdapter.getCurrentList()
        selectedMusic = if (position < currentList.size) currentList[position] else null

        // Update UI to show selected song
        selectedMusic?.let { music ->
            // Hide search bar and show details view
            binding.songPreviewContainer.visibility = View.VISIBLE
            binding.musicListContainer.visibility = View.GONE

            // Update preview fields
            binding.songName.text = music.title
            binding.uploadTopic.setText(music.title)
            binding.uploadSinger.setText(music.artist)
            binding.uploadAlbum.setText(music.album)

            // Load image with proper error handling
            com.bumptech.glide.Glide.with(this)
                .load(music.artUri)
                .apply(com.bumptech.glide.request.RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .error(R.drawable.music_player_icon_slash_screen))
                .into(binding.uploadImage)
        }
    }

    private fun validateInputs(): Boolean {
        return binding.uploadTopic.text.toString().trim().isNotEmpty() &&
                binding.uploadSinger.text.toString().trim().isNotEmpty() &&
                binding.uploadAlbum.text.toString().trim().isNotEmpty()
    }

    private fun saveSelectedMusic() {
        // Show loading animation
        showLoading(true)

        // Simulate network delay (for UX purposes)
        binding.root.postDelayed({
            val music = selectedMusic

            if (music == null) {
                showLoading(false)
                showToast("Bài hát chưa được chọn")
                return@postDelayed
            }

            // Create a copy of the music with the user-edited metadata
            val updatedMusic = Music(
                id = music.id,
                title = binding.uploadTopic.text.toString().trim(),
                album = binding.uploadAlbum.text.toString().trim(),
                artist = binding.uploadSinger.text.toString().trim(),
                duration = music.duration,
                path = music.path,
                artUri = music.artUri
            )

            // Check if song already exists in list to avoid duplicates
            val existingIndex = mySelectionList.indexOfFirst { it.id == updatedMusic.id }
            if (existingIndex >= 0) {
                // Replace with updated metadata
                mySelectionList[existingIndex] = updatedMusic
            } else {
                // Add to selections
                mySelectionList.add(updatedMusic)
            }

            // Save to SharedPreferences
            saveSelectionToPrefs()

            showLoading(false)
            showToast("Đã thêm vào danh sách của tôi")
            finish()
        }, 1000) // 1 second delay to simulate "uploading"
    }

    private fun saveSelectionToPrefs() {
        val sharedPrefs = getSharedPreferences("MY_SELECTIONS", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val jsonString = Gson().toJson(mySelectionList)
        editor.putString("selections", jsonString)
        editor.apply()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
