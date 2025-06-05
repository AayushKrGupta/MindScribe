package Screens

import NoteViewModel.NoteViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import backend.Note
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ui.components.MediaRecorderHelper
import ui.components.AudioPlayer
import ui.components.ColorPaletteDialog
import kotlinx.coroutines.launch
import android.Manifest
import androidx.compose.runtime.saveable.rememberSaveable
import java.io.File
import com.example.mindscribe.R
import Database.NoteDao

private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    noteViewModel: NoteViewModel, // ViewModel is provided by NavHost
    noteId: Int // noteId is passed directly
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // <--- NEW: Get the userId from the ViewModel
    val currentUserId = remember { noteViewModel.userId }

    // State variables
    var titleText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedImageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var recordingFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    var selectedColorResId by rememberSaveable { mutableStateOf(R.color.note_color_default) }
    val recorder = remember { MediaRecorderHelper(context) }

    // Permission launcher (no change)
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "RECORD_AUDIO permission granted.")
            val path = recorder.startRecording()
            if (path != null) {
                recordingFilePath = path
                isRecording = true
                Toast.makeText(context, "Recording...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to start recording.", Toast.LENGTH_LONG).show()
                isRecording = false
            }
        } else {
            Toast.makeText(context, "Audio recording permission denied.", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    // Effect to load existing note data
    LaunchedEffect(noteId) {
        Log.d(TAG, "NotesScreen: LaunchedEffect for noteId: $noteId")
        if (noteId != null && noteId != -1) {
            // Call suspend function getNoteById inside coroutineScope
            noteViewModel.getNoteById(noteId)?.let { note ->
                Log.d(TAG, "NotesScreen: Loaded existing note: ${note.id}, Title: ${note.noteTitle}")
                titleText = note.noteTitle
                noteText = note.noteDesc
                note.imageUrls?.let { uriStringList ->
                    selectedImageUris = uriStringList.mapNotNull { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            // Attempt to re-take persistable URI permission if needed
                            // Note: This might throw SecurityException if the URI is no longer valid or app restarts
                            // without proper intent flags. Handling this gracefully is key.
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            Log.d(TAG, "NotesScreen: Re-granted permission for URI: $uri")
                            uri
                        } catch (e: Exception) {
                            Log.e(TAG, "NotesScreen: Error parsing or re-granting permission for URI: $uriString - ${e.message}")
                            null // Filter out invalid URIs
                        }
                    }
                    Log.d(TAG, "NotesScreen: Loaded image URIs: ${selectedImageUris.size}")
                } ?: run {
                    selectedImageUris = emptyList()
                }

                note.audioPath?.let { path ->
                    recordingFilePath = path
                    Log.d(TAG, "NotesScreen: Loaded audio path: $path")
                } ?: run {
                    recordingFilePath = null
                }
            } ?: Log.d(TAG, "NotesScreen: Note with ID $noteId not found.")
        } else {
            // For new notes, clear existing data
            titleText = ""
            noteText = ""
            selectedImageUris = emptyList()
            recordingFilePath = null
        }
    }

    // Clean up recorder (no change)
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                recorder.stopRecording()
            }
        }
    }

    fun saveOrUpdateNote() {
        if (titleText.isBlank()) {
            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val noteToSave = Note(
            id = if (noteId == -1) 0 else noteId,
            noteTitle = titleText.trim(),
            noteDesc = noteText.trim(),
            imageUrls = selectedImageUris.map { it.toString() },
            audioPath = recordingFilePath,
            colorResId = selectedColorResId,
            userId = currentUserId // <--- NEW: Pass the userId here
            // isPinned and isArchived will use their default values or be loaded from existing note if applicable
        )

        coroutineScope.launch {
            if (noteToSave.id == 0) {
                noteViewModel.insert(noteToSave)
                Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
            } else {
                noteViewModel.update(noteToSave)
                Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
            }
            isSaving = false
            navController.popBackStack()
        }
    }

    // Resolve the selected color resource ID to an actual Compose Color (no change)
    val resolvedSelectedColor = colorResource(id = selectedColorResId)

    Scaffold(
        topBar = {
            NotesTopAppBar(
                navController = navController,
                onExit = { navController.popBackStack() },
                backgroundColor = resolvedSelectedColor
            )
        },
        bottomBar = {
            NotesBottomAppBar(
                context = context,
                onSaveClick = { saveOrUpdateNote() },
                recorder = recorder,
                isRecording = isRecording,
                onToggleRecording = {
                    if (isRecording) {
                        val path = recorder.stopRecording()
                        if (path != null) {
                            recordingFilePath = path
                            Toast.makeText(context, "Recording saved!", Toast.LENGTH_SHORT).show()
                        }
                        isRecording = false
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onImagesSelected = { uris -> selectedImageUris = uris },
                onColorPickClick = { showColorPicker = true },
                selectedColor = resolvedSelectedColor
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(resolvedSelectedColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Input (no change)
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = TextStyle(fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface),
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
                    },
                    enabled = !isSaving
                )

                // Images (no change)
                selectedImageUris.forEachIndexed { index, uri ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .build()
                            ),
                            contentDescription = "Attached Image ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Audio Player (no change)
                recordingFilePath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AudioPlayer(audioFilePath = path)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Note Content (no change)
                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
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
                    },
                    enabled = !isSaving
                )
            }

            // Color Picker Dialog (no change)
            if (showColorPicker) {
                ColorPaletteDialog(
                    onColorSelected = { colorResId ->
                        selectedColorResId = colorResId
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }
        }
    }
}

// NotesTopAppBar and NotesBottomAppBar remain unchanged as they were correct.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopAppBar(
    navController: NavController,
    onExit: () -> Unit,
    backgroundColor: Color
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        ),
        title = { Text("Notes") },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
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
fun NotesBottomAppBar(
    context: Context,
    onSaveClick: () -> Unit,
    recorder: MediaRecorderHelper,
    isRecording: Boolean,
    onToggleRecording: (Boolean) -> Unit,
    onImagesSelected: (List<Uri>) -> Unit,
    onColorPickClick: () -> Unit,
    selectedColor: Color
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)

            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to take persistable permission for URI: $uri", e)
                }
            }
            Toast.makeText(context, "${uris.size} Images Selected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No Images Selected", Toast.LENGTH_SHORT).show()
        }
    }

    BottomAppBar(
        actions = {
            // Color Picker Button
            IconButton(onClick = onColorPickClick) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = "Pick Color",
                    tint = selectedColor
                )
            }

            // Image Button
            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Filled.Image, contentDescription = "Add Image")
            }

            // Record Button
            IconButton(onClick = { onToggleRecording(!isRecording) }) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = if (isRecording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Check, "Save Note")
            }
        }
    )
}