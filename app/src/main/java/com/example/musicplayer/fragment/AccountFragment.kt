package com.example.musicplayer.fragment

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        musicAdapter = MusicAdapter(requireContext(), musicList)
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
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Remove song")
            .setMessage("Do you want to remove \"${music.title}\" from My Selections?")
            .setPositiveButton(getString(R.string.download_music)) { dialog, _ ->
                // Remove from the list
                if (position < musicList.size) {
                    removeFromSelections(position)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
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

        // Create playlist from selection list
        PlayerActivity.musicListPA = ArrayList(musicList)
        PlayerActivity.songPosition = position

        // Set source for "Now Playing" to identify it came from My Selections
        PlayerActivity.currentPlaylistOrigin = "MySelections"

        // Clear PlayNext list and add the selection
        PlayNext.playNextList.clear()
        PlayNext.playNextList.addAll(musicList)

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
        PlayerActivity.musicService!!.audioManager = requireActivity().getSystemService(AppCompatActivity.AUDIO_SERVICE) as android.media.AudioManager
        PlayerActivity.musicService!!.audioManager.requestAudioFocus(PlayerActivity.musicService, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)

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
