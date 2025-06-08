package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.musicplayer.BuildConfig
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivitySettingsBinding
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSettings.setNavigationOnClickListener {
            finish() // Navigate back when the back button is pressed
        }

        firebaseAuth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

        // Theme Mode Selection
        val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
        val currentNightMode = appSettingPrefs.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val initialSelectedButtonId = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.lightModeBtn
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.darkModeBtn
            else -> R.id.systemDefaultBtn // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM or other
        }
        binding.themeModeToggleGroup.check(initialSelectedButtonId)

        binding.themeModeToggleGroup.addOnButtonCheckedListener { group, changedButtonId, isChecked ->
            val currentSelectionId = group.checkedButtonId
            if (currentSelectionId != View.NO_ID) {
                val editor = appSettingPrefs.edit()
                when (currentSelectionId) {
                    R.id.lightModeBtn -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    R.id.darkModeBtn -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    R.id.systemDefaultBtn -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                }
                editor.apply()
                // Optional: Restart activity or recreate to apply theme immediately
                // recreate() // or prompt user to restart
            }
        }

        binding.changePasswordBtn.setOnClickListener {
            val user = firebaseAuth.currentUser
            if (user != null && user.email != null) {
                firebaseAuth.sendPasswordResetEmail(user.email!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent to ${user.email}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to send password reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "User not logged in or email not available.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutBtn.setOnClickListener {
            // Display a logout message
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()

            try {
                // Stop MusicService first to release audio resources
                try {
                    val serviceIntent = Intent(this, MusicService::class.java)
                    stopService(serviceIntent)
                } catch (e: Exception) {
                    Log.e("SettingsActivity", "Error stopping MusicService: ${e.message}")
                }

                // Sign out from Firebase
                firebaseAuth.signOut()

                // Create intent to navigate to login screen
                val loginIntent = Intent(this, LoginActivity::class.java)

                // Clear activity stack so user can't go back after logout
                loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                // Add flag to indicate successful logout
                loginIntent.putExtra("USER_LOGGED_OUT", true)

                // Launch LoginActivity
                startActivity(loginIntent)

                // Finish SettingsActivity
                finish()
            } catch (e: Exception) {
                // Handle any errors during logout
                Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SettingsActivity", "Logout error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun saveTheme(index: Int){
        if(MainActivity.themeIndex != index){
            val editor = getSharedPreferences("THEMES", MODE_PRIVATE).edit()
            editor.putInt("themeIndex", index)
            editor.apply()
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Apply Theme")
                .setMessage("Do you want to apply theme?")
                .setPositiveButton("Yes"){ _, _ ->
                    exitApplication()
                }
                .setNegativeButton("No"){dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()

            setDialogBtnBackground(this, customDialog)
        }
    }

    private fun setVersionDetails():String{
        return "Version Name: ${BuildConfig.VERSION_NAME}"
    }
}