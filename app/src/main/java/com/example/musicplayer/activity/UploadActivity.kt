package com.example.musicplayer.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cloudinary.android.MediaManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivityUploadBinding
import com.example.musicplayer.service.CloudinaryApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private var fileUri: Uri? = null
    private val FILE_PICK_CODE = 1001
    private val cloudApi = CloudinaryApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupViews()
    }

    private fun setupViews() {
        binding.loadingView.root.visibility = View.GONE

        binding.uploadImage.setOnClickListener {
            pickFileFromStorage()
        }

        binding.saveButton.setOnClickListener {
            if (fileUri != null && validateInputs()) {
                uploadSelectedFile()
            } else {
                showToast("Vui lòng chọn file và điền đầy đủ thông tin")
            }
        }
    }

    private fun pickFileFromStorage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("audio/*") // Chỉ chọn file audio
        startActivityForResult(intent, FILE_PICK_CODE)
    }

    private fun validateInputs(): Boolean {
        return binding.uploadTopic.text.toString().trim().isNotEmpty() &&
                binding.uploadSinger.text.toString().trim().isNotEmpty() &&
                binding.uploadAlbum.text.toString().trim().isNotEmpty()
    }

    private fun uploadSelectedFile() {
        val uri = fileUri ?: return showToast("File chưa được chọn")
        lifecycleScope.launch {
            showLoading(true)
            try {
                // 1) Đọc file trên IO thread
                val tempFile = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        File.createTempFile("upload_", ".tmp", cacheDir).apply {
                            outputStream().use { output -> inputStream.copyTo(output) }
                        }
                    } ?: throw IllegalStateException("Không thể đọc file")
                }
                // 2) Upload (CloudinaryApi vẫn dùng callback nội bộ)
                uploadToCloudinary(tempFile)
            } catch (e: Exception) {
                showLoading(false)
                showToast("Lỗi xử lý file: ${e.localizedMessage}")
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
                    Log.e("Cloudinary", response?.secureUrl.toString())
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
            showToast("Người dùng chưa đăng nhập")
            return
        }

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("uploads").child(userId)

        val data = mapOf(
            "url" to fileUrl,  // Đổi "path" thành "url"
            "title" to binding.uploadTopic.text.toString().trim(),
            "singer" to binding.uploadSinger.text.toString().trim(),
            "album" to binding.uploadAlbum.text.toString().trim(),
            "timestamp" to System.currentTimeMillis()
        )

        ref.push().setValue(data)
            .addOnSuccessListener {
                showToast("Lưu thành công")
                startActivity(Intent(this, AccountActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                showToast("Lỗi khi lưu vào Firebase: ${it.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingView.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !show
    }

    private fun updateProgress(progress: Int) {
        binding.loadingView.loadingLayout.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data

            // Lấy tên file nhạc (title)
            val fileName = getFileNameFromMediaStoreUri(this, fileUri)
            binding.uploadTopic.setText(fileName ?: "")

            // Lấy album
            val albumName = getAlbumNameFromUri(this, fileUri)
            binding.uploadAlbum.setText(albumName)

            // Lấy ca sĩ (artist)
            val artistName = getArtistNameFromUri(this, fileUri)
            binding.uploadSinger.setText(artistName)

            Log.e("UploadActivity", "FileUri: $fileUri, title=$fileName, album=$albumName, artist=$artistName")
        }
    }

    fun getFileNameFromMediaStoreUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        var fileName: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                fileName = cursor.getString(columnIndex)
            }
        }
        return fileName
    }

    fun getAlbumNameFromUri(context: Context, uri: Uri?): String {
        var albumName = "Unknown Album"
        if (uri == null) return albumName

        val projection = arrayOf(MediaStore.Audio.Media.ALBUM)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    albumName = cursor.getString(albumColumn)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return albumName
    }

    fun getArtistNameFromUri(context: Context, uri: Uri?): String {
        if (uri == null) return ""
        val mmr = android.media.MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            return artist ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mmr.release()
        }
        return ""
    }
}
