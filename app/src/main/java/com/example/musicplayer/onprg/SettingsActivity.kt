package com.example.musicplayer.onprg

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.musicplayer.BuildConfig
import com.example.musicplayer.R // Added import for R class
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.databinding.ActivitySettingsBinding
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.setDialogBtnBackground
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex]) // Theme is now set by the activity in AndroidManifest or AppTheme
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbarSettings) // Use the new toolbar ID
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSettings.setNavigationOnClickListener {
            finish() // Navigate back when the back button is pressed
        }

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
        }

        binding.logoutBtn.setOnClickListener {

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
