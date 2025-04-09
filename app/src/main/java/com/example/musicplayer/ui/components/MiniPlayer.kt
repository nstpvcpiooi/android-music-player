package com.example.musicplayer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import com.example.musicplayer.ui.theme.Orange
import kotlinx.coroutines.launch

data class PlayerState(
    val songTitle: String,
    var isPlaying: Boolean,
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
            MusicPlayerScreen(
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
            .background(Orange, RoundedCornerShape(10.dp))
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
                    painterResource(R.drawable.backward_step_solid),
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = {
                playerState.onPlayPause()
            }) {
                Icon(
                    if (playerState.isPlaying) painterResource(R.drawable.pause_solid)
                    else painterResource(R.drawable.play_solid),
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = { playerState.onNext() }) {
                Icon(
                    painterResource(R.drawable.forward_step_solid),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MusicPlayerScreen(onHideButtonClick: () -> Unit) {
    // State để theo dõi trạng thái bài hát và thanh thời gian
    val isPlaying = remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf(0f) }
    val totalTime =
        remember { mutableStateOf(180f) } // Giả sử tổng thời gian bài hát là 180 giây (3 phút)

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
            .clickable(onClick = onHideButtonClick)
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown, contentDescription = "Hide",
            Modifier.size(30.dp), tint = Color.Gray
        )


        Text(text = "Scroll down to hide", color = Color.Gray)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .size(300.dp) // Kích thước album cover
                .clip(RoundedCornerShape(12.dp)) // Bo góc
                .background(Color.Gray) // Màu nền cho album cover
        ) {
            // Thêm hình ảnh album cover ở đây
            // Example: Image(painter = painterResource(id = R.drawable.album_cover),
            // contentDescription = "Album Cover")
            Image(
                painter = painterResource(R.drawable.album_4_walls),
                contentDescription = null,
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("4 walls", style = MaterialTheme.typography.titleLarge, fontWeight = Bold)
        Spacer(modifier = Modifier.height(5.dp))
        Text("f(x)", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // Thanh thời gian
        Slider(
            value = currentTime.value,
            onValueChange = { currentTime.value = it },
            valueRange = 0f..totalTime.value,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Orange,
                activeTrackColor = Orange,
                inactiveTrackColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Pause/Play, Next, Previous
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = 30.dp,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { /* Previous song action */ }) {
                Icon(
                    painterResource(R.drawable.backward_step_solid),
                    contentDescription = "Previous",
                    tint = Orange
                )
            }

            IconButton(onClick = {
                // Toggle Play/Pause
                isPlaying.value = !isPlaying.value
            }) {
                Icon(
                    if (isPlaying.value) painterResource(R.drawable.pause_solid)
                    else painterResource(R.drawable.play_solid),
                    contentDescription = "Play/Pause",
                    tint = Orange,
                )
            }

            IconButton(onClick = { /* Next song action */ }) {
                Icon(
                    painterResource(R.drawable.forward_step_solid),
                    contentDescription = "Next",
                    tint = Orange
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicPlayerScreenPreview() {
    MusicPlayerScreen(
        onHideButtonClick = {}
    )
}


@Preview
@Composable
private fun MiniPlayerPreview() {
    MiniPlayer(
        PlayerState(
            songTitle = "Song Title",
            isPlaying = false,
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