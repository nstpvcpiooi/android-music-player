package com.example.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.navigation.AppNavigationBar
import com.example.musicplayer.ui.navigation.MiniPlayer
import com.example.musicplayer.ui.navigation.MyNavHost
import com.example.musicplayer.ui.navigation.PlayerState
import com.example.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }

            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController() // Khởi tạo NavController

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    PlayerState(
                        songTitle = "Song Title",
                        isPlaying = true,
                        onPlayPause = {},
                        onNext = {
                            println("Next")
                        },
                        onPrevious = {
                            println("Previous")
                        }
                    )
                )
                AppNavigationBar(navController)
            }
        },
    ) { innerPadding ->
        MyNavHost(navController, innerPadding)
    }
}










