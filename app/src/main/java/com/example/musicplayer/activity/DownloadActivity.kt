package com.example.musicplayer.activity

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import java.io.File

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

        // Trong DownloadActivity.kt -> setupRecyclerView()

        musicAdapter.setOnItemClickListener { position ->
            val music = musicList[position]
            AlertDialog.Builder(this)
                .setTitle(music.title)
                .setMessage("Bạn muốn làm gì với bài này?")
                .setPositiveButton("Phát nhạc") { dialog, _ ->
                    // --- BẮT ĐẦU THAY ĐỔI ---

                    // 1. Tạo một danh sách mới chỉ chứa bài hát này
                    val singleSongList = ArrayList<Music>()
                    singleSongList.add(music)

                    // 2. Tạo Intent và truyền cả danh sách và một "class" identifier mới
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        // Đặt vị trí là 0 vì danh sách chỉ có 1 bài
                        putExtra("index", 0)
                        // Đặt định danh để PlayerActivity biết cách xử lý
                        putExtra("class", "OnlineSong")
                    }

                    // 3. Gán danh sách nhạc cho PlayerActivity
                    //    (Chúng ta cần làm điều này vì PlayerActivity.musicListPA là static)
                    PlayerActivity.musicListPA = singleSongList

                    startActivity(intent)

                    // --- KẾT THÚC THAY ĐỔI ---
                    dialog.dismiss()
                }
                .setNeutralButton("Tải về") { dialog, _ ->
                    downloadMusic(music.title, music.album, music.artist, music.path)
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
    // Bạn sẽ đặt hàm này trong DownloadActivity.kt

    private fun downloadMusic(title: String, album: String, artist: String, fileUrl: String) {
        try {
            // --- Bước 1: Chuẩn bị thông tin và đường dẫn ---

            // Tạo URI từ URL của file trên mây
            val uri = Uri.parse(fileUrl)

            // Làm sạch tên file để tránh các ký tự không hợp lệ
            val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")

            // Lấy phần mở rộng của file, mặc định là ".mp3"
            val extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl)
                ?.takeIf { it.isNotBlank() }
                ?.let { ".$it" } ?: ".mp3"

            // Tạo tên file cuối cùng, ví dụ: "Ten_Bai_Hat.mp3"
            val fileName = "$safeTitle$extension"

            // Lấy đối tượng File của thư mục Music công khai
            val musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

            // Tạo đường dẫn tuyệt đối đến file đích
            val destinationPath = File(musicDirectory, fileName).path

            // Kiểm tra xem file đã tồn tại trên máy chưa
            if (File(destinationPath).exists()) {
                Toast.makeText(this, "'$title' đã có trong thư viện.", Toast.LENGTH_SHORT).show()
                return // Dừng lại nếu file đã tồn tại
            }

            // --- Bước 2: Bắt đầu quá trình tải bằng DownloadManager ---

            // Tạo yêu cầu tải
            val request = DownloadManager.Request(uri).apply {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName)
                setTitle("Đang tải: $title")
                setDescription("Bài hát sẽ được thêm vào thư viện nhạc của bạn.")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            // Gửi yêu cầu tải đến hệ thống
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)


            // --- Bước 3: Tự thêm bài hát mới vào danh sách nhạc của ứng dụng ---

            // Tạo một đối tượng Music mới với thông tin chính xác
            val newLocalSong = Music(
                id = System.currentTimeMillis().toString(), // Dùng timestamp làm ID tạm thời
                title = title,           // Title từ server
                album = album,           // Album từ server
                artist = artist,         // Artist từ server
                path = destinationPath,  // Đường dẫn cục bộ đã xác định
                duration = 0L,           // Sẽ được cập nhật khi hệ thống quét lại
                artUri = ""              // Chưa có ảnh album
            )

            // Gọi hàm tĩnh trong MainActivity để cập nhật danh sách nhạc
            MainActivity.addSongToMusicList(newLocalSong)

            // Thông báo cho người dùng rằng quá trình tải đã bắt đầu
            Toast.makeText(this, "Bắt đầu tải '$title'...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Xử lý các lỗi có thể xảy ra
            Toast.makeText(this, "Lỗi khi tải: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}