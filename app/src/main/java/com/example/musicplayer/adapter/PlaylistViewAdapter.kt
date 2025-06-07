package com.example.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.activity.PlaylistDetailsActivity
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.fragment.PlaylistMoreFeaturesBottomSheet
import com.example.musicplayer.databinding.PlaylistViewBinding
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.utils.PlaylistManager

class PlaylistViewAdapter(private val context: Context, private var playlistList: ArrayList<Playlist>) : RecyclerView.Adapter<PlaylistViewAdapter.MyHolder>() {

    class MyHolder(binding: PlaylistViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.playlistImg
        val name = binding.playlistName
        val root = binding.root
        val songCount = binding.playlistSongCount
        val creator = binding.playlistCreator
        val separator = binding.playlistMetaSeparator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(PlaylistViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val currentPlaylist = playlistList[position]
        if(MainActivity.themeIndex == 4){
            // holder.root.strokeColor = ContextCompat.getColor(context, R.color.white)
        }
        holder.name.text = currentPlaylist.name
        holder.name.isSelected = true
        holder.songCount.text = context.getString(R.string.playlist_song_count_format, currentPlaylist.playlist.size)

        if (currentPlaylist.createdBy.isNotEmpty()) {
            holder.creator.text = currentPlaylist.createdBy
            holder.creator.visibility = View.VISIBLE
            holder.separator.visibility = View.VISIBLE
        } else {
            holder.creator.visibility = View.GONE
            holder.separator.visibility = View.GONE
        }

        holder.root.setOnClickListener {
            val intent = Intent(context, PlaylistDetailsActivity::class.java)
            intent.putExtra("index", position)
            ContextCompat.startActivity(context, intent, null)
        }

        holder.root.setOnLongClickListener {
            val bottomSheet = PlaylistMoreFeaturesBottomSheet.newInstance(position)
            if (context is AppCompatActivity) {
                bottomSheet.show(context.supportFragmentManager, PlaylistMoreFeaturesBottomSheet.TAG)
            }
            true
        }

        if (currentPlaylist.playlist.isNotEmpty() && currentPlaylist.playlist[0].artUri.isNotEmpty()) {
            Glide.with(context)
                .load(currentPlaylist.playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(holder.image)
        } else {
            Glide.with(context).clear(holder.image)
            holder.image.setImageResource(R.drawable.music_player_icon_slash_screen)
        }
    }

    override fun getItemCount(): Int {
        return playlistList.size
    }

    fun refreshPlaylist(){
        playlistList = ArrayList()
        playlistList.addAll(PlaylistManager.musicPlaylist.ref)
        notifyDataSetChanged()
    }
}

