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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.components.AppNavigationBar
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.components.MyNavHost
import com.example.musicplayer.ui.components.PlayerState
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
    var isPlaying by remember { mutableStateOf(false) }
    var songTitle by remember { mutableStateOf("Song Title") }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    PlayerState(
                        songTitle = songTitle,
                        isPlaying = isPlaying,
                        onPlayPause = {
                            isPlaying = !isPlaying
                        },
                        onNext = {
                            songTitle = "Next Song"
                        },
                        onPrevious = {
                            songTitle = "Previous Song"
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










