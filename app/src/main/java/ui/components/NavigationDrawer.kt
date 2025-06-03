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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.mindscribe.R

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    selectedItem: MutableState<String>,
    currentRoute: String // 📌 Pass current route from HomeScreen
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
        modifier = Modifier.fillMaxWidth(0.75f)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))

            // 🌟 App Logo
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color.Gray, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mind),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(65.dp)
                            .clip(CircleShape)
                            .clickable {
                                selectedItem.value = "home"
                                navController.navigate("home")
                            }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Main Drawer Items
            mainItems.forEach { (label, icon, route) ->
                val isSelected = when (label) {
                    "Note" -> currentRoute == "home" // ✅ Mark Note as active on HomeScreen
                    else -> selectedItem.value == route
                }

                if (label == "Note") {
                    // 🔹 "Note" is a collapsible menu, doesn't navigate
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = isSelected,
                        onClick = { isNoteExpanded = !isNoteExpanded }, // Toggle dropdown
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        badge = {
                            Icon(
                                imageVector = if (isNoteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        }
                    )

                    // 🔹 Show "Audio" & "Images" if expanded
                    if (isNoteExpanded) {
                        subItems.forEach { (subLabel, subIcon, subRoute) ->
                            NavigationDrawerItem(
                                icon = { Icon(subIcon, contentDescription = subLabel) },
                                label = { Text(subLabel) },
                                selected = selectedItem.value == subRoute,
                                onClick = {
                                    selectedItem.value = subRoute
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(start = 24.dp) // Indent sub-items
                            )
                        }
                    }
                } else {
                    // 🔹 Regular navigation items
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = isSelected,
                        onClick = {
                            selectedItem.value = route
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
