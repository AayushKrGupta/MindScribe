
package com.example.mindscribe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mindscribe.ui.components.ThemeManager

@Composable
fun ColorSelectionDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    currentSelection: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Theme Color",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeManager.colorPalettes.forEachIndexed { index, palette ->
                    val isSelected = currentSelection == index
                    val bgColor = if (ThemeManager.isDarkTheme) palette.darkPrimary else palette.lightPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelected(index) }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = getColorName(index),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (index < ThemeManager.colorPalettes.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

private fun getColorName(index: Int): String {
    return when(index) {
        0 -> "Purple"
        1 -> "Blue"
        2 -> "Green"
        3 -> "Orange"
        4 -> "Red"
        5 -> "Teal"
        else -> "Custom ${index + 1}"
    }
}