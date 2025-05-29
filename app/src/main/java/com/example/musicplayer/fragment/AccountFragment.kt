package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.musicplayer.R
import com.example.musicplayer.activity.AccountActivity
import com.example.musicplayer.activity.DownloadActivity
import com.example.musicplayer.onprg.AboutActivity
import com.example.musicplayer.onprg.SettingsActivity
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
            // Open account settings screen
            try {
                startActivity(Intent(requireContext(), AccountActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open Account Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Add click listener for My Uploaded Files
        view.findViewById<View>(R.id.my_uploaded_files).setOnClickListener {
            // Open uploaded files screen
            try {
                startActivity(Intent(requireContext(), DownloadActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open My Uploaded Files: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.app_settings).setOnClickListener {
            // Open app settings screen
            try {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open App Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý nút About riêng biệt
        view.findViewById<View>(R.id.about_button).setOnClickListener {
            // Mở AboutActivity
            try {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open About: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý nút Exit riêng biệt - giống như nút Exit trong navigation drawer
        view.findViewById<View>(R.id.exit_button).setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Exit")
                .setMessage("Do you want to close app?")
                .setPositiveButton("Yes") { _, _ ->
                    try {
                        exitApplication()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot exit app: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()

            // Áp dụng style cho nút dialog nếu có hàm setDialogBtnBackground
            try {
                setDialogBtnBackground(requireContext(), customDialog)
            } catch (e: Exception) {
                // Bỏ qua nếu không có hàm này
            }
        }
    }
}
