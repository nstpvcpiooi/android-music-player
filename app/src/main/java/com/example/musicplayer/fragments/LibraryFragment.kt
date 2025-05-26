package com.example.musicplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.AccountActivity
import com.example.musicplayer.activity.DownloadActivity
import com.example.musicplayer.activity.MainActivity

class LibraryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        initViews(view)

        return view
    }

    private fun initViews(view: View) {
        // Initialize RecyclerView
        val musicRV: RecyclerView = view.findViewById(R.id.musicRV)

        // Set layout manager
        musicRV.setHasFixedSize(true)
        musicRV.setItemViewCacheSize(13)
        musicRV.layoutManager = LinearLayoutManager(requireContext())

        // Get adapter from MainActivity and set it to RecyclerView
        val mainActivity = activity as? MainActivity
        if (mainActivity != null && MainActivity.MusicListMA.isNotEmpty()) {
            musicRV.adapter = mainActivity.musicAdapter
        }

        // Setup buttons
        view.findViewById<View>(R.id.shuffleBtn).setOnClickListener {
            (activity as? MainActivity)?.openShufflePlayer()
        }

        view.findViewById<View>(R.id.favouriteBtn).setOnClickListener {
            (activity as? MainActivity)?.openFavorites()
        }

        view.findViewById<View>(R.id.playlistBtn).setOnClickListener {
            (activity as? MainActivity)?.openPlaylist()
        }

        view.findViewById<View>(R.id.playNextBtn).setOnClickListener {
            (activity as? MainActivity)?.openPlayNext()
        }

//        // Setup account and download buttons
//        view.findViewById<View>(R.id.accountButton).setOnClickListener {
//            startActivity(Intent(requireContext(), AccountActivity::class.java))
//        }
//
//        view.findViewById<View>(R.id.downloadButton).setOnClickListener {
//            startActivity(Intent(requireContext(), DownloadActivity::class.java))
//        }
    }
}
