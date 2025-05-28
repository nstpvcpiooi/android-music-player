package com.example.musicplayer.onprg

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.BuildConfig
import com.example.musicplayer.R // Added import for R class
import com.example.musicplayer.activity.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.musicplayer.databinding.ActivitySettingsBinding
import com.example.musicplayer.utils.exitApplication
import com.example.musicplayer.utils.setDialogBtnBackground
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Settings"
        when(MainActivity.themeIndex){
            0 -> binding.coolPinkTheme.setBackgroundColor(Color.YELLOW)
            1 -> binding.coolBlueTheme.setBackgroundColor(Color.YELLOW)
            2 -> binding.coolPurpleTheme.setBackgroundColor(Color.YELLOW)
            3 -> binding.coolGreenTheme.setBackgroundColor(Color.YELLOW)
            4 -> binding.coolBlackTheme.setBackgroundColor(Color.YELLOW)
        }
        binding.coolPinkTheme.setOnClickListener { saveTheme(0) }
        binding.coolBlueTheme.setOnClickListener { saveTheme(1) }
        binding.coolPurpleTheme.setOnClickListener { saveTheme(2) }
        binding.coolGreenTheme.setOnClickListener { saveTheme(3) }
        binding.coolBlackTheme.setOnClickListener { saveTheme(4) }
        binding.versionName.text = setVersionDetails()
        binding.sortBtn.setOnClickListener {
            val menuList = arrayOf("Recently Added", "Song Title", "File Size")
            var currentSort = MainActivity.sortOrder
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Sorting")
                .setPositiveButton("OK"){ _, _ ->
                    val editor = getSharedPreferences("SORTING", MODE_PRIVATE).edit()
                    editor.putInt("sortOrder", currentSort)
                    editor.apply()
                }
                .setSingleChoiceItems(menuList, currentSort){ _,which->
                    currentSort = which
                }
            val customDialog = builder.create()
            customDialog.show()

            setDialogBtnBackground(this, customDialog)
        }

        // Theme Mode Selection
        val appSettingPrefs = getSharedPreferences("APP_SETTINGS_PREFS", MODE_PRIVATE)
        val currentNightMode = appSettingPrefs.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightModeRb.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkModeRb.isChecked = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.systemDefaultRb.isChecked = true
        }

        binding.themeModeRg.setOnCheckedChangeListener { _, checkedId ->
            val editor = appSettingPrefs.edit()
            when (checkedId) {
                R.id.lightModeRb -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_NO)
                }
                R.id.darkModeRb -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_YES)
                }
                R.id.systemDefaultRb -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    editor.putInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            editor.apply()
            // Optional: Restart activity or recreate to apply theme immediately
            // recreate() // or prompt user to restart
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
