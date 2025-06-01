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
import com.example.musicplayer.onprg.PlayNext
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
        fun onQueueUpdated()
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
            // Update the playlist to the current queue
            PlayerActivity.musicListPA = ArrayList(musicList)
            PlayerActivity.songPosition = holder.adapterPosition
            PlayerActivity.currentPlaylistOrigin = "PlayNext"

            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", holder.adapterPosition)
            intent.putExtra("class", "PlayNext")
            ContextCompat.startActivity(context, intent, null)
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

        // Get the song being removed for better error messages
        val songToRemove = musicList[position]

        // Check if trying to remove currently playing song
        if (PlayerActivity.currentPlaylistOrigin == "PlayNext" &&
            position == currentPosition) {
            Toast.makeText(context,
                "Cannot remove currently playing song",
                Toast.LENGTH_SHORT).show()
            return
        }

        // Remove from the adapter's list
        musicList.removeAt(position)

        // Remove from the global PlayNext list
        if (position < PlayNext.playNextList.size) {
            PlayNext.playNextList.removeAt(position)
        }

        // Update player lists if playing from queue
        if (PlayerActivity.currentPlaylistOrigin == "PlayNext") {
            // If song removed was before current position, update the position
            if (position < PlayerActivity.songPosition) {
                PlayerActivity.songPosition--
            }

            // Update the player's list to match the queue
            PlayerActivity.musicListPA = ArrayList(musicList)
        }

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, musicList.size)

        // Notify any listeners about the queue update
        queueUpdateListener?.onQueueUpdated()

        Toast.makeText(context,
            "Removed from queue",
            Toast.LENGTH_SHORT).show()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 ||
            fromPosition >= musicList.size || toPosition >= musicList.size) {
            return false
        }

        // Update the adapter's list with swap
        Collections.swap(musicList, fromPosition, toPosition)

        // Update the global PlayNext list
        if (fromPosition < PlayNext.playNextList.size && toPosition < PlayNext.playNextList.size) {
            Collections.swap(PlayNext.playNextList, fromPosition, toPosition)
        } else {
            // Ensure PlayNext.playNextList matches musicList
            PlayNext.playNextList = ArrayList(musicList)
        }

        // Update player position if needed
        if (PlayerActivity.currentPlaylistOrigin == "PlayNext") {
            if (PlayerActivity.songPosition == fromPosition) {
                // If moving the currently playing song
                PlayerActivity.songPosition = toPosition
            } else if (PlayerActivity.songPosition in (fromPosition + 1)..toPosition) {
                // If moving a song from before the current song to after it
                PlayerActivity.songPosition--
            } else if (PlayerActivity.songPosition in toPosition until fromPosition) {
                // If moving a song from after the current song to before it
                PlayerActivity.songPosition++
            }
        }

        // Update current highlighted position
        if (currentPosition == fromPosition) {
            currentPosition = toPosition
        }

        notifyItemMoved(fromPosition, toPosition)

        // Notify any listeners about the queue update
        queueUpdateListener?.onQueueUpdated()

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
        val removeButton = binding.removeButtonQiv
        val dragHandle = binding.dragHandleIconQiv
    }
}

