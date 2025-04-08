package com.example.musicplayer.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import kotlinx.coroutines.launch

data class PlayerState(
    val songTitle: String,
    val isPlaying: Boolean,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(playerState: PlayerState) {
    var openBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (openBottomSheet) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { openBottomSheet = false },
            dragHandle = {},
            shape = RoundedCornerShape(10.dp)
        ) {
            PlayerContent(
                onHideButtonClick = {
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) openBottomSheet = false
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(Color.Blue, RoundedCornerShape(10.dp))
            .padding(5.dp)
            .clickable { openBottomSheet = true }
    ) {
        // Chứa tên bài hát và các nút điều khiển
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(
                    text = playerState.songTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 100.dp)
                )
            }
            IconButton(onClick = { playerState.onPrevious() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }

            IconButton(onClick = { playerState.onPlayPause() }) {
                Icon(
                    if (playerState.isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White
                )
            }

            IconButton(onClick = { playerState.onNext() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
        }
    }
}


@Composable
fun PlayerContent(
    onHideButtonClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp), contentAlignment = Alignment.Center
        ) {
            Text("Swipe up to expand sheet")
        }
        Box(
            modifier = Modifier.padding(50.dp, 50.dp)
        ) {
            //image
            Image(
                painter = painterResource(R.drawable.album_4_walls),
                contentDescription = null,
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onHideButtonClick
        ) {
            Text(text = "Cancel")
        }
    }
}

@Preview
@Composable
private fun MiniPlayerPreview() {
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
}