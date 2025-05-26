package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)

        return view
    }

    private fun initViews(view: View) {
        // Initialize RecyclerViews for Recently Played and Recommended sections
        val recentlyPlayedRV: RecyclerView = view.findViewById(R.id.recently_played_rv)
        val recommendedRV: RecyclerView = view.findViewById(R.id.recommended_rv)

        // Set layout managers for horizontal scrolling in Recently Played
        recentlyPlayedRV.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Set layout manager for vertical list in Recommended
        recommendedRV.layoutManager = LinearLayoutManager(context)

        // Load data and set adapters (to be implemented)
        loadRecentlyPlayed(recentlyPlayedRV)
        loadRecommended(recommendedRV)
    }

    private fun loadRecentlyPlayed(recyclerView: RecyclerView) {
        // This will be implemented to load and display recently played songs
        // For now, we'll leave it empty as it will use data from the app's database or shared preferences
    }

    private fun loadRecommended(recyclerView: RecyclerView) {
        // This will be implemented to load and display recommended songs
        // For now, we'll leave it empty as it will use data from the app's music library
    }
}
