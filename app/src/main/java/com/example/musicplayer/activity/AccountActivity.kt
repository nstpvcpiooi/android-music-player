package com.example.musicplayer.activity

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.activity.DownloadActivity.Companion.downloadIndex
import com.example.musicplayer.activity.DownloadActivity.Companion.downloadPlaylist
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityAccountBinding
import com.example.musicplayer.model.Music
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountActivity : AppCompatActivity() {

    companion object {
        private lateinit var binding: ActivityAccountBinding
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var database: DatabaseReference
        private lateinit var musicAdapter: MusicAdapter
        val musicList = ArrayList<Music>()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        database = FirebaseDatabase.getInstance().reference

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.backBtnAccount.setOnClickListener { finish() }
        binding.logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.uploadMusicBtn.setOnClickListener{
            startActivity(Intent(this, UploadActivity::class.java))
            finish()
        }

        binding.emailValue.setText(firebaseAuth.currentUser?.email.toString());

        setupRecyclerView()
        fetchUploadedMusic()
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(this, musicList)
        binding.uploadedMusicRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            adapter = musicAdapter
        }

        binding.uploadedMusicRecyclerView.adapter = musicAdapter

        musicAdapter.setOnItemClickListener { position ->
            val music = musicList[position]

            if (music.path.isBlank() || !music.path.startsWith("http")) {
                Toast.makeText(this, "URL nhạc không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(music.title)
                .setMessage("Bạn muốn làm gì với bài này?")
                .setPositiveButton("Phát nhạc") { dialog, _ ->

                    // Trong AccountActivity, trước khi mở PlayerActivityOnline
                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkInfo = connectivityManager.activeNetworkInfo
                    if (networkInfo == null || !networkInfo.isConnected) {
                        Toast.makeText(this, "Vui lòng kiểm tra kết nối mạng", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Phát playlist online với PlayerActivityOnline
                    downloadPlaylist = ArrayList(musicList)
                    downloadIndex    = position
                    startActivity(Intent(this, PlayerActivityOnline::class.java))
                    dialog.dismiss()
                }
                .setNeutralButton("Tải về") { dialog, _ ->
                    downloadMusic(music.title, music.path)
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    /**
     * Tải file nhạc từ URL Cloudinary về thư mục Music của thiết bị
     */
    private fun downloadMusic(title: String, fileUrl: String) {
        try {
            val uri = Uri.parse(fileUrl)

            val extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl)
                ?.takeIf { it.isNotBlank() }
                ?.let { ".$it" } ?: ".mp3"

            val request = DownloadManager.Request(uri).apply {
                setTitle("Tải: $title")
                setDescription("Đang tải file nhạc…")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MUSIC, "$title$extension"
                )
                // Cho phép tải qua mạng di động/roaming
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(this,
                "Đang tải $title…", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this,
                "Lỗi khi tải: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun fetchUploadedMusic() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("uploads").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                musicList.clear()

                for (item in snapshot.children) {
                    val title = item.child("title").getValue(String::class.java) ?: continue
                    val url   = item.child("url").getValue(String::class.java)   ?: continue

                    // Dùng thuộc tính path để tái sử dụng MusicAdapter
                    musicList.add(
                        Music(
                            id = item.key ?: "",
                            title = title,
                            album = item.child("album").getValue(String::class.java) ?: "",
                            artist = item.child("singer").getValue(String::class.java) ?: "",
                            duration = 0L,
                            artUri = "",
                            path = url                     // CHÍNH LÀ URL CLOUDINARY
                        )
                    )
                }
                musicAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Load failed: ${error.message}")
            }
        })
    }
}