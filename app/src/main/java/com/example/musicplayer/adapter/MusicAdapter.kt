package com.example.musicplayer.adapter

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.model.Music
import com.example.musicplayer.onprg.PlaylistDetails
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter.MyHolder
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.onprg.PlaylistActivity
import com.example.musicplayer.databinding.MusicViewBinding
import com.example.musicplayer.fragment.MoreFeaturesBottomSheet
import com.example.musicplayer.utils.formatDuration


class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>, private val playlistDetails: Boolean = false,
                   private val selectionActivity: Boolean = false)
    : RecyclerView.Adapter<MyHolder>() {

    // Interface for click events
    interface OnMusicItemClickListener {
        fun onSongClicked(position: Int, isSearch: Boolean)
    }

    private var musicItemClickListener: OnMusicItemClickListener? = null

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
    }

    private var onItemClick: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClick = listener
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
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        holder.duration.text = formatDuration(musicList[position].duration)

        val artUri = musicList[position].artUri
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

        // Set OnClickListener for the moreInfoButton
        if (!selectionActivity) {
            holder.moreInfoButton.setOnClickListener {
                showMoreFeaturesBottomSheet(musicList[position].id)
            }
        }

        when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity -> {
                holder.root.setOnClickListener {
                    if (addSong(musicList[position]))
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_pink))
                    else
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
            }
            else -> {
                holder.root.setOnClickListener {
                    // Call the new listener interface method
                    musicItemClickListener?.onSongClicked(position, MainActivity.search)

//                    if (onItemClick != null) {
//                        // khi dùng DownloadActivity đã gán listener => chỉ download
//                        onItemClick!!.invoke(position)
//                    } else {
//                        // hành động mặc định: play nhạc
//                        when {
//                            MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)
//                            musicList[position].id == PlayerActivity.nowPlayingId ->
//                                sendIntent(ref = "NowPlaying", pos = PlayerActivity.songPosition)
//                            else -> sendIntent(ref = "MusicAdapter", pos = position)
//                        }
//                    }
                }
                // Add long-click listener for non-selection and non-playlistDetails cases
                if (!selectionActivity) { // Ensure not in selection mode
                    holder.root.setOnLongClickListener {
                        showMoreFeaturesBottomSheet(musicList[position].id)
                        true // Consume the long click
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateMusicList(searchList: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun sendIntent(ref: String, pos: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    private fun addSong(song: Music): Boolean {
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if (song.id == music.id) {
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }

    fun refreshPlaylist() {
        musicList = ArrayList()
        musicList = PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist
        notifyDataSetChanged()
    }
}

