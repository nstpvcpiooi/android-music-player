package com.example.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.model.Music
import com.example.musicplayer.onprg.PlayNext
import com.example.musicplayer.onprg.PlaylistDetails
import com.example.musicplayer.R
import com.google.android.material.snackbar.Snackbar
import com.example.musicplayer.adapter.MusicAdapter.MyHolder
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.onprg.PlaylistActivity
import com.example.musicplayer.databinding.DetailsViewBinding
import com.example.musicplayer.databinding.MusicViewBinding
import com.example.musicplayer.fragment.MoreFeaturesBottomSheet
import com.example.musicplayer.utils.formatDuration
import com.example.musicplayer.utils.setDialogBtnBackground


class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>, private val playlistDetails: Boolean = false,
                   private val selectionActivity: Boolean = false)
    : RecyclerView.Adapter<MyHolder>() {

    //anh xa item trong layout
    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val root = binding.root
    }

    private var onItemClick: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClick = listener
    }

    //inflate layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        holder.duration.text = formatDuration(musicList[position].duration)
        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        //for play next feature
        if(!selectionActivity)
            holder.root.setOnLongClickListener {
                if (context is AppCompatActivity) {
                    // Pass the music ID to the bottom sheet
                    val moreFeaturesSheet = MoreFeaturesBottomSheet.newInstance(musicList[position].id)
                    moreFeaturesSheet.show(context.supportFragmentManager, MoreFeaturesBottomSheet.TAG)
                } else {
                    Toast.makeText(context, "Cannot show more features here", Toast.LENGTH_SHORT).show()
                }

                return@setOnLongClickListener true
            }

        when{
            playlistDetails ->{
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity ->{
                holder.root.setOnClickListener {
                    if(addSong(musicList[position]))
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_pink))
                    else
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))

                }
            }
            else ->{
                holder.root.setOnClickListener {
                    when{
                        MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)
                        musicList[position].id == PlayerActivity.nowPlayingId ->
                            sendIntent(ref = "NowPlaying", pos = PlayerActivity.songPosition)
                        else->sendIntent(ref="MusicAdapter", pos = position) } }
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateMusicList(searchList : ArrayList<Music>){
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }
    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }
    private fun addSong(song: Music): Boolean{
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if(song.id == music.id){
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }
    fun refreshPlaylist(){
        musicList = ArrayList()
        musicList = PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist
        notifyDataSetChanged()
    }
}

