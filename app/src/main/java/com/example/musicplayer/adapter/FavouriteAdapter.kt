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
import com.example.musicplayer.onprg.PlayNext
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
                intent.putExtra("index", position)
                intent.putExtra("class", "PlayNext")
                ContextCompat.startActivity(context, intent, null)
            }
            holder.root.setOnLongClickListener {
                // Replace the old dialog logic with MaterialAlertDialogBuilder
                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove from Play Next")
                    .setMessage("Remove '${musicList[position].title}' from the Play Next queue?")
                    .setPositiveButton("Remove") { dialogInterface, _ ->
                        if (position == PlayerActivity.songPosition && PlayerActivity.musicListPA.isNotEmpty() && musicList[position].id == PlayerActivity.musicListPA[PlayerActivity.songPosition].id) {
                            Snackbar.make((context as Activity).findViewById(R.id.linearLayoutPN) ?: holder.root,
                                "Can't Remove Currently Playing Song.", Snackbar.LENGTH_SHORT).show()
                        } else {
                            val removedSongId = musicList[position].id
                            PlayNext.playNextList.removeAt(position)
                            // Also remove from PlayerActivity.musicListPA if it's being used as the source for PlayNext
                            // and ensure songPosition is updated correctly.
                            val indexInPlayerActivityList = PlayerActivity.musicListPA.indexOfFirst { it.id == removedSongId }
                            if (indexInPlayerActivityList != -1) {
                                PlayerActivity.musicListPA.removeAt(indexInPlayerActivityList)
                                if (PlayerActivity.songPosition > indexInPlayerActivityList) {
                                    PlayerActivity.songPosition--
                                } else if (PlayerActivity.songPosition == indexInPlayerActivityList) {
                                    // This case should ideally be handled by the check above.
                                    // If current song is removed, PlayerActivity needs to handle this,
                                    // e.g., play next, stop, or reset songPosition.
                                    // For now, we assume the Snackbar prevents this or PlayerActivity handles it.
                                }
                            }
                            notifyItemRemoved(position)
                            // Notify adapter about data change for items that shift position
                            if (position < musicList.size) { // Check if position is still valid after removal
                                notifyItemRangeChanged(position, musicList.size - position)
                            }
                        }
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .show()
                return@setOnLongClickListener true
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

