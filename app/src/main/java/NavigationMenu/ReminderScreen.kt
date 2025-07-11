package NavigationMenu

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
import android.R
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.ui.res.painterResource
import androidx.room.Room
import com.example.mindscribe.data.ReminderDatabase
import com.example.mindscribe.viewmodel.ReminderViewModel
import com.example.mindscribe.viewmodel.ReminderViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.mindscribe.model.Reminder
import com.example.mindscribe.repository.ReminderRepository
import com.example.mindscribe.util.NotificationScheduler


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(navController: NavController, reminderViewModel: ReminderViewModel = viewModel()) {

    val context = LocalContext.current

    // Initialize database and repository
    val reminderDb = remember {
        Room.databaseBuilder(
            context,
            ReminderDatabase::class.java,
            "reminder_database"
        ).build()
    }
    val repository = remember { ReminderRepository(reminderDb.reminderDao()) }

    // Get ViewModel with factory
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(repository)
    )
    val reminders by viewModel.reminders.collectAsState(initial = emptyList())
    val notificationScheduler = remember { NotificationScheduler(context) }

    // State for new reminder
    var newReminderTitle by remember { mutableStateOf("") }
    var newReminderDescription by remember { mutableStateOf("") }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
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
                        Icon(imageVector = Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Back")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.mindscribe.R.drawable.reminder),
                            contentDescription = "No Reminders",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No reminders set!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap '+' to add your first reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
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
                                // Date selection - using derivedStateOf for better performance
                                val dateText by remember(selectedCalendar) {
                                    derivedStateOf {
                                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                            .format(selectedCalendar.time)
                                    }
                                }
                                Text(
                                    text = dateText,
                                    modifier = Modifier.clickable {
                                        showDatePicker(context, selectedCalendar) { newDate ->
                                            selectedCalendar = newDate
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Time selection - using derivedStateOf for better performance
                                val timeText by remember(selectedCalendar) {
                                    derivedStateOf {
                                        SimpleDateFormat("hh:mm a", Locale.getDefault())
                                            .format(selectedCalendar.time)
                                    }
                                }
                                Text(
                                    text = timeText,
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
                                        val addedReminder = reminders.lastOrNull { it.timestamp == reminderTime && it.title == newReminderTitle.trim() }
                                        addedReminder?.let {
                                            notificationScheduler.scheduleReminder(it)
                                        } ?: run {

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