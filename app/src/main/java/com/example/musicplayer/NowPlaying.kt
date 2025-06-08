package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireContext().theme.applyStyle(MainActivity.currentTheme[MainActivity.themeIndex], true)
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.INVISIBLE

        binding.playPauseBtnNP.setOnClickListener {
            PlayerActivity.musicService?.handlePlayPause()
        }
        binding.nextBtnNP.setOnClickListener {
            PlayerActivity.musicService?.prevNextSong(increment = true, context = requireContext())
        }
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("index", PlayerActivity.songPosition)
            intent.putExtra("class", "NowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        refreshUIContent()
    }

    fun refreshUIContent() {
        if (!isAdded || view == null) {
            return
        }

        if (PlayerActivity.musicService != null && PlayerActivity.musicListPA.isNotEmpty() &&
            PlayerActivity.songPosition >= 0 && PlayerActivity.songPosition < PlayerActivity.musicListPA.size) {
            binding.root.visibility = View.VISIBLE

            binding.songNameNP.isSelected = true
            Glide.with(requireContext())
                .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
            if (PlayerActivity.isPlaying) {
                binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
            } else {
                binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
            }
        } else {
            binding.root.visibility = View.INVISIBLE
        }
    }
}
