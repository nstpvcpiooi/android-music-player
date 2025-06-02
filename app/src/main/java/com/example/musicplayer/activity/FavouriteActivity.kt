package com.example.musicplayer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.model.Music
import com.example.musicplayer.databinding.ActivityFavouriteBinding
import com.example.musicplayer.utils.checkPlaylist

class FavouriteActivity : AppCompatActivity(), MusicAdapter.OnMusicItemClickListener {

    private lateinit var binding: ActivityFavouriteBinding
    private lateinit var adapter: MusicAdapter

    companion object {
        var favouriteSongs: ArrayList<Music> = ArrayList()
        var favouritesChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbarFA)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarFA.setNavigationOnClickListener {
            finish()
        }

        favouriteSongs = checkPlaylist(favouriteSongs)
        binding.favouriteRV.setHasFixedSize(true)
        binding.favouriteRV.setItemViewCacheSize(13)
        binding.favouriteRV.layoutManager = LinearLayoutManager(this)
        adapter = MusicAdapter(this, ArrayList(favouriteSongs), selectionActivity = false, playlistDetails = false)
        adapter.setOnMusicItemClickListener(this)
        binding.favouriteRV.adapter = adapter

        favouritesChanged = false

        updateUIElements()

        binding.playAllFavouritesBtn.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouritePlayAll")
            PlayerActivity.musicListPA = ArrayList(favouriteSongs)
            startActivity(intent)
        }

        binding.shuffleFavouritesImageView.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouriteShuffle")
            PlayerActivity.musicListPA = ArrayList(favouriteSongs)
            startActivity(intent)
        }
    }

    override fun onSongClicked(position: Int, isSearch: Boolean) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("index", position)
        intent.putExtra("class", "FavouriteSongClick")
        PlayerActivity.musicListPA = ArrayList(favouriteSongs)
        PlayerActivity.songPosition = position
        if (position >= 0 && position < favouriteSongs.size) {
            PlayerActivity.nowPlayingId = favouriteSongs[position].id
        }
        startActivity(intent)
    }

    private fun updateUIElements() {
        binding.favouritesSongCountTV.text = "${favouriteSongs.size} songs"
        if (favouriteSongs.size < 1) {
            binding.playAllFavouritesBtn.visibility = View.GONE
            binding.shuffleFavouritesImageView.visibility = View.GONE
            binding.instructionFV.visibility = View.VISIBLE
        } else {
            binding.playAllFavouritesBtn.visibility = View.VISIBLE
            binding.shuffleFavouritesImageView.visibility = View.VISIBLE
            binding.instructionFV.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (favouritesChanged) {
            favouriteSongs = checkPlaylist(favouriteSongs)
            adapter.updateMusicList(ArrayList(favouriteSongs))
            updateUIElements()
            favouritesChanged = false
        }
    }
}

