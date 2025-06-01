package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.QueueItemViewBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.onprg.PlayNext
import java.util.Collections

class QueueAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private val itemTouchHelper: ItemTouchHelper? = null
) : RecyclerView.Adapter<QueueAdapter.MyHolder>() {

    // Track the currently playing song position to highlight it in the queue
    private var currentPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = QueueItemViewBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val music = musicList[position]
        holder.title.text = music.title
        holder.album.text = music.album
        holder.duration.text = com.example.musicplayer.utils.formatDuration(music.duration)
        Glide.with(context)
            .load(music.artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        // Highlight the currently playing song
        if (position == currentPosition) {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.yellow))
        } else {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.white))
        }

        holder.root.setOnClickListener {
            PlayerActivity.musicListPA = ArrayList(PlayNext.playNextList)
            PlayerActivity.songPosition = holder.adapterPosition

            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", holder.adapterPosition)
            intent.putExtra("class", "PlayNext")
            ContextCompat.startActivity(context, intent, null)
        }

        holder.removeButton.setOnClickListener {
            removeItem(holder.adapterPosition)
        }

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateQueue(newQueue: ArrayList<Music>) {
        musicList.clear()
        musicList.addAll(newQueue)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position < 0 || position >= musicList.size) return

        // Check if trying to remove currently playing song
        if (position == PlayerActivity.songPosition &&
            PlayerActivity.currentPlaylistOrigin == "PlayNext") {
            // Cannot remove currently playing song
            return
        }

        musicList.removeAt(position)
        PlayNext.playNextList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, musicList.size)

        // Update PlayerActivity position if necessary
        if (position < PlayerActivity.songPosition &&
            PlayerActivity.currentPlaylistOrigin == "PlayNext") {
            PlayerActivity.songPosition--
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // Update both adapter list and PlayNext list
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(musicList, i, i + 1)
                Collections.swap(PlayNext.playNextList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(musicList, i, i - 1)
                Collections.swap(PlayNext.playNextList, i, i - 1)
            }
        }

        // Update current position if playing from queue
        if (PlayerActivity.currentPlaylistOrigin == "PlayNext") {
            if (PlayerActivity.songPosition == fromPosition) {
                PlayerActivity.songPosition = toPosition
            } else if (PlayerActivity.songPosition in (fromPosition + 1)..toPosition) {
                PlayerActivity.songPosition--
            } else if (PlayerActivity.songPosition in toPosition until fromPosition) {
                PlayerActivity.songPosition++
            }
        }

        // Update current highlight position
        if (currentPosition == fromPosition) {
            currentPosition = toPosition
        }

        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    // Method to get the current music list (used in PlayerActivity when updating the list)
    fun getMusicList(): ArrayList<Music> {
        return ArrayList(musicList)
    }

    // Method to set the current position for highlighting
    fun setCurrentPosition(position: Int) {
        if (position >= 0 && position < musicList.size) {
            currentPosition = position
            notifyDataSetChanged()
        }
    }

    inner class MyHolder(binding: QueueItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameQIV
        val album = binding.songAlbumQIV
        val image = binding.imageQIV
        val duration = binding.songDurationQIV
        val root = binding.root
        val removeButton: View = binding.removeButtonQiv
        val dragHandle: View = binding.dragHandleIconQiv
    }
}

