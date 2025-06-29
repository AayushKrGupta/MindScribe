// ui.components/AudioPlayer.kt
package ui.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

private const val TAG = "AudioPlayerDebug"

@Composable
fun AudioPlayer(
    audioFilePath: String?,
    isRecording: Boolean = false
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isPrepared by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var fileExists by remember { mutableStateOf(false) }

    // Check file existence and size
    LaunchedEffect(audioFilePath) {
        fileExists = if (audioFilePath != null) {
            val file = File(audioFilePath)
            file.exists() && file.length() > 0
        } else {
            false
        }
    }

    // Handle MediaPlayer lifecycle
    LaunchedEffect(audioFilePath, isRecording, fileExists) {
        Log.d(TAG, "AudioPlayer: LaunchedEffect - path: $audioFilePath, recording: $isRecording, exists: $fileExists")

        // Reset states
        isPlaying = false
        currentPosition = 0
        duration = 0
        isPrepared = false
        showError = false

        // Don't try to initialize player while recording or if file doesn't exist
        if (isRecording || !fileExists) {
            mediaPlayer.reset()
            return@LaunchedEffect
        }

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioFilePath)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                Log.d(TAG, "AudioPlayer: MediaPlayer prepared.")
                duration = mp.duration
                isPrepared = true
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
                showError = true
                mp.reset()
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "AudioPlayer: IOException setting data source: ${e.message}")
            isPrepared = false
            showError = true
        } catch (e: Exception) {
            Log.e(TAG, "AudioPlayer: General error setting data source: ${e.message}")
            isPrepared = false
            showError = true
        }
    }

    // Update progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            currentPosition = mediaPlayer.currentPosition
            delay(100)
        }
    }

    // Clean up MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "AudioPlayer: Disposing MediaPlayer.")
            isPlaying = false
            mediaPlayer.release()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isRecording -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Recording",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording in progress...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                audioFilePath == null -> {
                    Text(
                        text = "No audio recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                !fileExists -> {
                    Text(
                        text = "Audio file not found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                showError -> {
                    Text(
                        text = "Error loading audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                !isPrepared -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preparing audio...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
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
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            enabled = isPrepared
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            LinearProgressIndicator(
                                progress = if (duration > 0) currentPosition / duration.toFloat() else 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTime(duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}