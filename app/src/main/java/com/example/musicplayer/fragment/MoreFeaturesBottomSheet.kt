package com.example.musicplayer.fragment

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.format.DateUtils
import android.text.style.MetricAffectingSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.activity.PlayerActivity.Companion.songPosition
import com.example.musicplayer.databinding.DetailsViewBinding
import com.example.musicplayer.databinding.MoreFeaturesBinding
import com.example.musicplayer.utils.PlayNext
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
                }
                R.id.AddToPNBtn -> {
                    try {
                        if (PlayNext.playNextList.isEmpty()) {
                            PlayNext.playNextList.add(musicListPA[songPosition])
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
                R.id.addToQueueBtn -> {
                    try {
                        if (PlayerActivity.musicService == null || PlayerActivity.musicListPA.isEmpty()) {
                            Snackbar.make(binding.root, "Play a song first!", Snackbar.LENGTH_LONG).show()
                        } else if (currentMusic != null) {
                            // If the PlayNext list is empty, first add the currently playing song
                            if (PlayNext.playNextList.isEmpty()) {
                                PlayNext.playNextList.add(musicListPA[songPosition])
                            }

                            // Add song to queue right after the current song
                            val currentSongIndex = PlayNext.playNextList.indexOfFirst {
                                it.id == musicListPA[songPosition].id
                            }

                            if (currentSongIndex != -1) {
                                // Add the selected song right after the currently playing song
                                PlayNext.playNextList.add(currentSongIndex + 1, currentMusic)
                                // Update the player's music list to reflect this change
                                PlayerActivity.musicListPA = ArrayList(PlayNext.playNextList)

                                Toast.makeText(requireContext(),
                                    "\"${currentMusic.title}\" will play after current song",
                                    Toast.LENGTH_SHORT).show()
                            } else {
                                // If we couldn't find the current song in the queue (shouldn't happen)
                                PlayNext.playNextList.add(currentMusic)
                                PlayerActivity.musicListPA = ArrayList(PlayNext.playNextList)
                                Toast.makeText(requireContext(), "Added to Queue", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding to queue", e)
                        Snackbar.make(binding.root, "Error adding to queue", Snackbar.LENGTH_LONG).show()
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

        // Apply custom font to NavigationView items
        try {
            val menu = binding.moreFeaturesNavigationView.menu
            val typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_medium)
            if (typeface != null) {
                for (i in 0 until menu.size()) {
                    val menuItem = menu.getItem(i)
                    applyFontToMenuItem(menuItem, typeface)
                }
            } else {
                Log.w(TAG, "Typeface R.font.inter_semibold not loaded.")
                Toast.makeText(context, "Failed to load font resource", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying custom font to navigation menu.", e)
            Toast.makeText(context, "Error applying font", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFontToMenuItem(menuItem: android.view.MenuItem, typeface: Typeface) {
        val title = menuItem.title
        if (title != null) {
            val spannableString = SpannableString(title)
            spannableString.setSpan(CustomTypefaceSpan(typeface), 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            menuItem.title = spannableString
        }

        if (menuItem.hasSubMenu()) {
            val subMenu = menuItem.subMenu
            if (subMenu != null) {
                for (j in 0 until subMenu.size()) {
                    applyFontToMenuItem(subMenu.getItem(j), typeface)
                }
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

    private inner class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(ds: TextPaint?) {
            ds?.typeface = typeface
        }

        override fun updateMeasureState(paint: TextPaint) {
            paint.typeface = typeface
        }
    }
}
