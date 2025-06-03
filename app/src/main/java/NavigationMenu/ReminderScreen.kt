// com.example.mindscribe.ui.screens/ReminderScreen.kt
package com.example.mindscribe.ui.screens

import Reminder.Reminder
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ui.components.showDatePicker
import ui.components.showTimePicker
import util.NotificationScheduler
import Reminder.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(navController: NavController, reminderViewModel: ReminderViewModel = viewModel()) {
    val context = LocalContext.current
    val reminders by reminderViewModel.reminders.collectAsState()
    val notificationScheduler = remember { NotificationScheduler(context) }

    // State for new reminder
    var newReminderTitle by remember { mutableStateOf("") }
    var newReminderDescription by remember { mutableStateOf("") }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) } // Calendar for date/time
    var showAddReminderDialog by remember { mutableStateOf(false) }

    // Request POST_NOTIFICATIONS permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Reminders may not show.", Toast.LENGTH_LONG).show()
        }
    }

    // Check and request permission on first launch of this screen (or when creating reminder)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddReminderDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reminders set. Tap '+' to add one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDelete = {
                                reminderViewModel.deleteReminder(it)
                                notificationScheduler.cancelReminder(it.id)
                                Toast.makeText(context, "Reminder deleted!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            if (showAddReminderDialog) {
                AlertDialog(
                    onDismissRequest = { showAddReminderDialog = false },
                    title = { Text("Add New Reminder") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newReminderTitle,
                                onValueChange = { newReminderTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newReminderDescription,
                                onValueChange = { newReminderDescription = it },
                                label = { Text("Description (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedCalendar.time),
                                    modifier = Modifier.clickable {
                                        showDatePicker(context, selectedCalendar) { newDate ->
                                            selectedCalendar = newDate
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedCalendar.time),
                                    modifier = Modifier.clickable {
                                        showTimePicker(context, selectedCalendar) { newTime ->
                                            selectedCalendar = newTime
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newReminderTitle.isNotBlank()) {
                                    val reminderTime = selectedCalendar.timeInMillis
                                    if (reminderTime > System.currentTimeMillis()) { // Only schedule future reminders
                                        reminderViewModel.addReminder(
                                            newReminderTitle.trim(),
                                            newReminderDescription.trim().takeIf { it.isNotBlank() },
                                            reminderTime
                                        )
                                        // Find the ID of the newly added reminder (hacky for in-memory, use Room ID for real app)
                                        // For now, we assume the last added reminder is the one we want to schedule
                                        val addedReminder = reminders.lastOrNull { it.timestamp == reminderTime && it.title == newReminderTitle.trim() }
                                        addedReminder?.let {
                                            notificationScheduler.scheduleReminder(it)
                                        } ?: run {
                                            // Fallback if we can't find the exact reminder (e.g., if multiple same titles)
                                            // In a Room DB, you'd get the ID back from the insert operation.
                                            Toast.makeText(context, "Reminder added but scheduling might fail if ID not retrieved correctly.", Toast.LENGTH_LONG).show()
                                        }

                                        Toast.makeText(context, "Reminder added!", Toast.LENGTH_SHORT).show()
                                        newReminderTitle = ""
                                        newReminderDescription = ""
                                        selectedCalendar = Calendar.getInstance()
                                        showAddReminderDialog = false
                                    } else {
                                        Toast.makeText(context, "Please select a future date and time.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddReminderDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReminderCard(reminder: Reminder, onDelete: (Reminder) -> Unit) {
    val formattedDateTime = remember(reminder.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        sdf.format(reminder.timestamp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                reminder.description?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminder Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onDelete(reminder) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Reminder")
            }
        }
    }
}