package com.example.mindscribe.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindscribe.ui.components.ThemeManager
import com.example.mindscribe.ui.components.ColorSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val themeManager = ThemeManager
    var showColorDialog by remember { mutableStateOf(false) }
    val githubUrl = "https://github.com/AayushKrGupta/MindScribe.git"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- APPEARANCE ---
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Dark Mode Toggle
            SettingsToggleOption(
                title = "Dark Mode",
                description = "Enable dark theme",
                icon = if (themeManager.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                checked = themeManager.isDarkTheme,
                onCheckedChange = {
                    themeManager.toggleTheme(context)
                    Toast.makeText(
                        context,
                        "Dark Mode ${if (it) "Enabled" else "Disabled"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Theme Color Selection
            SettingsClickableOption(
                title = "Theme Color",
                description = "Current: ${getCurrentColorName(themeManager.selectedColorScheme)}",
                icon = Icons.Default.Palette,
                onClick = { showColorDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- NOTES ---
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            var defaultSortOption by remember { mutableStateOf("Date Modified") }
            var fontSize by remember { mutableStateOf(16) }

            SettingsClickableOption(
                title = "Default Sort",
                description = "Current: $defaultSortOption",
                icon = Icons.Default.Sort,
                onClick = {
                    Toast.makeText(context, "Change sort preference", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsClickableOption(
                title = "Note Font Size",
                description = "Current: ${fontSize}sp",
                icon = Icons.Default.TextFields,
                onClick = {
                    Toast.makeText(context, "Adjust font size", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- DATA & STORAGE ---
            Text(
                text = "Data & Storage",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsClickableOption(
                title = "Backup Notes",
                description = "Export your notes data",
                icon = Icons.Default.Backup,
                onClick = {
                    Toast.makeText(context, "Backup notes", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsClickableOption(
                title = "Restore Notes",
                description = "Import from backup",
                icon = Icons.Default.Restore,
                onClick = {
                    Toast.makeText(context, "Restore notes", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- ABOUT ---
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsClickableOption(
                title = "Version",
                description = "1.0.0 (Build 20240101)",
                icon = Icons.Default.Info,
                onClick = {
                    Toast.makeText(context, "App version info", Toast.LENGTH_SHORT).show()
                },
                showArrow = false
            )

            SettingsClickableOption(
                title = "Source Code",
                description = "View on GitHub",
                icon = Icons.Default.Code,
                onClick = {
                    openUrlInBrowser(context, githubUrl)
                }
            )

            if (showColorDialog) {
                ColorSelectionDialog(
                    onDismiss = { showColorDialog = false },
                    onColorSelected = { index ->
                        themeManager.selectColorScheme(index, context)
                        showColorDialog = false
                        Toast.makeText(
                            context,
                            "${getCurrentColorName(index)} theme selected",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    currentSelection = themeManager.selectedColorScheme
                )
            }
        }
    }
}

@Composable
fun SettingsClickableOption(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggleOption(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private fun getCurrentColorName(index: Int): String {
    return when(index) {
        0 -> "Purple"
        1 -> "Blue"
        2 -> "Green"
        3 -> "Orange"
        else -> "Custom"
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening link", Toast.LENGTH_SHORT).show()
    }
}