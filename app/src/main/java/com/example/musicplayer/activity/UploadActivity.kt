package com.example.musicplayer.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityUploadBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.CloudinaryApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UploadActivity : AppCompatActivity(), MusicAdapter.OnMusicItemClickListener {
    private lateinit var binding: ActivityUploadBinding
    private var selectedMusic: Music? = null
    private val cloudApi = CloudinaryApi()
    private lateinit var musicAdapter: MusicAdapter
    private var allMusicList = ArrayList<Music>()

    companion object {
        private var isCloudinaryInit = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!isCloudinaryInit) {
            isCloudinaryInit = true
            // Cloudinary config
            val config = mapOf(
                "cloud_name" to "dzr0oakeq",
                "api_key" to "518386354398875",
                "api_secret" to "J3CGcuT6eFhgwaXfqBRf72cd7uE",
                "secure" to true
            )
            try {
                MediaManager.init(this, config)
                Log.i("Cloudinary", "MediaManager initialized successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Cloudinary", "Error initializing MediaManager: ${e.message}")
                showToast("Lỗi khởi tạo Cloudinary")
                return
            }
        }

        setupToolbar()
        setupViews()
        setupMusicList()
        setupSearchView()
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
                uploadSelectedMusic()
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

    private fun uploadSelectedMusic() {
        val music = selectedMusic ?: return showToast("Bài hát chưa được chọn")

        lifecycleScope.launch {
            showLoading(true)
            try {
                // Create a temporary file copy of the music
                val tempFile = withContext(Dispatchers.IO) {
                    val originalFile = File(music.path)
                    if (!originalFile.exists()) {
                        throw IllegalStateException("Không tìm thấy file nhạc")
                    }

                    // Create a temp file and copy the content
                    File.createTempFile("upload_", ".tmp", cacheDir).apply {
                        originalFile.inputStream().use { input ->
                            outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                // Upload the file
                uploadToCloudinary(tempFile)
            } catch (e: Exception) {
                showLoading(false)
                showToast("Lỗi xử lý file: ${e.localizedMessage}")
                Log.e("UploadError", "Error processing file", e)
            }
        }
    }

    private fun uploadToCloudinary(file: File) {
        showLoading(true)

        cloudApi.uploadFile(
            filePath = file.absolutePath,
            onStart = { runOnUiThread { showToast("Bắt đầu upload...") } },
            onProgress = { bytes, totalBytes ->
                val progress = (bytes * 100 / totalBytes).toInt()
                runOnUiThread { updateProgress(progress) }
            },
            onSuccess = { response ->
                runOnUiThread {
                    showLoading(false)
                    file.delete()
                    Log.i("Cloudinary", "Upload successful: ${response?.secureUrl}")
                    val url = response?.secureUrl
                    if (url != null) {
                        saveToFirebase(url)
                    } else {
                        showToast("Upload thành công nhưng URL null")
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    showToast("Lỗi upload: $error")
                    Log.e("UploadError", "Lỗi khi upload: $error")
                    file.delete()
                }
            }
        )
    }

    private fun saveToFirebase(fileUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showToast("Người dùng chưa ��ăng nhập")
            return
        }

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("uploads").child(userId)

        val data = mapOf(
            "url" to fileUrl,
            "title" to binding.uploadTopic.text.toString().trim(),
            "singer" to binding.uploadSinger.text.toString().trim(),
            "album" to binding.uploadAlbum.text.toString().trim(),
            "timestamp" to System.currentTimeMillis()
        )

        ref.push().setValue(data)
            .addOnSuccessListener {
                showToast("Lưu thành công")
                finish()
            }
            .addOnFailureListener {
                showToast("Lỗi khi lưu vào Firebase: ${it.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateProgress(progress: Int) {
        // You could add a text view in the progress_layout to display the percentage
        // binding.loadingView.progressText?.text = "$progress%"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
