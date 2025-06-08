package com.example.musicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.databinding.QueueRecommendedItemBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.utils.formatDuration

class RecommendedSongsAdapter(
    private val context: Context,
    private var songsList: ArrayList<Music>
) : RecyclerView.Adapter<RecommendedSongsAdapter.MyHolder>() {

    interface OnRecommendedItemClickListener {
        fun onAddToQueueClick(position: Int, song: Music)
    }

    private var clickListener: OnRecommendedItemClickListener? = null

    fun setOnRecommendedItemClickListener(listener: OnRecommendedItemClickListener) {
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = QueueRecommendedItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyHolder(binding)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val song = songsList[position]
        holder.bind(song)

        // Add song to queue button click
        holder.binding.addButtonRecommended.setOnClickListener {
            clickListener?.onAddToQueueClick(position, song)
        }

        // Item click also adds to queue
        holder.binding.root.setOnClickListener {
            clickListener?.onAddToQueueClick(position, song)
        }
    }

    override fun getItemCount(): Int = songsList.size

    fun updateList(newList: ArrayList<Music>) {
        songsList = ArrayList(newList)
        notifyDataSetChanged()
    }

    inner class MyHolder(val binding: QueueRecommendedItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Music) {
            // Set song details
            binding.songNameRecommended.text = song.title
            binding.songAlbumRecommended.text = song.album
            binding.songDurationRecommended.text = formatDuration(song.duration)

            // Load album art
            Glide.with(context)
                .load(song.artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.imageRecommended)
        }
    }
}
