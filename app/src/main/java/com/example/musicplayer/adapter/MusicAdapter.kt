package com.example.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.ProgressBar // Added import for ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.model.Music
import com.example.musicplayer.activity.PlaylistDetailsActivity
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter.MyHolder
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.MusicViewBinding
import com.example.musicplayer.fragment.MoreFeaturesBottomSheet
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.utils.formatDuration


class MusicAdapter(
    private val context: Context,
    private var musicListToDisplay: ArrayList<Music>, // Renamed from musicList for clarity
    private val playlistDetails: Boolean = false,
    private val selectionActivity: Boolean = false,
    private val currentSelectedSongsForPlaylist: ArrayList<Music>? = null, // Added to hold selected songs in SelectionActivity
    private val isSongDownloadedCallback: ((String) -> Boolean)? = null,
    private val onDownloadClickCallback: ((String) -> Unit)? = null
) : RecyclerView.Adapter<MyHolder>() {

    // Interface for click events
    interface OnMusicItemClickListener {
        fun onSongClicked(position: Int, isSearch: Boolean)
    }

    private var musicItemClickListener: OnMusicItemClickListener? = null
    private var onItemClick: ((Int) -> Unit)? = null
    private var onItemLongClick: ((Int) -> Boolean)? = null

    fun setOnMusicItemClickListener(listener: OnMusicItemClickListener) {
        this.musicItemClickListener = listener
    }

    //anh xa item trong layout
    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val root = binding.root
        val moreInfoButton = binding.moreInfoButtonMV
        val downloadButton: ImageButton? = binding.downloadBtnMV
        val downloadProgressBar: ProgressBar? = binding.downloadProgressBarMV // Added ProgressBar reference
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClick = listener
    }

    fun setOnItemLongClickListener(listener: (Int) -> Boolean) {
        onItemLongClick = listener
    }

    //inflate layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    private fun showMoreFeaturesBottomSheet(musicId: String) {
        if (context is AppCompatActivity) {
            val moreFeaturesSheet = MoreFeaturesBottomSheet.newInstance(musicId)
            moreFeaturesSheet.show(context.supportFragmentManager, MoreFeaturesBottomSheet.TAG)
        } else {
            Toast.makeText(context, "Cannot show more features here", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val currentSongDisplayed = musicListToDisplay[position]
        holder.title.text = currentSongDisplayed.title
        holder.album.text = currentSongDisplayed.album
        holder.duration.text = formatDuration(currentSongDisplayed.duration)

        val artUri = currentSongDisplayed.artUri
        if (artUri.isNullOrEmpty()) {
            holder.image.setImageResource(R.drawable.music_player_icon_slash_screen)
        } else {
            // Có cover riêng, load bằng Glide
            Glide.with(context)
                .load(artUri)
                .apply(RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .error(R.drawable.music_player_icon_slash_screen)
                    .centerCrop()
                ).into(holder.image)
        }

        // Ensure ProgressBar is hidden by default when binding
        holder.downloadProgressBar?.visibility = android.view.View.GONE

        // Handle download button visibility and state if callbacks are provided
        if (holder.downloadButton != null && isSongDownloadedCallback != null && onDownloadClickCallback != null) {
            holder.downloadButton.visibility = android.view.View.VISIBLE // Ensure button is visible
            val isDownloaded = isSongDownloadedCallback.invoke(currentSongDisplayed.id)
            if (isDownloaded) {
                holder.downloadButton.setImageResource(R.drawable.download_icon_filled) // Placeholder
            } else {
                holder.downloadButton.setImageResource(R.drawable.download_icon_outline) // Placeholder
            }
            holder.downloadButton.setOnClickListener {
                onDownloadClickCallback.invoke(currentSongDisplayed.id)
                // The icon update will be handled by notifyItemChanged from the fragment
            }
        } else {
            holder.downloadButton?.visibility = android.view.View.GONE
            holder.downloadProgressBar?.visibility = android.view.View.GONE // Also hide progress bar if download button is gone
        }

        // Set OnClickListener for the moreInfoButton
        if (!selectionActivity) {
            holder.moreInfoButton.setOnClickListener {
                showMoreFeaturesBottomSheet(currentSongDisplayed.id)
            }
        }

        when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity -> {
                // This block is for SelectionActivity
                if (currentSelectedSongsForPlaylist == null) return // Should not happen if used correctly

                // Determine initial selection state by checking against currentSelectedSongsForPlaylist
                var isInitiallySelected = false
                for (selectedSong in currentSelectedSongsForPlaylist) {
                    if (selectedSong.id == currentSongDisplayed.id) {
                        isInitiallySelected = true
                        break
                    }
                }

                // Set initial background based on selection state
                val initialColorAttr = if (isInitiallySelected) {
                    com.google.android.material.R.attr.colorSecondaryContainer
                } else {
                    com.google.android.material.R.attr.colorSurface
                }
                val typedValueInitial = TypedValue()
                context.theme.resolveAttribute(initialColorAttr, typedValueInitial, true)
                holder.root.setBackgroundColor(typedValueInitial.data)

                holder.root.setOnClickListener {
                    val isNowSelectedAfterClick = toggleSongSelection(currentSongDisplayed)
                    val colorAttrOnClick = if (isNowSelectedAfterClick) {
                        com.google.android.material.R.attr.colorSecondaryContainer
                    } else {
                        com.google.android.material.R.attr.colorSurface
                    }
                    val typedValueOnClick = TypedValue()
                    context.theme.resolveAttribute(colorAttrOnClick, typedValueOnClick, true)
                    holder.root.setBackgroundColor(typedValueOnClick.data)
                }
            }
            else -> {
                holder.root.setOnClickListener {
                    // Handle click via both interfaces for compatibility
                    musicItemClickListener?.onSongClicked(position, MainActivity.search)
                    onItemClick?.invoke(position)
                }

                holder.root.setOnLongClickListener {
                    // Try the custom long click listener first
                    if (onItemLongClick != null) {
                        return@setOnLongClickListener onItemLongClick!!.invoke(position)
                    }
                    // Fall back to default behavior if no custom listener
                    showMoreFeaturesBottomSheet(currentSongDisplayed.id)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return musicListToDisplay.size
    }

    fun updateMusicList(searchList: ArrayList<Music>) {
        musicListToDisplay = ArrayList()
        musicListToDisplay.addAll(searchList)
        notifyDataSetChanged()
    }

    // New method to get current displayed list
    fun getCurrentList(): ArrayList<Music> {
        return musicListToDisplay
    }

    private fun sendIntent(ref: String, pos: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    // Renamed from addSong to toggleSongSelection for clarity in SelectionActivity context
    private fun toggleSongSelection(song: Music): Boolean {
        if (currentSelectedSongsForPlaylist == null) return false // Safety check

        var songFound = false
        var indexOfSong = -1
        for ((index, selectedSong) in currentSelectedSongsForPlaylist.withIndex()) {
            if (song.id == selectedSong.id) {
                songFound = true
                indexOfSong = index
                break
            }
        }

        return if (songFound) {
            currentSelectedSongsForPlaylist.removeAt(indexOfSong)
            false // Song was removed, so it's no longer selected
        } else {
            currentSelectedSongsForPlaylist.add(song)
            true // Song was added, so it's now selected
        }
    }

    fun refreshPlaylist() {
        // This method is specific to PlaylistDetails context, not SelectionActivity
        if (playlistDetails && PlaylistDetailsActivity.currentPlaylistPos != -1) {
            musicListToDisplay = ArrayList()
            musicListToDisplay.addAll(PlaylistManager.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist)
            notifyDataSetChanged()
        }
    }
}
