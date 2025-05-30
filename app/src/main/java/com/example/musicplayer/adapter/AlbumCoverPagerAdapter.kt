package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.model.Music
import com.google.android.material.imageview.ShapeableImageView

class AlbumCoverPagerAdapter(private var musicList: ArrayList<Music>) :
    RecyclerView.Adapter<AlbumCoverPagerAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val motionLayout: MotionLayout = itemView as MotionLayout
        val coverImageView: ShapeableImageView = itemView.findViewById(R.id.albumCoverImageView)
        val blurImageView: ShapeableImageView = itemView.findViewById(R.id.albumBlurImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_cover, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val music = musicList[position]

        // Load main cover image
        Glide.with(holder.itemView.context)
            .load(music.artUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.music_player_icon_slash_screen)
                .centerCrop())
            .into(holder.coverImageView)

        // Load blurred background version with lighter processing
        Glide.with(holder.itemView.context)
            .load(music.artUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.music_player_icon_slash_screen)
                .transform(CenterCrop()))
            .into(holder.blurImageView)

        // Reset motion layout to middle position (default state)
        holder.motionLayout.progress = 0.5f
    }

    override fun getItemCount(): Int = musicList.size

    fun updateMusicList(newList: ArrayList<Music>) {
        musicList = newList
        notifyDataSetChanged()
    }
}
