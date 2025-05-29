package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.bold
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.databinding.DetailsViewBinding
import com.example.musicplayer.onprg.PlayNext
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MoreFeaturesBottomSheet : BottomSheetDialogFragment() {

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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.more_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentMusic = MainActivity.MusicListMA.find { it.id == musicId } ?: PlayerActivity.musicListPA.find { it.id == musicId }

        // val renameBtn = view.findViewById<TextView>(R.id.renameBtn) // Uncomment if you add renameBtn back
        // renameBtn?.setOnClickListener { ... }

        val deleteBtn = view.findViewById<TextView>(R.id.deleteBtn)
        deleteBtn.setOnClickListener {
            Toast.makeText(context, "Delete clicked for ${currentMusic?.title}", Toast.LENGTH_SHORT).show()
            // Add your delete logic here using currentMusic
            dismiss()
        }

        val shareBtn = view.findViewById<TextView>(R.id.shareBtn)
        shareBtn.setOnClickListener {
            Toast.makeText(context, "Share clicked for ${currentMusic?.title}", Toast.LENGTH_SHORT).show()
            // Add your share logic here using currentMusic
            // For example, to share a simple text:
            if (currentMusic != null) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this song: ${currentMusic.title} by ${currentMusic.artist}")
                startActivity(Intent.createChooser(shareIntent, "Share Song"))
            }
            dismiss()
        }

        val addToPNBtn = view.findViewById<TextView>(R.id.AddToPNBtn)
        addToPNBtn.setOnClickListener {

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
        Snackbar.make(view, "Play A Song First!!", Snackbar.LENGTH_LONG).show()
    }

//            if (currentMusic != null) {
//                try {
//                    val playNextList = PlayNext.playNextList // Get the reference to the true Play Next list
//                    var toastMessage: String
//
//                    // Add the selected song (currentMusic) to the PlayNext list if not already present
//                    if (!playNextList.contains(currentMusic)) {
//                        playNextList.add(currentMusic) // Modify the actual PlayNext.playNextList
//                        toastMessage = "Added ${currentMusic.title} to Play Next"
//                    } else {
//                        toastMessage = "${currentMusic.title} is already in Play Next"
//                    }
//
//                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
//
//                    // PlayerActivity is responsible for observing or fetching updates from PlayNext.playNextList
//                    // when it needs to play this queue. We no longer directly set PlayerActivity.musicListPA here.
//                    // Example: If PlayerActivity is actively playing from PlayNext, it might need a mechanism
//                    // to refresh its data, e.g., by calling a method like PlayerActivity.updateQueueFromPlayNext().
//
//                } catch (e: Exception) {
//                    Toast.makeText(context, "Error adding to Play Next: ${e.message}", Toast.LENGTH_LONG).show()
//                    // Log.e(TAG, "Error in addToPNBtn", e) // Consider adding Log for debugging
//                }
//            } else {
//                Toast.makeText(context, "Song not found", Toast.LENGTH_SHORT).show()
//            }
            dismiss()
        }

        val infoBtn = view.findViewById<TextView>(R.id.infoBtn)
        infoBtn.setOnClickListener {
            if (currentMusic != null) {
                val detailsDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.details_view, null)
                val binder = DetailsViewBinding.bind(detailsDialogView)
                // It's better to set text colors via themes/styles if possible
                // binder.detailsTV.setTextColor(Color.WHITE)

                val detailsDialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(detailsDialogView)
                    .setPositiveButton("OK") { self, _ -> self.dismiss() }
                    .setCancelable(false)
                    .create()

                detailsDialog.setOnShowListener {
                    // Consider using theme attributes for button text color
                    // detailsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                    // setDialogBtnBackground(requireContext(), detailsDialog) // Review if this utility is still needed and compatible
                }
                // Consider using theme attributes for dialog background
                // detailsDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))

                val str = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                    .append(currentMusic.title)
                    .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(currentMusic.duration / 1000))
                    .bold { append("\n\nLocation: ") }.append(currentMusic.path)
                binder.detailsTV.text = str
                detailsDialog.show()
            } else {
                Toast.makeText(context, "Song details not available", Toast.LENGTH_SHORT).show()
            }
            dismiss() // Dismiss the bottom sheet after showing the details dialog or if song is null
        }
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
