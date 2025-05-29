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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LibraryFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private var appBarLayout: AppBarLayout? = null
    private lateinit var musicRV: RecyclerView // Declare musicRV as a class member

    // Store the listener instance to correctly remove it
    private val appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        val mainActivity = activity as? MainActivity
        val isToolbarAtTop = verticalOffset == 0
        // Check if musicRV is initialized before using it
        val isListAtTop = if (::musicRV.isInitialized) {
            !musicRV.canScrollVertically(-1)
        } else {
            false // If musicRV isn't ready, assume list is not at top for safety
        }
        mainActivity?.setRefreshLayoutEnabled(isToolbarAtTop && isListAtTop)
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
        musicRV = view.findViewById(R.id.musicRV) // Initialize the class member musicRV

        setupToolbar()
        initViews(view) // initViews will configure the musicRV instance
        // Add the listener
        appBarLayout?.addOnOffsetChangedListener(appBarOffsetChangedListener)
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
        val localMusicRV: RecyclerView = view.findViewById(R.id.musicRV)

        localMusicRV.setHasFixedSize(true)
        localMusicRV.setItemViewCacheSize(13) // Giữ nguyên giá trị này, nó giúp cache nhiều view hơn

        val layoutManager = LinearLayoutManager(requireContext())
        // Tăng số lượng item được prefetch. Giá trị mặc định là 2.
        // Việc này có thể giúp cuộn mượt hơn bằng cách chuẩn bị nhiều view hơn trước khi cần đến.
        // Bạn có thể thử nghiệm với các giá trị khác nhau (ví dụ: 4, 6) để xem giá trị nào phù hợp nhất.
        layoutManager.initialPrefetchItemCount = 4
        localMusicRV.layoutManager = layoutManager

        val mainActivity = activity as? MainActivity
        if (mainActivity != null && MainActivity.MusicListMA.isNotEmpty()) {
            localMusicRV.adapter = mainActivity.musicAdapter
            mainActivity.musicAdapter.updateMusicList(MainActivity.MusicListMA)
        }
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
                    MainActivity.MusicListMA = (activity as MainActivity).getAllAudio()
                    (activity as MainActivity).musicAdapter.updateMusicList(MainActivity.MusicListMA)
                }
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()
    }

    override fun onResume() {
        super.onResume()
        // When the fragment resumes, ensure the SwipeRefreshLayout's state is correctly set.
        // Post this to run after the layout pass, ensuring appBarLayout.top is accurate.
        appBarLayout?.post {
            val mainActivity = activity as? MainActivity
            val isToolbarAtTop = appBarLayout?.top == 0
            // Check if musicRV is initialized before using it
            val isListAtTop = if (::musicRV.isInitialized) {
                !musicRV.canScrollVertically(-1)
            } else {
                false // If musicRV isn't ready, assume list is not at top
            }
            mainActivity?.setRefreshLayoutEnabled(isToolbarAtTop && isListAtTop)
        }
    }

    override fun onPause() {
        super.onPause()
        // When the fragment is paused (not visible), disable SwipeRefreshLayout in MainActivity.
        val mainActivity = activity as? MainActivity
        mainActivity?.setRefreshLayoutEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the specific listener instance
        appBarLayout?.removeOnOffsetChangedListener(appBarOffsetChangedListener)
        appBarLayout = null // Help Garbage Collection
    }
}
