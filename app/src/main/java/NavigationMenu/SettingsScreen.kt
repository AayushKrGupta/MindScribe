package com.example.mindscribe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.*
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector // Import ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var defaultSortOption by remember { mutableStateOf("Date Modified") }
    var fontSize by remember { mutableStateOf(16) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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

            // --- GENERAL SETTINGS ---
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider()

            // Dark Mode Toggle
            SettingsToggleOption(
                title = "Dark Mode",
                description = "Enable or disable dark theme",
                icon = if (isDarkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                checked = isDarkModeEnabled,
                onCheckedChange = {
                    isDarkModeEnabled = it
                    // TODO: Implement actual theme change logic (e.g., update app theme)
                    Toast.makeText(context, "Dark Mode: ${if (it) "On" else "Off"}", Toast.LENGTH_SHORT).show()
                }
            )

            // Default Sorting Option
            SettingsClickableOption(
                title = "Default Note Sorting",
                description = "Current: $defaultSortOption",
                icon = Icons.Default.SortByAlpha,
                onClick = {
                    // TODO: Implement a dialog or new screen to select sorting options
                    Toast.makeText(context, "Open sorting options", Toast.LENGTH_SHORT).show()
                }
            )

            // Font Size Adjustment (Example with a basic clickable, could be a slider or dialog)
            SettingsClickableOption(
                title = "Note Font Size",
                description = "Current: ${fontSize}sp",
                icon = Icons.Default.Description,
                onClick = {
                    // TODO: Implement a dialog or slider to adjust font size
                    Toast.makeText(context, "Adjust font size (e.g., show slider)", Toast.LENGTH_SHORT).show()
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
            Divider()

            SettingsClickableOption(
                title = "Backup Notes",
                description = "Export your notes data",
                icon = Icons.Default.Storage,
                onClick = {
                    // TODO: Implement backup functionality (e.g., save to external storage, cloud)
                    Toast.makeText(context, "Initiate notes backup", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsClickableOption(
                title = "Restore Notes",
                description = "Import notes from a backup file",
                icon = Icons.Default.Restore,
                onClick = {
                    // TODO: Implement restore functionality
                    Toast.makeText(context, "Initiate notes restore", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsClickableOption(
                title = "Clear Cache",
                description = "Free up space by clearing temporary files",
                onClick = {
                    // TODO: Implement cache clearing logic
                    Toast.makeText(context, "Cache cleared!", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsClickableOption(
                title = "Clear All Data",
                description = "Delete all notes and app data permanently",
                onClick = {
                    // TODO: Implement a confirmation dialog and then clear all data
                    Toast.makeText(context, "Confirm to clear all data", Toast.LENGTH_SHORT).show()
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
            Divider()

            SettingsClickableOption(
                title = "Version",
                description = "1.0.0 (Build 20240101)", // Hardcoded for example, get from BuildConfig in real app
                icon = Icons.Default.Info,
                onClick = {
                    Toast.makeText(context, "App version details", Toast.LENGTH_SHORT).show()
                },
                showArrow = false // No navigation for just displaying info
            )
            SettingsClickableOption(
                title = "Open Source Licenses",
                description = "View licenses for libraries used",
                onClick = {
                    // TODO: Navigate to Android's default OSS license screen or your custom one
                    Toast.makeText(context, "Show open source licenses", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SettingsClickableOption(
    title: String,
    description: String? = null,
    icon: ImageVector? = null, // Changed type to ImageVector?
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
                    imageVector = icon, // Directly use icon here
                    contentDescription = null, // Content description for icon
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}


@Composable
fun SettingsToggleOption(
    title: String,
    description: String? = null,
    icon: ImageVector? = null, // Changed type to ImageVector?
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
                    imageVector = icon, // Directly use icon here
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}