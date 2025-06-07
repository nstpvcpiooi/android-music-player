package com.example.musicplayer.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cloudinary.android.MediaManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityUploadBinding
import com.example.musicplayer.model.FirebaseUploadItem
import com.example.musicplayer.model.Music
import com.example.musicplayer.service.CloudApi
import com.example.musicplayer.service.CloudinaryApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.Executors

class UploadActivity : AppCompatActivity(), MusicAdapter.OnMusicItemClickListener {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var musicAdapter: MusicAdapter
    private var allMusicList = ArrayList<Music>()
    private var selectedMusic: Music? = null
    // Sử dụng interface để dễ dàng thay đổi implementation sau này
    private val cloudApi: CloudApi = CloudinaryApi()

    companion object {
        var mySelectionList: ArrayList<Music> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeCloudinary()
        loadMySelectionsFromPrefs()
        setupToolbar()
        setupViews()
        setupMusicList()
        setupSearchView()
    }

    // --- SETUP AND INITIALIZATION ---

    private fun initializeCloudinary() {
        Executors.newSingleThreadExecutor().execute {
            try {
                val config = mapOf(
                    "cloud_name" to "dzr0oakeq",
                    "api_key" to "518386354398875",
                    "api_secret" to "J3CGcuT6eFhgwaXfqBRf72cd7uE"
                )
                MediaManager.init(applicationContext, config)
                Log.i("Cloudinary", "MediaManager initialized successfully")
            } catch (e: Exception) {
                Log.e("Cloudinary", "Error initializing MediaManager", e)
            }
        }
    }

    private fun loadMySelectionsFromPrefs() {
        if (mySelectionList.isEmpty()) {
            val sharedPrefs = getSharedPreferences("MY_SELECTIONS", MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("selections", null)
            if (jsonString != null) {
                try {
                    val type = object : TypeToken<ArrayList<Music>>() {}.type
                    mySelectionList = Gson().fromJson(jsonString, type)
                } catch (e: Exception) {
                    Log.e("UploadActivity", "Error loading selections", e)
                    mySelectionList = ArrayList()
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        binding.loadingOverlay.visibility = View.GONE
        binding.songPreviewContainer.visibility = View.GONE
        binding.musicListContainer.visibility = View.VISIBLE

        binding.saveButton.setOnClickListener { startUploadAndSaveProcess() }
        binding.browseButton.setOnClickListener {
            binding.songPreviewContainer.visibility = View.GONE
            binding.musicListContainer.visibility = View.VISIBLE
        }
    }

    private fun setupMusicList() {
        allMusicList = ArrayList(MainActivity.MusicListMA)
        binding.musicListRecyclerView.layoutManager = LinearLayoutManager(this)
        musicAdapter = MusicAdapter(this, allMusicList)
        musicAdapter.setOnMusicItemClickListener(this)
        binding.musicListRecyclerView.adapter = musicAdapter
        binding.emptyMusicListText.visibility = if (allMusicList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSearchView() {
        binding.searchViewUA.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val searchResultList = ArrayList<Music>()
                if (!newText.isNullOrEmpty()) {
                    val userInput = newText.lowercase()
                    for (song in allMusicList) {
                        if (song.title.lowercase().contains(userInput) ||
                            song.artist.lowercase().contains(userInput)) {
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

    // --- CORE WORKFLOW ---

    override fun onSongClicked(position: Int, isSearch: Boolean) {
        val currentList = musicAdapter.getCurrentList()
        selectedMusic = if (position in 0 until currentList.size) currentList[position] else null

        selectedMusic?.let { music ->
            binding.songPreviewContainer.visibility = View.VISIBLE
            binding.musicListContainer.visibility = View.GONE
            binding.songName.text = music.title
            binding.uploadTopic.setText(music.title)
            binding.uploadSinger.setText(music.artist)
            binding.uploadAlbum.setText(music.album)
            Glide.with(this)
                .load(music.artUri)
                .apply(RequestOptions.placeholderOf(R.drawable.music_player_icon_slash_screen)
                    .error(R.drawable.music_player_icon_slash_screen))
                .into(binding.uploadImage)
        }
    }

    private fun startUploadAndSaveProcess() {
        val musicToUpload = selectedMusic ?: run {
            showToast("Vui lòng chọn một bài hát")
            return
        }

        if (!validateInputs()) {
            showToast("Vui lòng điền đầy đủ thông tin")
            return
        }

        val musicFile = File(musicToUpload.path)
        if (!musicFile.exists()) {
            showToast("File gốc không tồn tại. Không thể upload.")
            return
        }

        // Tạo public_id để đặt tên file trên server
        val title = binding.uploadTopic.text.toString().trim()
        val artist = binding.uploadSinger.text.toString().trim()
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val safeArtist = artist.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val publicId = "${safeArtist}_-_${safeTitle}_${System.currentTimeMillis()}"

        uploadToCloudinary(musicFile, publicId)
    }

    // In UploadActivity.kt

    private fun uploadToCloudinary(file: File, publicId: String) {
        showLoading(true)
        cloudApi.uploadFile(
            filePath = file.absolutePath,
            publicId = publicId,
            onStart = {},
            onProgress = { _, _ -> },

            // --- START OF FIX ---
            // Explicitly define the type of the 'response' parameter
            onSuccess = { response: com.example.musicplayer.model.CloudinaryResponse? ->
                runOnUiThread {
                    // Now 'response' is guaranteed to be a CloudinaryResponse? object
                    val url = response?.secureUrl
                    if (url != null) {
                        Log.i("Cloudinary", "Upload success: $url")
                        saveToFirebaseAndLocal(url)
                    } else {
                        showLoading(false)
                        // You can even provide more details from the response if it exists but url is null
                        Log.e("UploadActivity", "Cloudinary response was received but URL was null. Response: $response")
                        showToast("Lỗi: Không nhận được URL từ Cloudinary")
                    }
                }
            },
            // --- END OF FIX ---

            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    showToast("Lỗi upload: $error")
                }
            }
        )
    }
    private fun saveToFirebaseAndLocal(cloudinaryUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            showLoading(false)
            showToast("Lỗi: Người dùng chưa đăng nhập.")
            return
        }

        val title = binding.uploadTopic.text.toString().trim()
        val artist = binding.uploadSinger.text.toString().trim()
        val album = binding.uploadAlbum.text.toString().trim()

        val firebaseData = FirebaseUploadItem(
            url = cloudinaryUrl,
            title = title,
            singer = artist,
            album = album
        )

        val ref = FirebaseDatabase.getInstance().getReference("uploads").child(currentUser.uid)
        val newPostRef: DatabaseReference = ref.push()
        val firebaseKey = newPostRef.key

        newPostRef.setValue(firebaseData)
            .addOnSuccessListener {
                Log.i("Firebase", "Data saved successfully.")

                val originalMusic = selectedMusic!!
                val newCloudMusic = Music(
                    id = firebaseKey ?: originalMusic.id,
                    title = title,
                    album = album,
                    artist = artist,
                    duration = originalMusic.duration,
                    path = cloudinaryUrl,
                    artUri = originalMusic.artUri
                )

                val existingIndex = mySelectionList.indexOfFirst { it.id == newCloudMusic.id }
                if (existingIndex != -1) {
                    mySelectionList[existingIndex] = newCloudMusic
                } else {
                    mySelectionList.add(newCloudMusic)
                }

                saveMySelectionsToPrefs()

                showLoading(false)
                showToast("Upload và lưu thành công!")
                finish()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e("FirebaseError", "Failed to save data", exception)
                showToast("Lỗi khi lưu thông tin.")
            }
    }

    private fun saveMySelectionsToPrefs() {
        val sharedPrefs = getSharedPreferences("MY_SELECTIONS", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val jsonString = Gson().toJson(mySelectionList)
        editor.putString("selections", jsonString)
        editor.apply()
    }

    private fun validateInputs(): Boolean {
        return binding.uploadTopic.text.toString().trim().isNotEmpty() &&
                binding.uploadSinger.text.toString().trim().isNotEmpty() &&
                binding.uploadAlbum.text.toString().trim().isNotEmpty()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}