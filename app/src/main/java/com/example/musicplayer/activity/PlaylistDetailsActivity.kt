package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.SelectionActivity
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityPlaylistDetailsBinding
import com.example.musicplayer.databinding.BottomSheetPlaylistOptionsBinding
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.utils.checkPlaylist
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder

class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private lateinit var adapter: MusicAdapter

    companion object{
        var currentPlaylistPos: Int = -1
        private const val SELECTION_ACTIVITY_REQUEST_CODE = 1 // Added request code
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentPlaylistPos = intent.extras?.get("index") as Int

        try{
            PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist)
        }
        catch(e: Exception){}

        // Set up RecyclerView
        binding.playlistDetailsRV.setItemViewCacheSize(10)
        binding.playlistDetailsRV.setHasFixedSize(true)
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this)
        adapter = MusicAdapter(
            this,
            PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist,
            playlistDetails = true
        )
        binding.playlistDetailsRV.adapter = adapter

        // Set up toolbar
        binding.toolbarPD.setNavigationOnClickListener { finish() }

        // Set up buttons
        binding.playBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsAdapter") // Changed "PlaylistDetails" to "PlaylistDetailsAdapter"
            startActivity(intent)
        }

        binding.editBtnPD.setOnClickListener {
            val intent = Intent(this, SelectionActivity::class.java)
            // intent.putExtra("playlist_index", currentPlaylistPos) // Optional: if SelectionActivity needs it directly
            startActivityForResult(intent, SELECTION_ACTIVITY_REQUEST_CODE) // Start for result
        }

        binding.moreFeaturesBtn.setOnClickListener {
            showPlaylistOptionsBottomSheet()
        }

        // Update UI
        updateUI()
    }

    // Add onActivityResult to handle the result from SelectionActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECTION_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Playlist was updated in SelectionActivity, refresh UI here
                adapter.refreshPlaylist()
                updateUI()
            }
        }
    }

    private fun showPlaylistOptionsBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetPlaylistOptionsBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(bottomSheetBinding.root)

        // Set up bottom sheet options
        bottomSheetBinding.addToQueueOption.setOnClickListener {
            // Add playlist to queue functionality
            if(PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist.isNotEmpty()) {
                PlayerActivity.musicListPA.addAll(PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist)
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetBinding.playNextOption.setOnClickListener {
            // Play next functionality
            if(PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist.isNotEmpty()) {
                PlayerActivity.musicListPA.addAll(0, PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist)
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetBinding.shuffleOption.setOnClickListener {
            // Shuffle functionality
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.deleteOption.setOnClickListener {
            // Delete playlist functionality
            showDeletePlaylistDialog()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showDeletePlaylistDialog() {
        val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
        builder.setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete this playlist?")
            .setPositiveButton("Yes") { dialog, _ ->
                PlaylistManager.musicPlaylist.ref.removeAt(currentPlaylistPos)
                adapter.refreshPlaylist()
                dialog.dismiss()
                finish() // Close this activity after deletion
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()
        setDialogBtnBackground(this, customDialog)
    }

    private fun updateUI() {
        val playlist = PlaylistManager.musicPlaylist.ref[currentPlaylistPos]
        binding.playlistTitleTV.text = playlist.name

        // Set the song count, creation date, and creator in one line separated by dots
        val songCount = playlist.playlist.size
        val songCountText = if (songCount == 1) "1 song" else "$songCount songs"
        val infoText = "${playlist.createdBy} • ${playlist.createdOn} • $songCountText"
        binding.playlistInfoTV.text = infoText

        // Set songs title
//        binding.songsTitleTV.text = "Songs"

        // Load album art
        if (playlist.playlist.isNotEmpty()) {
            val firstSongArt = playlist.playlist[0].artUri

            // Load the regular album art
            Glide.with(this)
                .load(firstSongArt)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.playlistImgPD)

            binding.playBtnPD.visibility = View.VISIBLE
        } else {
            binding.playBtnPD.visibility = View.GONE
        }
    }

    private fun showRemoveAllDialog() {
        val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
        builder.setTitle("Remove")
            .setMessage("Do you want to remove all songs from playlist?")
            .setPositiveButton("Yes"){ dialog, _ ->
                PlaylistManager.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
                adapter.refreshPlaylist()
                dialog.dismiss()
                updateUI() // Update UI after removal
            }
            .setNegativeButton("No"){dialog, _ ->
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()
        setDialogBtnBackground(this, customDialog)
    }

    override fun onResume() {
        super.onResume()
        // Update UI every time activity resumes
        updateUI()

        // Save playlist data
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistManager.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
    }
}