package com.example.musicplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.adapter.FavouriteAdapter
import com.example.musicplayer.databinding.ActivityPlayNextBinding
import com.example.musicplayer.model.Music

class PlayNext : AppCompatActivity() {

    companion object{
        var playNextList: ArrayList<Music> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        val binding = ActivityPlayNextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playNextRV.setHasFixedSize(true)
        binding.playNextRV.setItemViewCacheSize(13)
        binding.playNextRV.layoutManager = GridLayoutManager(this, 4)
        binding.playNextRV.adapter = FavouriteAdapter(this, playNextList, playNext = true)

        if(playNextList.isNotEmpty())
            binding.instructionPN.visibility = View.GONE

        binding.backBtnPN.setOnClickListener {
            finish()
        }
    }
}