package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.SearchActivity
import com.google.android.material.appbar.MaterialToolbar

class LibraryFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        // Initialize the toolbar
        toolbar = view.findViewById(R.id.topToolbar)
        setupToolbar()

        initViews(view)

        return view
    }

    private fun setupToolbar() {
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    startActivity(Intent(requireContext(), SearchActivity::class.java))
                    true
                }
                else -> false
            }
        }
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
