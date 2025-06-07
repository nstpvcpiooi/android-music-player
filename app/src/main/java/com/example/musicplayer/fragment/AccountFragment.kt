package com.example.musicplayer.fragment

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
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
import com.example.musicplayer.activity.DownloadActivity
import com.example.musicplayer.activity.LoginActivity
import com.example.musicplayer.activity.PlayerActivity
import com.example.musicplayer.activity.UploadActivity
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.FragmentAccountBinding
import com.example.musicplayer.model.Music
import com.example.musicplayer.onprg.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
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
        database = FirebaseDatabase.getInstance().reference
        firebaseAuth = FirebaseAuth.getInstance()

        emailValueTextView = binding.emailValue
        uploadMusicBtn = binding.uploadMusicBtn
        uploadedMusicRecyclerView = binding.uploadedMusicRecyclerView
        noUploadedMusicText = binding.noUploadedMusicText
        shimmerLayout = binding.shimmerUploadedMusic.root

        return binding.root
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
        emailValueTextView.text = firebaseAuth.currentUser?.email ?: "N/A"

        uploadMusicBtn.setOnClickListener {
            startActivity(Intent(requireContext(), UploadActivity::class.java))
        }

        setupRecyclerView()
        fetchUploadedMusic()
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(requireContext(), musicList)
        uploadedMusicRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
            isNestedScrollingEnabled = false
        }

        musicAdapter.setOnItemClickListener { position ->
            val music = musicList[position]

            if (music.path.isBlank() || !music.path.startsWith("http")) {
                Toast.makeText(requireContext(), getString(R.string.invalid_music_url), Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle(music.title)
                .setMessage(getString(R.string.music_action_prompt))
                .setPositiveButton(getString(R.string.play_music)) { dialog, _ ->
                    val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkInfo = connectivityManager.activeNetworkInfo
                    if (networkInfo == null || !networkInfo.isConnected) {
                        Toast.makeText(requireContext(), getString(R.string.check_network_connection), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    DownloadActivity.downloadPlaylist = ArrayList(musicList)
                    DownloadActivity.downloadIndex = position
                    startActivity(Intent(requireContext(), PlayerActivity::class.java))
                    dialog.dismiss()
                }
                .setNeutralButton(getString(R.string.download_music)) { dialog, _ ->
                    downloadMusic(music.title, music.path)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun downloadMusic(title: String, fileUrl: String) {
        try {
            val uri = Uri.parse(fileUrl)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(requireContext().contentResolver.getType(uri))
                ?.let { ".$it" } ?: MimeTypeMap.getFileExtensionFromUrl(fileUrl)?.let { ".$it" } ?: ".mp3"

            val request = DownloadManager.Request(uri).apply {
                setTitle(getString(R.string.download_title, title))
                setDescription(getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "$title$extension")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(requireContext(), getString(R.string.downloading_music, title), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.download_error, e.message), Toast.LENGTH_LONG).show()
            Log.e("DownloadError", "Error downloading music: ${e.message}", e)
        }
    }

    private fun fetchUploadedMusic() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("uploads").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                musicList.clear()
                if (snapshot.exists()) {
                    for (item in snapshot.children) {
                        val title = item.child("title").getValue(String::class.java)
                        val url = item.child("url").getValue(String::class.java)
                        val album = item.child("album").getValue(String::class.java) ?: ""
                        val artist = item.child("singer").getValue(String::class.java) ?: ""

                        if (title != null && url != null) {
                            musicList.add(
                                Music(
                                    id = item.key ?: "",
                                    title = title,
                                    album = album,
                                    artist = artist,
                                    duration = 0L,
                                    artUri = "",
                                    path = url
                                )
                            )
                        }
                    }
                    noUploadedMusicText.visibility = View.GONE
                    uploadedMusicRecyclerView.visibility = View.VISIBLE
                    shimmerLayout.visibility = View.GONE
                } else {
                    noUploadedMusicText.visibility = View.VISIBLE
                    uploadedMusicRecyclerView.visibility = View.GONE
                    shimmerLayout.visibility = View.GONE
                }
                musicAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Load failed: ${error.message}")
//                Toast.makeText(requireContext(), getString(R.string.load_music_failed, error.message), Toast.LENGTH_SHORT).show()
                noUploadedMusicText.visibility = View.VISIBLE
                uploadedMusicRecyclerView.visibility = View.GONE
                shimmerLayout.visibility = View.GONE
            }
        })
        shimmerLayout.visibility = View.VISIBLE
    }
}
