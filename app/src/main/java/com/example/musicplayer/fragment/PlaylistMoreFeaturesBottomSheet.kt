package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.LayoutPlaylistMoreFeaturesBottomSheetBinding
import com.example.musicplayer.utils.PlaylistManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistMoreFeaturesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutPlaylistMoreFeaturesBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var playlistPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistPosition = it.getInt(ARG_PLAYLIST_POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutPlaylistMoreFeaturesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlist = PlaylistManager.musicPlaylist.ref.getOrNull(playlistPosition)
        if (playlist == null) {
            Toast.makeText(context, "Playlist not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Set delete playlist text color to red
        val deleteMenuItem = binding.playlistMoreFeaturesNavigationView.menu.findItem(R.id.playlist_delete_playlist)
        val title = SpannableString(deleteMenuItem.title)
//        title.setSpan(ForegroundColorSpan(Color.RED), 0, title.length, 0)
        deleteMenuItem.title = title

        binding.playlistMoreFeaturesNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.playlist_play -> {
                    if (playlist.playlist.isNotEmpty()) {
                        val intent = Intent(requireContext(), PlayerActivity::class.java)
                        intent.putExtra("index", 0)
                        intent.putExtra("class", "PlaylistDetails") // Simulate playing from details
                        PlayerActivity.musicListPA = ArrayList(playlist.playlist)
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                    true
                }
                R.id.playlist_add_to_queue -> {
                    // Similar to AddToPNBtn in MoreFeaturesBottomSheet, adapt for whole playlist
                    if (playlist.playlist.isNotEmpty()) {
                        PlayerActivity.musicListPA.addAll(playlist.playlist)
                        Toast.makeText(context, "Added ${playlist.name} to queue", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                    true
                }
                R.id.playlist_shuffle -> {
                    if (playlist.playlist.isNotEmpty()) {
                        val intent = Intent(requireContext(), PlayerActivity::class.java)
                        intent.putExtra("index", 0)
                        intent.putExtra("class", "PlaylistDetailsShuffle")
                        PlayerActivity.musicListPA = ArrayList(playlist.playlist)
                        // PlayerActivity will handle shuffle logic based on "PlaylistDetailsShuffle"
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                    true
                }
                R.id.playlist_delete_playlist -> {
                    PlaylistManager.musicPlaylist.ref.removeAt(playlistPosition)

                    // Attempt to refresh PlaylistFragment if it's the current fragment in MainActivity
                    val mainActivity = activity as? com.example.musicplayer.activity.MainActivity
                    if (mainActivity != null) {
                        val currentFragmentInMain = mainActivity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                        if (currentFragmentInMain is PlaylistFragment) {
                            currentFragmentInMain.adapter.refreshPlaylist()
                        }
                    }

                    Toast.makeText(context, "Deleted playlist: ${playlist.name}", Toast.LENGTH_SHORT).show()
                    dismiss()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PlaylistMoreFeaturesBottomSheet"
        private const val ARG_PLAYLIST_POSITION = "playlist_position"

        fun newInstance(playlistPosition: Int): PlaylistMoreFeaturesBottomSheet {
            val args = Bundle()
            args.putInt(ARG_PLAYLIST_POSITION, playlistPosition)
            val fragment = PlaylistMoreFeaturesBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }
}

