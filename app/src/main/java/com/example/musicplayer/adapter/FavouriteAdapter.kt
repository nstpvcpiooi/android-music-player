package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.model.Music
import com.example.musicplayer.utils.PlayNext
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.musicplayer.databinding.FavouriteViewBinding

class FavouriteAdapter(private val context: Context, private var musicList: ArrayList<Music>, val playNext: Boolean = false) : RecyclerView.Adapter<FavouriteAdapter.MyHolder>() {

    class MyHolder(binding: FavouriteViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.songImgFV
        val name = binding.songNameFV
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(FavouriteViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.name.text = musicList[position].title
        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        //when play next music is clicked
        if(playNext){
            holder.root.setOnClickListener {
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("index", holder.adapterPosition) // Use adapterPosition
                intent.putExtra("class", "PlayNext")
                ContextCompat.startActivity(context, intent, null)
            }
            holder.root.setOnLongClickListener {
                val currentAdapterPosition = holder.adapterPosition
                if (currentAdapterPosition == RecyclerView.NO_POSITION || currentAdapterPosition >= musicList.size) {
                    // Invalid position, perhaps the list was modified concurrently
                    Snackbar.make((context as Activity).findViewById(android.R.id.content) ?: holder.root,
                        "Error: Could not determine song to remove.", Snackbar.LENGTH_SHORT).show()
                    return@setOnLongClickListener true
                }
                val songToRemove = musicList[currentAdapterPosition]

                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove from Play Next")
                    .setMessage("Remove '${songToRemove.title}' from the Play Next queue?")
                    .setPositiveButton("Remove") { dialogInterface, _ ->
                        // Check if trying to remove the currently playing song from the PlayNext queue
                        // Use PlayerActivity.currentPlaylistOrigin to check if player is in "PlayNext" mode
                        if (PlayerActivity.currentPlaylistOrigin == "PlayNext" &&
                            songToRemove.id == PlayerActivity.nowPlayingId) {
                            Snackbar.make((context as Activity).findViewById(android.R.id.content) ?: holder.root,
                                "Cannot remove the currently playing song. Please skip or change songs first.", Snackbar.LENGTH_LONG).show()
                        } else {
                            // Re-fetch position just in case, though less likely to change inside dialog
                            val latestPosition = holder.adapterPosition
                            if (latestPosition != RecyclerView.NO_POSITION && latestPosition < musicList.size && musicList[latestPosition].id == songToRemove.id) {
                                // Remove from the adapter's list (which should be PlayNext.playNextList)
                                musicList.removeAt(latestPosition)
                                notifyItemRemoved(latestPosition)
                                // No need to call notifyItemRangeChanged if only one item is removed and list shrinks
                                // However, if you have specific UI updates that depend on it, you can add it back.
                                // notifyItemRangeChanged(latestPosition, musicList.size - latestPosition)

                                // If PlayerActivity is *currently* playing from the "PlayNext" queue,
                                // we need to update its internal list (musicListPA) and potentially its current songPosition.
                                if (PlayerActivity.currentPlaylistOrigin == "PlayNext") {
                                    val indexInPlayerActivityList = PlayerActivity.musicListPA.indexOfFirst { it.id == songToRemove.id }
                                    if (indexInPlayerActivityList != -1) {
                                        PlayerActivity.musicListPA.removeAt(indexInPlayerActivityList)
                                        // If the removed song was before or at the current playing position,
                                        // decrement the songPosition in PlayerActivity.
                                        if (PlayerActivity.songPosition >= indexInPlayerActivityList) {
                                            // If it was the current song, PlayerActivity's onCompletion or next/prev logic will handle it.
                                            // If it was before, just decrement.
                                            if (PlayerActivity.songPosition > indexInPlayerActivityList) {
                                                PlayerActivity.songPosition--
                                            }
                                            // If PlayerActivity.songPosition == indexInPlayerActivityList (it was the current song)
                                            // and it's removed, PlayerActivity needs to handle this. Often, it might play the next song
                                            // or stop if the list becomes empty. This part might need more robust handling
                                            // in PlayerActivity itself if the currently playing item is removed from its list.
                                        }
                                    }
                                }
                                Snackbar.make((context as Activity).findViewById(android.R.id.content) ?: holder.root,
                                    "'${songToRemove.title}' removed from Play Next.", Snackbar.LENGTH_SHORT).show()
                            } else {
                                Snackbar.make((context as Activity).findViewById(android.R.id.content) ?: holder.root,
                                    "Could not remove song. Queue may have changed or song not found.", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .show()
                true // Consumed long click
            }
        }else{
            holder.root.setOnClickListener {
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("index", position)
                intent.putExtra("class", "FavouriteAdapter")
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFavourites(newList: ArrayList<Music>){
        musicList = ArrayList()
        musicList.addAll(newList)
        notifyDataSetChanged()
    }

}

