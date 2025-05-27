package com.example.musicplayer.activity

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityDownloadBinding
import com.example.musicplayer.model.Music
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var musicAdapter: MusicAdapter
    private val musicList = ArrayList<Music>()   // chỉ dùng để hiển thị

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database     = FirebaseDatabase.getInstance().reference

        binding.backBtndown.setOnClickListener { finish() }

        binding.backBtndown.setOnClickListener { finish() }

        setupRecyclerView()
        fetchMusicList()
    }

    // ----------------------------- UI ----------------------------------------

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(this, musicList)
        binding.playlistRV.layoutManager = LinearLayoutManager(this)
        binding.playlistRV.adapter      = musicAdapter

        musicAdapter.setOnItemClickListener { position ->
            val music = musicList[position]
            AlertDialog.Builder(this)
                .setTitle(music.title)
                .setMessage("Bạn muốn làm gì với bài này?")
                .setPositiveButton("Phát nhạc") { dialog, _ ->
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra("url", music.path)        // truyền url (dù online hay offline)
                        putExtra("title", music.title)
                    }
                    startActivity(intent)
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

    // --------------------------- Firebase ------------------------------------

    private fun fetchMusicList() {
        database.child("uploads")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    musicList.clear()

                    // Lặp qua tất cả người dùng
                    for (userSnap in snapshot.children) {
                        for (item in userSnap.children) {
                            val title = item.child("title").getValue(String::class.java) ?: continue
                            val url   = item.child("url").getValue(String::class.java)   ?: continue

                            musicList.add(
                                Music(
                                    id = item.key ?: "",
                                    title = title,
                                    album = item.child("album").getValue(String::class.java) ?: "",
                                    artist = item.child("singer").getValue(String::class.java) ?: "",
                                    duration = 0L,
                                    artUri = "",
                                    path = url
                                )
                            )
                        }
                    }

                    musicAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DownloadActivity,
                        "Lỗi Firebase: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }


    // --------------------------- Download -------------------------------------

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
}
