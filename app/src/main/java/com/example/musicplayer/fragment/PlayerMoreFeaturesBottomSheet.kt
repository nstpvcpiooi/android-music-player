package com.example.musicplayer.fragment

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.musicplayer.R
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.PlayerActivity.Companion.loudnessEnhancer
import com.example.musicplayer.activity.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.activity.PlayerActivity.Companion.musicService
import com.example.musicplayer.activity.PlayerActivity.Companion.songPosition
import com.example.musicplayer.databinding.AudioBoosterBinding
import com.example.musicplayer.databinding.PlayerMoreFeaturesBottomSheetBinding
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
                R.id.equalizerBtnPA -> {
                    try {
                        val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                        eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
                        eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        startActivityForResult(eqIntent, 13)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                    true
                }
                R.id.boosterBtnPA -> {
                    val customDialogB = LayoutInflater.from(requireContext()).inflate(R.layout.audio_booster, binding.root, false)
                    val bindingB = AudioBoosterBinding.bind(customDialogB)
                    val dialogB = MaterialAlertDialogBuilder(requireContext())
                        .setView(customDialogB)
                        .setOnCancelListener { PlayerActivity.musicService?.playMusic() }
                        .setPositiveButton("OK") { self, _ ->
                            // loudnessEnhancer expects gain in millibels.
                            // Slider value is 0-500. Multiply by 10 to get 0-5000 millibels.
                            loudnessEnhancer.setTargetGain(bindingB.audioBoosterSlider.value.toInt() * 10)
                            PlayerActivity.musicService?.playMusic()
                            self.dismiss()
                        }
                        .create()
                    dialogB.show()

                    // Set initial slider value. TargetGain is in millibels. Divide by 10.
                    bindingB.audioBoosterSlider.value = loudnessEnhancer.targetGain.toInt() / 10f
                    bindingB.progressText.text = "Audio Boost\n\n${bindingB.audioBoosterSlider.value.toInt()} %"

                    bindingB.audioBoosterSlider.addOnChangeListener { _, value, _ ->
                        bindingB.progressText.text = "Audio Boost\n\n${value.toInt()} %"
                    }
                    setDialogBtnBackground(requireContext(), dialogB)
                    dismiss()
                    true
                }
                else -> false
            }
        }

        // Apply custom font to NavigationView items
        try {
            val menu = binding.playerFeaturesNavigationView.menu
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
            // Check if subMenu is not null, although hasSubMenu() should imply it.
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

    private inner class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(ds: TextPaint?) {
            ds?.typeface = typeface
        }

        override fun updateMeasureState(paint: TextPaint) {
            paint.typeface = typeface
        }
    }
}
