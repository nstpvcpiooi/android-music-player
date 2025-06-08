package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.activity.DownloadActivity
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.adapter.PlaylistViewAdapter
import com.example.musicplayer.databinding.AddPlaylistDialogBinding
import com.example.musicplayer.databinding.FragmentPlaylistBinding
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.utils.PlaylistManager
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!
    internal lateinit var adapter: PlaylistViewAdapter
    private lateinit var toolbar: MaterialToolbar
    private var appBarLayout: AppBarLayout? = null

    private val appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        val mainActivity = activity as? MainActivity
        val isToolbarAtTop = verticalOffset == 0
        val isListAtTop = if (_binding != null && binding.playlistRVFragment.adapter != null && binding.playlistRVFragment.adapter!!.itemCount > 0) {
            val rvScrollY = binding.playlistRVFragment.computeVerticalScrollOffset()
            rvScrollY == 0
        } else {
            true
        }
        mainActivity?.setRefreshLayoutEnabled(isToolbarAtTop && isListAtTop)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.playlistFragmentToolbar)
        appBarLayout = view.findViewById(R.id.playlistFragmentAppBarLayout)

        setupToolbar()
        appBarLayout?.addOnOffsetChangedListener(appBarOffsetChangedListener)

        binding.playlistRVFragment.setHasFixedSize(true)
        binding.playlistRVFragment.setItemViewCacheSize(13)
        binding.playlistRVFragment.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = PlaylistViewAdapter(requireContext(), playlistList = PlaylistManager.musicPlaylist.ref)
        binding.playlistRVFragment.adapter = adapter

        if (PlaylistManager.musicPlaylist.ref.isNotEmpty()) {
            binding.instructionPAFragment.visibility = View.GONE
        } else {
            binding.instructionPAFragment.visibility = View.VISIBLE
        }

        binding.playlistFavoritesBtn.setOnClickListener {
            (activity as? MainActivity)?.openFavorites()
        }

        binding.playlistHistoryBtn.setOnClickListener {
            startActivity(Intent(requireContext(), DownloadActivity::class.java))

            Toast.makeText(context, "Download Clicked", Toast.LENGTH_SHORT).show()

        }
    }

    private fun setupToolbar() {
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_playlist -> {
                    customAlertDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun customAlertDialog() {
        val customDialog = LayoutInflater.from(requireContext()).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialog = builder.setView(customDialog)
            .setTitle("Playlist Details")
            .setPositiveButton("ADD") { dialog, _ ->
                val playlistName = binder.playlistName.text
                val createdBy = binder.yourName.text
                if (playlistName != null && createdBy != null) {
                    if (playlistName.isNotEmpty() && createdBy.isNotEmpty()) {
                        addPlaylist(playlistName.toString(), createdBy.toString())
                    }
                }
                dialog.dismiss()
            }.create()
        dialog.show()
        setDialogBtnBackground(requireContext(), dialog)
    }

    private fun addPlaylist(name: String, createdBy: String) {
        var playlistExists = false
        for (i in PlaylistManager.musicPlaylist.ref) {
            if (name == i.name) {
                playlistExists = true
                break
            }
        }
        if (playlistExists) {
            Toast.makeText(requireContext(), "Playlist Exist!!", Toast.LENGTH_SHORT).show()
        } else {
            val tempPlaylist = Playlist()
            tempPlaylist.name = name
            tempPlaylist.playlist = ArrayList()
            tempPlaylist.createdBy = createdBy
            val calendar = Calendar.getInstance().time
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            tempPlaylist.createdOn = sdf.format(calendar)
            PlaylistManager.musicPlaylist.ref.add(tempPlaylist)
            adapter.refreshPlaylist()
            if (PlaylistManager.musicPlaylist.ref.isNotEmpty()) {
                binding.instructionPAFragment.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.refreshPlaylist()
            if (PlaylistManager.musicPlaylist.ref.isNotEmpty()) {
                binding.instructionPAFragment.visibility = View.GONE
            } else {
                binding.instructionPAFragment.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appBarLayout?.removeOnOffsetChangedListener(appBarOffsetChangedListener)
        appBarLayout = null
        _binding = null
    }
}
