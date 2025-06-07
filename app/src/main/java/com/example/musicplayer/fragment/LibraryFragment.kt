package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.ContextCompat
import com.example.musicplayer.adapter.MusicAdapter

class LibraryFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private var appBarLayout: AppBarLayout? = null
    private lateinit var musicRV: RecyclerView
    private lateinit var fabScrollToTop: FloatingActionButton
    private lateinit var musicAdapter: MusicAdapter // Giả sử bạn có biến n

    private val appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        val mainActivity = activity as? MainActivity
        val isToolbarAtTop = verticalOffset == 0
        val isListAtTop = if (::musicRV.isInitialized && musicRV.adapter != null && musicRV.adapter!!.itemCount > 0) {
            !musicRV.canScrollVertically(-1)
        } else {
            true
        }
        mainActivity?.setRefreshLayoutEnabled(isToolbarAtTop && isListAtTop)
    }

    private val fabStateUpdater = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateFabVisibility()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateFabVisibility()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateFabVisibility()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            updateFabVisibility()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.topToolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        musicRV = view.findViewById(R.id.musicRV)
        fabScrollToTop = view.findViewById(R.id.fabScrollToTop)
        fabScrollToTop.hide()

        setupToolbar()
        initViews(view)
        setupFabScrollToTop()
        appBarLayout?.addOnOffsetChangedListener(appBarOffsetChangedListener)
    }

    private fun setupToolbar() {
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    // Launch SearchFragment using MainActivity's method
                    (activity as? MainActivity)?.openSearchFragment()
                    true
                }
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                R.id.action_shuffle -> { // Added case for shuffle button
                    (activity as? MainActivity)?.openShufflePlayer()
                    true
                }
                else -> false
            }
        }
    }
    fun refreshAdapter() {
        if (::musicAdapter.isInitialized) {
            musicAdapter.updateMusicList(MainActivity.MusicListMA)
        }
    }

    private fun initViews(view: View) {
        musicRV.setHasFixedSize(true)
        musicRV.setItemViewCacheSize(13)

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.initialPrefetchItemCount = 4
        musicRV.layoutManager = layoutManager

        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            try {
                musicRV.adapter?.unregisterAdapterDataObserver(fabStateUpdater)
            } catch (e: IllegalStateException) {
            }

            if (MainActivity.MusicListMA.isNotEmpty()) {
                musicRV.adapter = mainActivity.musicAdapter
            } else {
                musicRV.adapter = null
            }
            musicRV.adapter?.registerAdapterDataObserver(fabStateUpdater)
        }
        updateFabVisibility()
    }

    private fun setupFabScrollToTop() {
        fabScrollToTop.setOnClickListener {
            appBarLayout?.setExpanded(true, true)

            val layoutManager = musicRV.layoutManager as? LinearLayoutManager
            val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0

            if (firstVisibleItemPosition < 30) {
                musicRV.smoothScrollToPosition(0)
            } else {
                layoutManager?.scrollToPositionWithOffset(0, 0)
            }
        }

        musicRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isAdded || view == null) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (dy > 10 && fabScrollToTop.visibility != View.VISIBLE) {
                    fabScrollToTop.show()
                } else if (dy < -10 && fabScrollToTop.visibility == View.VISIBLE) {
                    if (firstVisibleItemPosition < 3) {
                        fabScrollToTop.hide()
                    }
                } else if (firstVisibleItemPosition == 0 && fabScrollToTop.visibility == View.VISIBLE) {
                    fabScrollToTop.hide()
                }
            }
        })
    }

    private fun showSortDialog() {
        val menuList = arrayOf("Recently Added", "Song Title", "File Size")
        var currentSort = MainActivity.sortOrder
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Sorting")
            .setSingleChoiceItems(menuList, currentSort) { dialog, which ->
                if (MainActivity.sortOrder != which) {
                    MainActivity.sortOrder = which
                    val editor = requireActivity().getSharedPreferences("SORTING", AppCompatActivity.MODE_PRIVATE).edit()
                    editor.putInt("sortOrder", which)
                    editor.apply()

                    val mainActivity = activity as? MainActivity
                    if (mainActivity != null) {
                        MainActivity.MusicListMA = mainActivity.getAllAudio()
                        mainActivity.musicAdapter.updateMusicList(MainActivity.MusicListMA)
                    }
                }
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()
    }

    private fun updateFabVisibility() {
        if (!isAdded || view == null || !::musicRV.isInitialized || !::fabScrollToTop.isInitialized) {
            return
        }

        musicRV.post {
            if (!isAdded || view == null) return@post

            val layoutManager = musicRV.layoutManager as? LinearLayoutManager
            val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION

            if (firstVisibleItemPosition == RecyclerView.NO_POSITION || musicRV.adapter == null || musicRV.adapter!!.itemCount == 0) {
                fabScrollToTop.hide()
                return@post
            }

            val showFabThreshold = 3
            if (firstVisibleItemPosition >= showFabThreshold) {
                fabScrollToTop.show()
            } else {
                fabScrollToTop.hide()
            }
        }
    }

    /**
     * Gets a color from a theme attribute
     * @param attrRes The attribute resource ID to resolve
     * @return The resolved color
     */
    private fun getColorFromAttr(attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    override fun onResume() {
        super.onResume()

        // Re-register the adapter observer if needed
        if (::musicRV.isInitialized && musicRV.adapter != null) {
            try {
                musicRV.adapter?.unregisterAdapterDataObserver(fabStateUpdater)
            } catch (e: IllegalStateException) {
                // Observer not registered, ignore
            }
            musicRV.adapter?.registerAdapterDataObserver(fabStateUpdater)
        }

        // Update FAB visibility when returning to fragment
        updateFabVisibility()

        // Use app's theme attributes for FAB colors (white background with purple icon)
//        if (::fabScrollToTop.isInitialized) {
//            fabScrollToTop.backgroundTintList = android.content.res.ColorStateList.valueOf(
//                getColorFromAttr(com.google.android.material.R.attr.colorOnPrimary))
//            fabScrollToTop.imageTintList = android.content.res.ColorStateList.valueOf(
//                getColorFromAttr(com.google.android.material.R.attr.colorPrimary))
//        }
    }

    override fun onPause() {
        super.onPause()

        // Unregister the adapter observer to prevent leaks
        if (::musicRV.isInitialized && musicRV.adapter != null) {
            try {
                musicRV.adapter?.unregisterAdapterDataObserver(fabStateUpdater)
            } catch (e: IllegalStateException) {
                // Observer not registered, ignore
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            musicRV.adapter?.unregisterAdapterDataObserver(fabStateUpdater)
        } catch (e: IllegalStateException) {
        }
        appBarLayout?.removeOnOffsetChangedListener(appBarOffsetChangedListener)
        appBarLayout = null
    }
}
