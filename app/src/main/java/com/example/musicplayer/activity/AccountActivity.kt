package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityAccountBinding
import com.example.musicplayer.model.Music
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var musicAdapter: MusicAdapter
    private val musicList = ArrayList<Music>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        database = FirebaseDatabase.getInstance().reference

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.backBtnAccount.setOnClickListener { finish() }
        binding.logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.uploadMusicBtn.setOnClickListener{
            startActivity(Intent(this, UploadActivity::class.java))
            finish()
        }

        binding.emailValue.setText(firebaseAuth.currentUser?.email.toString());

        setupRecyclerView()
        fetchUploadedMusic()
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(this, musicList)
        binding.uploadedMusicRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            adapter = musicAdapter
        }

        binding.uploadedMusicRecyclerView.adapter = musicAdapter
    }

    private fun fetchUploadedMusic() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("uploads").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                musicList.clear()
                if (snapshot.exists()) {
                    for (musicSnap in snapshot.children) {
                        val music = musicSnap.getValue(Music::class.java)
                        music?.let { musicList.add(it) }
                    }
                    binding.noUploadedMusicText.visibility = if (musicList.isEmpty()) View.VISIBLE else View.GONE
                    musicAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Load failed: ${error.message}")
            }
        })
    }
}