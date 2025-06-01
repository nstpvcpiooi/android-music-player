package com.example.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.MusicViewBinding // Changed from ItemMusicBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.onprg.PlayNext

class QueueAdapter(private val context: Context, private var musicList: ArrayList<Music>) : RecyclerView.Adapter<QueueAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = MusicViewBinding.inflate(LayoutInflater.from(context), parent, false) // Changed from ItemMusicBinding
        return MyHolder(binding)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val music = musicList[position]
        holder.title.text = music.title
        holder.album.text = music.album
        holder.duration.text = com.example.musicplayer.utils.formatDuration(music.duration)
        Glide.with(context)
            .load(music.artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "QueueAdapter")
            ContextCompat.startActivity(context, intent, null)
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateQueue(newQueue: ArrayList<Music>) {
        musicList = newQueue
        notifyDataSetChanged()
    }

    inner class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) { // Changed from ItemMusicBinding
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val root = binding.root
    }
}

