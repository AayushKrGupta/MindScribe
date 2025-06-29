package com.example.mindscribe.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var defaultSortOption by remember { mutableStateOf("Date Modified") }
    var fontSize by remember { mutableStateOf(16) }
    val githubUrl = "https://github.com/AayushKrGupta/MindScribe.git"

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
                    Toast.makeText(context, "Dark Mode: ${if (it) "On" else "Off"}", Toast.LENGTH_SHORT).show()
                }
            )

            // Font Size Adjustment
            SettingsClickableOption(
                title = "Note Font Size",
                description = "Current: ${fontSize}sp",
                icon = Icons.Default.Description,
                onClick = {
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
                    Toast.makeText(context, "Initiate notes backup", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsClickableOption(
                title = "Restore Notes",
                description = "Import notes from a backup file",
                icon = Icons.Default.Restore,
                onClick = {
                    Toast.makeText(context, "Initiate notes restore", Toast.LENGTH_SHORT).show()
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
                description = "1.0.0 (Build 20240101)",
                icon = Icons.Default.Info,
                onClick = {
                    Toast.makeText(context, "App version details", Toast.LENGTH_SHORT).show()
                },
                showArrow = false
            )
            SettingsClickableOption(
                title = "Open Source Licenses",
                description = "View licenses for libraries used",
                icon = Icons.Default.Info,
                onClick = {
                    openUrlInBrowser(context, githubUrl)
                }
            )
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

fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found to open the link", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}