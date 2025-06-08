package com.example.musicplayer.fragment

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.UploadActivity
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.FragmentAccountBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.activity.SettingsActivity
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.utils.PlayNext
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AccountFragment : Fragment(), ServiceConnection {

    private lateinit var binding: FragmentAccountBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var musicAdapter: MusicAdapter
    private val musicList = ArrayList<Music>()

    private lateinit var emailValueTextView: TextView
    private lateinit var uploadMusicBtn: ImageButton
    private lateinit var uploadedMusicRecyclerView: RecyclerView
    private lateinit var noUploadedMusicText: TextView
    private lateinit var shimmerLayout: View

    // For managing downloaded songs state
    private lateinit var downloadedSongIds: MutableSet<String>
    private val DOWNLOADED_SONGS_PREFS = "DOWNLOADED_SONGS_PREFS"
    private val DOWNLOADED_SONGS_KEY = "downloaded_ids"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()

        emailValueTextView = binding.emailValue
        uploadMusicBtn = binding.uploadMusicBtn
        uploadedMusicRecyclerView = binding.uploadedMusicRecyclerView
        noUploadedMusicText = binding.noUploadedMusicText
        shimmerLayout = binding.shimmerUploadedMusic.root

        // First, load saved selections from SharedPreferences
        loadSavedSelections()
        // Load downloaded song IDs
        loadDownloadedSongIds()

        return binding.root
    }

    private fun loadSavedSelections() {
        // Load saved selections from SharedPreferences if it hasn't been loaded yet
        if (UploadActivity.mySelectionList.isEmpty()) {
            val sharedPrefs = requireActivity().getSharedPreferences("MY_SELECTIONS", AppCompatActivity.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("selections", null)

            if (jsonString != null) {
                val type = object : TypeToken<ArrayList<Music>>() {}.type
                try {
                    UploadActivity.mySelectionList = Gson().fromJson(jsonString, type)
                } catch (e: Exception) {
                    Log.e("AccountFragment", "Error loading selections: ${e.message}")
                    UploadActivity.mySelectionList = ArrayList()
                }
            }
        }
    }

    private fun loadDownloadedSongIds() {
        val prefs = requireActivity().getSharedPreferences(DOWNLOADED_SONGS_PREFS, AppCompatActivity.MODE_PRIVATE)
        downloadedSongIds = prefs.getStringSet(DOWNLOADED_SONGS_KEY, HashSet())?.toMutableSet() ?: HashSet()
    }

    private fun saveDownloadedSongIds() {
        val prefs = requireActivity().getSharedPreferences(DOWNLOADED_SONGS_PREFS, AppCompatActivity.MODE_PRIVATE)
        prefs.edit().putStringSet(DOWNLOADED_SONGS_KEY, downloadedSongIds).apply()
    }

    private fun toggleSongDownloadedState(musicId: String): Boolean {
        val isCurrentlyDownloaded = downloadedSongIds.contains(musicId)
        if (isCurrentlyDownloaded) {
            downloadedSongIds.remove(musicId)
        } else {
            downloadedSongIds.add(musicId)
        }
        saveDownloadedSongIds()
        return !isCurrentlyDownloaded // Return the new state (true if it is now downloaded)
    }

    private fun isSongDownloaded(musicId: String): Boolean {
        return downloadedSongIds.contains(musicId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarAndMenu(view)
        initAccountFunctionality()
    }

    private fun setupToolbarAndMenu(view: View) {
        val toolbar = binding.accountToolbar
        (requireActivity() as? AppCompatActivity)?.setSupportActionBar(toolbar)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.account_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settings_button -> {
                        try {
                            startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), getString(R.string.settings_error, e.message), Toast.LENGTH_LONG).show()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initAccountFunctionality() {
        emailValueTextView.text = firebaseAuth.currentUser?.email ?: "My Music Collection"

        uploadMusicBtn.setOnClickListener {
            startActivity(Intent(requireContext(), UploadActivity::class.java))
        }

        setupRecyclerView()
        fetchMySelections()
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(
            requireContext(),
            musicList,
            isSongDownloadedCallback = { musicId -> isSongDownloaded(musicId) },
            onDownloadClickCallback = { musicId ->
                val index = musicList.indexOfFirst { it.id == musicId }
                if (index != -1) {
                    if (!isSongDownloaded(musicId)) { // Song is not downloaded, about to download
                        // Show loading animation for this specific item
                        val viewHolder = uploadedMusicRecyclerView.findViewHolderForAdapterPosition(index)
                        if (viewHolder is MusicAdapter.MyHolder) {
                            viewHolder.downloadButton?.visibility = View.GONE
                            viewHolder.downloadProgressBar?.visibility = View.VISIBLE
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            toggleSongDownloadedState(musicId) // Toggle state and save
                            // Notify adapter to rebind the item. onBindViewHolder will handle hiding progressbar.
                            musicAdapter.notifyItemChanged(index)
                        }, 3000) // 3 seconds delay

                    } else { // Song is already downloaded, about to 'un-download' -> Show confirmation
                        val bottomSheetDialog = BottomSheetDialog(requireContext())
                        val bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_confirm_delete, null)
                        bottomSheetDialog.setContentView(bottomSheetView)

                        val positiveButton = bottomSheetView.findViewById<Button>(R.id.bs_positive_button_delete)
                        val negativeButton = bottomSheetView.findViewById<Button>(R.id.bs_negative_button_delete)

                        positiveButton.setOnClickListener {
                            toggleSongDownloadedState(musicId) // Toggle state and save
                            musicAdapter.notifyItemChanged(index) // Rebind to update icon immediately
                            bottomSheetDialog.dismiss()
                        }

                        negativeButton.setOnClickListener {
                            bottomSheetDialog.dismiss()
                        }
                        bottomSheetDialog.show()
                    }
                }
            }
        )
        uploadedMusicRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
            isNestedScrollingEnabled = false
        }

        musicAdapter.setOnItemClickListener { position ->
            playSelectedSong(position)
        }

        musicAdapter.setOnItemLongClickListener { position ->
            showRemoveDialog(position)
            true
        }
    }

    private fun showRemoveDialog(position: Int) {
        if (position < 0 || position >= musicList.size) return

        val music = musicList[position]

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_remove_song, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val titleTextView = bottomSheetView.findViewById<TextView>(R.id.bs_title_remove_song)
        val messageTextView = bottomSheetView.findViewById<TextView>(R.id.bs_message_remove_song)
        val positiveButton = bottomSheetView.findViewById<Button>(R.id.bs_positive_button_remove_song)
        val negativeButton = bottomSheetView.findViewById<Button>(R.id.bs_negative_button_remove_song)

        titleTextView.text = getString(R.string.confirm_remove_song_title)
        messageTextView.text = getString(R.string.confirm_remove_song_message, music.title)

        positiveButton.setOnClickListener {
            if (position < musicList.size) { // Re-check position in case list changed
                removeFromSelections(position)
            }
            bottomSheetDialog.dismiss()
        }

        negativeButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun removeFromSelections(position: Int) {
        val removedMusic = musicList[position]

        // Remove from both the display list and the saved list
        musicList.removeAt(position)
        musicAdapter.notifyItemRemoved(position)

        // Find and remove from the saved selections list
        val indexInSelections = UploadActivity.mySelectionList.indexOfFirst { it.id == removedMusic.id }
        if (indexInSelections != -1) {
            UploadActivity.mySelectionList.removeAt(indexInSelections)
            // Save changes to SharedPreferences
            saveSelectionChanges()
        }

        // Show empty view if list is now empty
        if (musicList.isEmpty()) {
            uploadedMusicRecyclerView.visibility = View.GONE
            noUploadedMusicText.visibility = View.VISIBLE
        }

        Toast.makeText(requireContext(), "Removed \"${removedMusic.title}\" from My Selections", Toast.LENGTH_SHORT).show()
    }

    private fun saveSelectionChanges() {
        val sharedPrefs = requireActivity().getSharedPreferences("MY_SELECTIONS", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val jsonString = Gson().toJson(UploadActivity.mySelectionList)
        editor.putString("selections", jsonString)
        editor.apply()
    }

    private fun playSelectedSong(position: Int) {
        if (musicList.isEmpty() || position >= musicList.size) return
        val music = musicList[position]
        val songId = music.id // Store for use in delayed runnable

        if (!isSongDownloaded(songId)) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_confirm_download, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val positiveButton = bottomSheetView.findViewById<Button>(R.id.bs_positive_button)
            val negativeButton = bottomSheetView.findViewById<Button>(R.id.bs_negative_button)

            // Texts are already set in XML, but can be overridden if needed
            // titleTextView.text = getString(R.string.confirm_download_title)
            // messageTextView.text = getString(R.string.confirm_download_message)

            positiveButton.setOnClickListener {
                bottomSheetDialog.dismiss() // Dismiss dialog immediately

                // Show ProgressBar, Hide Button
                val currentPositionInListBeforeDelay = musicList.indexOfFirst { it.id == songId }
                if (currentPositionInListBeforeDelay != -1) {
                    val viewHolder = uploadedMusicRecyclerView.findViewHolderForAdapterPosition(currentPositionInListBeforeDelay)
                    if (viewHolder is MusicAdapter.MyHolder) {
                        viewHolder.downloadButton?.visibility = View.GONE
                        viewHolder.downloadProgressBar?.visibility = View.VISIBLE
                    }
                }

                // Start a 3-second delay to simulate download
                Handler(Looper.getMainLooper()).postDelayed({
                    // After 3 seconds, mark as downloaded and update UI
                    val stillNotDownloaded = !isSongDownloaded(songId)
                    if (stillNotDownloaded) {
                        toggleSongDownloadedState(songId) // This adds to downloadedSongIds and saves
                    }

                    // Notify adapter to change icon to "download_icon_filled"
                    // Find the current position of the item, as it might have changed if list is dynamic
                    val currentPositionInList = musicList.indexOfFirst { it.id == songId }
                    if (currentPositionInList != -1) {
                        if (stillNotDownloaded) { // Only notify if state actually changed
                             musicAdapter.notifyItemChanged(currentPositionInList) // This will trigger rebind and hide progressbar
                        }
                        proceedWithPlayback(currentPositionInList)
                    } else {
                        // Song might have been removed from list during the delay
                        Log.w("AccountFragment", "Song $songId not found in list after delay for playback.")
                        Toast.makeText(requireContext(), "Song not found, playback cancelled.", Toast.LENGTH_SHORT).show()
                    }

                }, 3000) // 3000 milliseconds = 3 seconds
            }

            negativeButton.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        } else {
            // Song is already downloaded, proceed to play immediately
            proceedWithPlayback(position)
        }
    }

    private fun proceedWithPlayback(position: Int) {
        if (musicList.isEmpty() || position >= musicList.size) return

        // Create playlist from selection list
        PlayerActivity.musicListPA = ArrayList(musicList)
        PlayerActivity.songPosition = position

        // Set source for "Now Playing" to identify it came from My Selections
        PlayerActivity.currentPlaylistOrigin = "MySelections"

        // Clear the dedicated "Play Next" queue, as a new playback context is starting.
        // PlayerActivity should primarily use musicListPA for this new context.
        PlayNext.playNextList.clear()

        if (PlayerActivity.musicService != null) {
            // If service is already running, just update the song and play
            PlayerActivity.musicService!!.createMediaPlayer()
            PlayerActivity.musicService!!.playMusic()
            showNowPlayingFragment()
        } else {
            // If service isn't running, start and bind to it
            try {
                val intent = Intent(requireContext(), MusicService::class.java)
                requireActivity().startService(intent)
                requireActivity().bindService(intent, this, AppCompatActivity.BIND_AUTO_CREATE)

                // We don't show the Now Playing fragment here immediately
                // It will be shown in onServiceConnected when music actually starts playing
            } catch (e: Exception) {
                Log.e("AccountFragment", "Error starting music service: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to start music playback: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Helper method to show Now Playing fragment
    private fun showNowPlayingFragment() {
        (requireActivity() as? MainActivity)?.let { mainActivity ->
            mainActivity.findViewById<View>(R.id.nowPlaying)?.visibility = View.VISIBLE
        }
    }

    // ServiceConnection implementation
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        PlayerActivity.musicService = binder.currentService()
        // Initialize audio manager in the service
        if (PlayerActivity.musicService != null && PlayerActivity.musicService!!.mediaPlayer != null && PlayerActivity.musicService!!.mediaPlayer!!.isPlaying) {
            // If music is already playing, likely from another source, we might not want to immediately request focus
            // or we might want to ensure this new playback context is the one taking over.
            // For now, let's assume if service is connected, we are good to proceed with current logic.
        } else if (PlayerActivity.musicService != null) {
            PlayerActivity.musicService!!.audioManager = requireActivity().getSystemService(AppCompatActivity.AUDIO_SERVICE) as android.media.AudioManager
            PlayerActivity.musicService!!.audioManager.requestAudioFocus(PlayerActivity.musicService, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)
        }

        // Create and play the media
        PlayerActivity.musicService!!.createMediaPlayer()
        PlayerActivity.musicService!!.playMusic()
        showNowPlayingFragment()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        PlayerActivity.musicService = null
    }

    private fun fetchMySelections() {
        // Show shimmer loading effect
        shimmerLayout.visibility = View.VISIBLE
        uploadedMusicRecyclerView.visibility = View.GONE
        noUploadedMusicText.visibility = View.GONE

        // Simulate network loading
        binding.root.postDelayed({
            // Clear current list
            musicList.clear()

            // Load songs from UploadActivity's saved selections
            if (UploadActivity.mySelectionList.isNotEmpty()) {
                musicList.addAll(UploadActivity.mySelectionList)
                noUploadedMusicText.visibility = View.GONE
                uploadedMusicRecyclerView.visibility = View.VISIBLE
            } else {
                noUploadedMusicText.visibility = View.VISIBLE
                uploadedMusicRecyclerView.visibility = View.GONE
            }

            musicAdapter.notifyDataSetChanged()
            shimmerLayout.visibility = View.GONE
        }, 800) // Simulate network delay for better UX
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning to this fragment
        fetchMySelections()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to unbind from the service if we're bound
        try {
            if (PlayerActivity.musicService != null) {
                requireActivity().unbindService(this)
            }
        } catch (e: Exception) {
            // Service might not be bound, ignore
        }
    }
}
