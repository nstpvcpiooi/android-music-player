package com.example.musicplayer.fragment

import android.graphics.LinearGradient // Added import
import android.graphics.Shader // Added import
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar

class HomeFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize the toolbar
        toolbar = view.findViewById(R.id.topToolbar)
        setupToolbar()

        initViews(view)

        return view
    }

    private fun setupToolbar() {
        // Set up toolbar menu item clicks
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    // Launch SearchFragment using MainActivity's method
                    (activity as? MainActivity)?.openSearchFragment()
                    true
                }
                else -> false
            }
        }

        // Direct approach - try to find TextView directly
        val appNameTextView = toolbar.findViewById<TextView>(
            toolbar.findViewById<View>(View.generateViewId())?.id ?: -1
        )
        if (appNameTextView != null) {
            applyGradientToTextView(appNameTextView)
        }
    }

    private fun applyGradientToTextView(textView: TextView) {
        val startActualColor = 0xFF16B0E2.toInt()
        val endColor = 0xFF6E5AF0.toInt()

        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            intArrayOf(startActualColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader
        textView.invalidate()
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
