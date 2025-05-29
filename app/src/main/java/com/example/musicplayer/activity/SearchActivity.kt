package com.example.musicplayer.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.model.Music
import com.google.android.material.appbar.MaterialToolbar

class SearchActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var searchAdapter: MusicAdapter
    private var musicList: ArrayList<Music> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_view_layout)

        MainActivity.search = true // Indicate that a search context is active

        initializeViews()
        setupToolbar()
        setupSearchView()
    }

    private fun initializeViews() {
        searchView = findViewById(R.id.search_view)
        recyclerView = findViewById(R.id.search_results_rv)
        statusText = findViewById(R.id.search_status)

        // Setup RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize with empty list
        searchAdapter = MusicAdapter(this, ArrayList())
        recyclerView.adapter = searchAdapter
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.search_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty() && newText.length >= 2) {
                    performSearch(newText)
                } else if (newText.isNullOrEmpty()) {
                    MainActivity.musicListSearch.clear() // Clear the global search list
                    searchAdapter.updateMusicList(ArrayList())
                    showStatus("Enter search query")
                }
                return true
            }
        })

        // Set focus on the search view when activity opens
        searchView.requestFocus()
    }

    private fun performSearch(query: String?) {
        if (query.isNullOrEmpty()) {
            MainActivity.musicListSearch.clear() // Clear the global search list
            searchAdapter.updateMusicList(ArrayList())
            showStatus("Enter search query")
            return
        }

        val filteredList = ArrayList<Music>()

        // Get original music list from MainActivity
        val originalList = MainActivity.MusicListMA

        // Filter songs by title, artist, album
        for (song in originalList) {
            if (song.title.lowercase().contains(query.lowercase()) ||
                song.album.lowercase().contains(query.lowercase()) ||
                song.artist.lowercase().contains(query.lowercase())) {
                filteredList.add(song)
            }
        }
        MainActivity.musicListSearch = filteredList // Update the global search list for PlayerActivity
        updateSearchResults(filteredList, query)
    }

    private fun updateSearchResults(results: ArrayList<Music>, query: String?) {
        if (results.isEmpty()) {
            showStatus("No results found for \"$query\"")
        } else {
            statusText.visibility = View.GONE
            searchAdapter.updateMusicList(results)
        }
    }

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.search = false // Reset search flag when activity is destroyed
        // It's generally safer not to clear MainActivity.musicListSearch here,
        // as PlayerActivity might still be using its copy if a song was just played from search.
        // PlayerActivity's musicListPA holds its own copy of the list.
    }
}
