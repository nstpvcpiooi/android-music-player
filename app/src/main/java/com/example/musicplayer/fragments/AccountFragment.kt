package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.musicplayer.R

class AccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        initViews(view)

        return view
    }

    private fun initViews(view: View) {
        // Setup username and email with sample data (in real app, get from user account)
        val usernameText: TextView = view.findViewById(R.id.username_text)
        val emailText: TextView = view.findViewById(R.id.email_text)

        // For demo purposes, set default values
        usernameText.text = "Music Lover"
        emailText.text = "user@example.com"

        // Setup click listeners for account options
        view.findViewById<View>(R.id.profile_settings).setOnClickListener {
            // Open profile settings screen
            Toast.makeText(context, "Profile Settings", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.app_settings).setOnClickListener {
            // Open app settings screen
            Toast.makeText(context, "App Settings", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.logout).setOnClickListener {
            // Handle logout
            Toast.makeText(context, "Logout", Toast.LENGTH_SHORT).show()
        }
    }
}
