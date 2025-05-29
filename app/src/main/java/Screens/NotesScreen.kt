package com.example.mindscribe.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController
import com.example.mindscribe.RequestAudioPermission
import ui.components.MediaRecorderHelper
import java.util.*

@Composable
fun NotesScreen(navController: NavController) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(true) }

    RequestAudioPermission {
        val recorder = remember { MediaRecorderHelper(context) }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -300 })
    ) {
        Scaffold(
            topBar = { NotesTopAppBar(navController, onExit = { isVisible = false }) },
            bottomBar = { NotesBottomAppBar(context) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                var titleText by remember { mutableStateOf("") }
                var noteText by remember { mutableStateOf("") }

                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = TextStyle(fontSize = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (titleText.isEmpty()) {
                                Text("Title", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    }
                )

                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    textStyle = TextStyle(fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteText.isEmpty()) {
                                Text("Note", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopAppBar(navController: NavController, onExit: () -> Unit) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // 📂 File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        uri?.let {
            Toast.makeText(context, "File Selected: $it", Toast.LENGTH_LONG).show()
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            Toast.makeText(context, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    TopAppBar(
        title = { Text("Notes") },
        navigationIcon = {
            IconButton(onClick = {
                onExit()
                navController.popBackStack()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick Date")
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Date") },
                    onClick = {
                        menuExpanded = false
                        Toast.makeText(context, "Sorting by Date", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Name") },
                    onClick = {
                        menuExpanded = false
                        Toast.makeText(context, "Sorting by Name", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomAppBar(context: Context) {
    val recorder = remember { MediaRecorderHelper(context) }
    val isRecording = remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Image Selected: $it", Toast.LENGTH_SHORT).show()
        }
    }

    BottomAppBar(
        actions = {
            IconButton(onClick = { /* Save Note Logic */ }) {
                Icon(Icons.Filled.Check, contentDescription = "Save Note")
            }
            IconButton(onClick = { /* Edit Note Logic */ }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Note")
            }
            IconButton(onClick = {
                if (isRecording.value) {
                    recorder.stopRecording()
                } else {
                    recorder.startRecording()
                }
                isRecording.value = !isRecording.value
            }) {
                Icon(
                    imageVector = if (isRecording.value) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording.value) "Stop Recording" else "Start Recording"
                )
            }
            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Filled.Image, contentDescription = "Insert Image")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* New Note Logic */ },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Add, "Add New Note")
            }
        }
    )
}
