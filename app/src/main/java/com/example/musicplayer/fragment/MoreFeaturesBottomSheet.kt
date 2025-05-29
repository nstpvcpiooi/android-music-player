package com.example.musicplayer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.bold
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.activity.PlayerActivity.Companion.songPosition
import com.example.musicplayer.databinding.DetailsViewBinding
import com.example.musicplayer.databinding.MoreFeaturesBinding
import com.example.musicplayer.onprg.PlayNext
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MoreFeaturesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: MoreFeaturesBinding? = null
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
        _binding = MoreFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentMusic = MainActivity.MusicListMA.find { it.id == musicId } ?: PlayerActivity.musicListPA.find { it.id == musicId }

        binding.moreFeaturesNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.deleteBtn -> {
                    Toast.makeText(context, "Delete clicked for ${currentMusic?.title}", Toast.LENGTH_SHORT).show()
                    // Add your delete logic here using currentMusic
                    dismiss()
                    true
                }
                R.id.shareBtn -> {
                    Toast.makeText(context, "Share clicked for ${currentMusic?.title}", Toast.LENGTH_SHORT).show()
                    if (currentMusic != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this song: ${currentMusic.title} by ${currentMusic.artist}")
                        startActivity(Intent.createChooser(shareIntent, "Share Song"))
                    }
                    dismiss()
                    true
//                    val shareIntent = Intent()
//                    shareIntent.action = Intent.ACTION_SEND
//                    shareIntent.type = "audio/*"
//                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
//                    startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
//                    dismiss()
//                    true
                }
                R.id.AddToPNBtn -> {
                    try {
                        if (PlayNext.playNextList.isEmpty()) {
                            PlayNext.playNextList.add(PlayerActivity.musicListPA[PlayerActivity.songPosition])
                            PlayerActivity.songPosition = 0
                        }

                        if (currentMusic != null) {
                            PlayNext.playNextList.add(currentMusic)
                            PlayerActivity.musicListPA = ArrayList()
                            PlayerActivity.musicListPA.addAll(PlayNext.playNextList)
                            Toast.makeText(requireContext(), "Added to Play Next", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Play A Song First!!", Snackbar.LENGTH_LONG).show()
                    }
                    dismiss()
                    true
                }
                R.id.infoBtn -> {
                    if (currentMusic != null) {
                        val detailsDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.details_view, null)
                        val dialogBinder = DetailsViewBinding.bind(detailsDialogView)

                        val detailsDialog = MaterialAlertDialogBuilder(requireContext())
                            .setView(detailsDialogView)
                            .setPositiveButton("OK") { self, _ -> self.dismiss() }
                            .setCancelable(false)
                            .create()

                        val str = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                            .append(currentMusic.title)
                            .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(currentMusic.duration / 1000))
                            .bold { append("\n\nLocation: ") }.append(currentMusic.path)
                        dialogBinder.detailsTV.text = str
                        detailsDialog.show()
                    } else {
                        Toast.makeText(context, "Song details not available", Toast.LENGTH_SHORT).show()
                    }
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
        const val TAG = "MoreFeaturesBottomSheet"
        private const val ARG_MUSIC_ID = "music_id"

        fun newInstance(musicId: String): MoreFeaturesBottomSheet {
            val args = Bundle()
            args.putString(ARG_MUSIC_ID, musicId)
            val fragment = MoreFeaturesBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }
}
