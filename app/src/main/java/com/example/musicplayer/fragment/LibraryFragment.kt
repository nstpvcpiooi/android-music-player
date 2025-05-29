package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.SearchActivity
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
                R.id.action_sort -> {
                    showSortDialog()
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
            // Refresh the list after sorting
            mainActivity.musicAdapter.updateMusicList(MainActivity.MusicListMA)
        }
    }

    private fun showSortDialog() {
        val menuList = arrayOf("Recently Added", "Song Title", "File Size")
        var currentSort = MainActivity.sortOrder // Keep track of the initial sort order
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Sorting")
            // No positive button needed, action happens on item selection
            .setSingleChoiceItems(menuList, currentSort) { dialog, which ->
                if (MainActivity.sortOrder != which) { // Only update if the sort order actually changed
                    MainActivity.sortOrder = which // Update the sort order in MainActivity
                    val editor = requireActivity().getSharedPreferences("SORTING", AppCompatActivity.MODE_PRIVATE).edit()
                    editor.putInt("sortOrder", which)
                    editor.apply()
                    // Refresh music list and adapter
                    MainActivity.MusicListMA = (activity as MainActivity).getAllAudio()
                    (activity as MainActivity).musicAdapter.updateMusicList(MainActivity.MusicListMA)
                }
                dialog.dismiss() // Dismiss the dialog after selection
            }
        val customDialog = builder.create()
        customDialog.show()
        // setDialogBtnBackground(requireContext(), customDialog) // This might not be needed or might cause issues if there are no buttons
    }
}
