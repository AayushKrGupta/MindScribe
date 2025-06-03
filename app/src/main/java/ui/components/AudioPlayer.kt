// ui.components/AudioPlayer.kt
package ui.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

private const val TAG = "AudioPlayerDebug"

@Composable
fun AudioPlayer(audioFilePath: String) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isPrepared by remember { mutableStateOf(false) }

    // Use LaunchedEffect to handle MediaPlayer lifecycle based on audioFilePath
    LaunchedEffect(audioFilePath) {
        Log.d(TAG, "AudioPlayer: LaunchedEffect for audioFilePath: $audioFilePath")
        isPlaying = false
        currentPosition = 0
        duration = 0
        isPrepared = false

        mediaPlayer.reset() // Reset for new file
        try {
            if (File(audioFilePath).exists()) {
                mediaPlayer.setDataSource(audioFilePath)
                mediaPlayer.prepareAsync() // Prepare asynchronously
                mediaPlayer.setOnPreparedListener { mp ->
                    Log.d(TAG, "AudioPlayer: MediaPlayer prepared.")
                    duration = mp.duration
                    isPrepared = true
                    // If you want to auto-play after preparation, set isPlaying = true here
                    // isPlaying = true
                    // mp.start()
                }
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                    Log.d(TAG, "AudioPlayer: Playback completed.")
                }
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "AudioPlayer: MediaPlayer error: what=$what, extra=$extra")
                    isPrepared = false
                    isPlaying = false
                    mp.reset()
                    false // Return false to indicate the error was not handled
                }
            } else {
                Log.e(TAG, "AudioPlayer: File does not exist at path: $audioFilePath")
            }
        } catch (e: IOException) {
            Log.e(TAG, "AudioPlayer: IOException setting data source: ${e.message}")
            isPrepared = false
        } catch (e: Exception) {
            Log.e(TAG, "AudioPlayer: General error setting data source: ${e.message}")
            isPrepared = false
        }
    }

    // Coroutine to update progress
    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            currentPosition = mediaPlayer.currentPosition
            delay(100) // Update every 100ms
        }
    }

    // DisposableEffect for cleanup
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "AudioPlayer: Disposing MediaPlayer.")
            isPlaying = false
            mediaPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isPrepared) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.start()
                        }
                        isPlaying = !isPlaying
                    },
                    enabled = isPrepared // Only enable if media is prepared
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                // Progress bar
                LinearProgressIndicator(
                    progress = if (duration > 0) currentPosition / duration.toFloat() else 0f,
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(horizontal = 8.dp)
                )
                Text("${formatTime(currentPosition)} / ${formatTime(duration)}")
            }
        } else {
            // Show loading or error state
            Text("Loading audio...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}