package com.example.musicplayer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.activity.PlayerActivity.Companion.songPosition
import com.example.musicplayer.databinding.PlayerMoreFeaturesBottomSheetBinding
// Import other necessary classes like Activity, Context, etc.
// For example, if you need to delete a song, you might need access to a ViewModel or a database helper.

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayerMoreFeaturesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: PlayerMoreFeaturesBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var musicId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            musicId = it.getString(ARG_MUSIC_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerMoreFeaturesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playerFeaturesNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.deleteBtnPA -> {
                    // Handle delete action
                    Toast.makeText(context, "Delete clicked for song ID: $musicId", Toast.LENGTH_SHORT).show()
                    // Implement actual delete logic here
                    dismiss()
                    true
                }
                R.id.shareBtnPA -> {

                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.type = "audio/*"
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
                    startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
                    dismiss()
                    true
                }
                R.id.aboutBtnPA -> {
                    // Handle about action
                    Toast.makeText(context, "About clicked for song ID: $musicId", Toast.LENGTH_SHORT).show()
                    // Implement actual about logic here (e.g., show song details, artist info)
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
        const val TAG = "PlayerMoreFeaturesBottomSheet"
        private const val ARG_MUSIC_ID = "music_id"

        fun newInstance(musicId: String): PlayerMoreFeaturesBottomSheet {
            val args = Bundle()
            args.putString(ARG_MUSIC_ID, musicId)
            val fragment = PlayerMoreFeaturesBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }
}

