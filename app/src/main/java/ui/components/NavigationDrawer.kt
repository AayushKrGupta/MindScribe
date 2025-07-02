package com.example.mindscribe.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.mindscribe.R

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    currentRoute: String
) {
    val scope = rememberCoroutineScope()
    var isNoteExpanded by remember { mutableStateOf(false) }

    val mainItems = listOf(
        Triple("Note", Icons.Default.NoteAlt, ""),
        Triple("Reminder", Icons.Default.CalendarMonth, "reminders"),
        Triple("Archive", Icons.Default.Archive, "archive"),
        Triple("Settings", Icons.Default.Settings, "settings"),
        Triple("About", Icons.Default.QuestionMark, "about")
    )

    val subItems = listOf(
        Triple("Audio", Icons.Default.AudioFile, "audio"),
        Triple("Images", Icons.Default.Image, "images")
    )

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.70f)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))

            // App Logo and Name Image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Gray, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mind),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(65.dp)
                            .clip(CircleShape)
                            .clickable {
                                onItemSelected("home")
                                scope.launch { drawerState.close() }
                                navController.navigate("home")
                            }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // App Name Image
                Image(
                    painter = painterResource(id = R.drawable.mindscribe),
                    contentDescription = "MindScribe",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(32.dp), // Adjust height as needed
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Drawer Items
            mainItems.forEach { (label, icon, route) ->
                val isSelected = when (label) {
                    "Note" -> currentRoute == "home"
                    else -> selectedItem == route
                }

                if (label == "Note") {
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = isSelected,
                        onClick = { isNoteExpanded = !isNoteExpanded },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        badge = {
                            Icon(
                                imageVector = if (isNoteExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        }
                    )

                    // Show sub-items if expanded
                    if (isNoteExpanded) {
                        subItems.forEach { (subLabel, subIcon, subRoute) ->
                            NavigationDrawerItem(
                                icon = { Icon(subIcon, contentDescription = subLabel) },
                                label = { Text(subLabel) },
                                selected = selectedItem == subRoute,
                                onClick = {
                                    onItemSelected(subRoute)
                                    scope.launch { drawerState.close() }
                                    navController.navigate(subRoute)
                                },
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = isSelected,
                        onClick = {
                            onItemSelected(route)
                            scope.launch { drawerState.close() }
                            navController.navigate(route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}