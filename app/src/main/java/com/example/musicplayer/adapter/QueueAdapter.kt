package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.QueueItemViewBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.utils.PlayNext
import java.util.Collections

class QueueAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private val itemTouchHelper: ItemTouchHelper? = null
) : RecyclerView.Adapter<QueueAdapter.MyHolder>() {

    // Track the currently playing song position to highlight it in the queue
    private var currentPosition: Int = -1

    // Interface for queue updates
    interface QueueUpdateListener {
        fun onQueueUpdated(fromPosition: Int = -1, toPosition: Int = -1, isRemoval: Boolean = false, removedPosition: Int = -1)
    }

    private var queueUpdateListener: QueueUpdateListener? = null

    fun setQueueUpdateListener(listener: QueueUpdateListener) {
        queueUpdateListener = listener
    }

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
            // Clicking on a queue item will play it
            if (position != currentPosition) { // Only change if clicking a different song
                // Keep the song ID reference to identify the currently playing song
                val currentSongId = if (currentPosition >= 0 && currentPosition < musicList.size)
                    musicList[currentPosition].id else ""

                // Update player lists and position
                PlayerActivity.musicListPA = ArrayList(musicList)
                PlayerActivity.songPosition = holder.adapterPosition
                PlayerActivity.currentPlaylistOrigin = "PlayNext"

                // Ensure PlayNext list stays in sync
                PlayNext.playNextList = ArrayList(musicList)

                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("index", holder.adapterPosition)
                intent.putExtra("class", "PlayNext")
                ContextCompat.startActivity(context, intent, null)
            }
        }

        holder.removeButton.setOnClickListener {
            removeItem(holder.adapterPosition)
        }

        // This is crucial for drag functionality - enable drag when touching the drag handle
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                // Start drag when the user touches the drag handle
                itemTouchHelper?.startDrag(holder)
                return@setOnTouchListener true
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

        // Keep the song ID reference
        val currentSongId = if (currentPosition >= 0 && currentPosition < musicList.size)
            musicList[currentPosition].id else ""

        // Get the song being removed for better error messages
        val songToRemove = musicList[position]

        // Check if trying to remove currently playing song
        if (position == currentPosition) {
            Toast.makeText(context,
                "Cannot remove currently playing song",
                Toast.LENGTH_SHORT).show()
            return
        }

        // Store the removed position for proper notification
        val removedPosition = position

        // Remove from the adapter's list
        musicList.removeAt(position)

        // Remove from the global PlayNext list
        if (position < PlayNext.playNextList.size) {
            PlayNext.playNextList.removeAt(position)
        }

        // After removal, need to update the currentPosition to reflect the new location
        // of the currently playing song in the updated list, if the removed song was before it
        if (position < currentPosition) {
            currentPosition--
        }

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, musicList.size)

        // Notify listener about removal with specific information
        queueUpdateListener?.onQueueUpdated(isRemoval = true, removedPosition = removedPosition)

        Toast.makeText(context,
            "Removed from queue",
            Toast.LENGTH_SHORT).show()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 ||
            fromPosition >= musicList.size || toPosition >= musicList.size) {
            return false
        }

        // Get the currently playing song's ID before the move to track it
        val currentSongId = if (currentPosition >= 0 && currentPosition < musicList.size)
            musicList[currentPosition].id else ""

        // Update the adapter's list with swap
        Collections.swap(musicList, fromPosition, toPosition)

        // Update the global PlayNext list
        if (fromPosition < PlayNext.playNextList.size && toPosition < PlayNext.playNextList.size) {
            Collections.swap(PlayNext.playNextList, fromPosition, toPosition)
        } else {
            // Ensure PlayNext.playNextList matches musicList
            PlayNext.playNextList = ArrayList(musicList)
        }

        // Update current highlighted position if the move affected it
        if (currentPosition == fromPosition) {
            // The song being moved is the currently playing song
            currentPosition = toPosition
        } else if (currentPosition in (fromPosition + 1)..toPosition) {
            // Moving song from above current to below - current position shifts up
            currentPosition--
        } else if (currentPosition in toPosition until fromPosition) {
            // Moving song from below current to above - current position shifts down
            currentPosition++
        }

        notifyItemMoved(fromPosition, toPosition)

        // Notify listener about the move with positions
        queueUpdateListener?.onQueueUpdated(fromPosition, toPosition)

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

    // Get the current position for external reference
    fun getCurrentPosition(): Int {
        return currentPosition
    }

    inner class MyHolder(binding: QueueItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameQIV
        val album = binding.songAlbumQIV
        val image = binding.imageQIV
        val duration = binding.songDurationQIV
        val root = binding.root
        val removeButton = binding.removeButtonQiv
        val dragHandle = binding.dragHandleIconQiv
    }
}

