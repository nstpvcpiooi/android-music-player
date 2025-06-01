package com.example.musicplayer.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.model.Music
import com.example.musicplayer.activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar

class SearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var searchAdapter: MusicAdapter
    private lateinit var toolbar: MaterialToolbar
    private var instanceLastQuery: String? = null // Renamed to avoid confusion, stores query for this instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        MainActivity.search = true // Indicate that a search context is active

        // Clear static search results when a new SearchFragment instance's view is created.
        // This ensures a fresh start when navigating via the search icon.
        // instanceLastQuery is already null for a new instance.
        MainActivity.musicListSearch.clear()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view) // Adapter will use the cleared MainActivity.musicListSearch
        setupToolbar()
        setupSearchView()     // Will set up a clear search view for new instances
    }

    private fun initializeViews(view: View) {
        toolbar = view.findViewById(R.id.search_toolbar)
        searchView = view.findViewById(R.id.search_view)
        recyclerView = view.findViewById(R.id.search_results_rv)
        statusText = view.findViewById(R.id.search_status)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with an empty list (since MainActivity.musicListSearch was cleared in onCreateView)
        searchAdapter = MusicAdapter(requireContext(), ArrayList()) // Use a new empty list for the adapter initially
        if (activity is MainActivity) {
            searchAdapter.setOnMusicItemClickListener(activity as MainActivity)
        }
        recyclerView.adapter = searchAdapter

        // Explicitly set initial UI state
        showStatus("Enter search query")
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                instanceLastQuery = query // Store query for this instance
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                instanceLastQuery = newText // Store query for this instance
                if (!newText.isNullOrEmpty() && newText.length >= 1) {
                    performSearch(newText)
                } else if (newText.isNullOrEmpty()) {
                    MainActivity.musicListSearch.clear() // Clear static list
                    searchAdapter.updateMusicList(ArrayList())
                    showStatus("Enter search query")
                    instanceLastQuery = null // Clear instance query too
                }
                return true
            }
        })
        searchView.requestFocus()

        // Restore instanceLastQuery if this fragment instance is being restored (e.g., from backstack or config change)
        if (!instanceLastQuery.isNullOrEmpty()) {
            searchView.setQuery(instanceLastQuery, true) // Set query and perform search
        } else {
            searchView.setQuery("", false) // Ensure search view is empty for new/reset instances
            // MainActivity.musicListSearch was cleared in onCreateView, adapter initialized empty.
            // Status text set in initializeViews.
        }
    }

    private fun performSearch(query: String?) {
        instanceLastQuery = query // Update this instance's last known query
        if (query.isNullOrEmpty()) {
            MainActivity.musicListSearch.clear()
            searchAdapter.updateMusicList(ArrayList())
            showStatus("Enter search query")
            return
        }

        val filteredList = ArrayList<Music>()
        val originalList = MainActivity.MusicListMA

        for (song in originalList) {
            if (song.title.lowercase().contains(query.lowercase()) ||
                song.album.lowercase().contains(query.lowercase()) ||
                song.artist.lowercase().contains(query.lowercase())) {
                filteredList.add(song)
            }
        }
        MainActivity.musicListSearch = filteredList // Update the static list for other components if needed
        updateSearchResults(filteredList, query)
    }

    private fun updateSearchResults(results: ArrayList<Music>, query: String?) {
        searchAdapter.updateMusicList(results) // Update adapter for this instance
        if (results.isEmpty()) {
            showStatus("No results found for \"$query\"")
        } else {
            statusText.visibility = View.GONE
        }
    }

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // When resuming an existing instance, its state (instanceLastQuery, searchView text)
        // should be as it was. setupSearchView handles restoring the query if instanceLastQuery is set.
        // If it's a new instance, instanceLastQuery is null, and onCreateView has cleared static state.

        // Request focus and show keyboard
        searchView.post {
            searchView.requestFocus()
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MainActivity.search = false // Reset global search flag
        // instanceLastQuery will be gone when this instance is fully destroyed.
        // MainActivity.musicListSearch will hold the results of the last search from this instance
        // until a new SearchFragment instance clears it in its onCreateView.
    }
}

