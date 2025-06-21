package Screens

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
import com.example.mindscribe.viewmodel.NoteViewModel
import java.util.UUID // Import UUID for generating local IDs

private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    noteViewModel: NoteViewModel, // ViewModel is provided by NavHost
    noteId: String? // Changed type to String?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = remember { noteViewModel.userId }

    // State variables
    var titleText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedImageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var recordingFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var currentNoteId by rememberSaveable { mutableStateOf(noteId ?: "") } // Store the ID here
    var selectedColorResId by rememberSaveable { mutableStateOf(R.color.note_color_default) }

    val recorder = remember { MediaRecorderHelper(context) }

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
    LaunchedEffect(noteId, currentUserId) { // Use noteId (String?) and currentUserId as keys
        Log.d(TAG, "NotesScreen: LaunchedEffect for noteId: $noteId, userId: $currentUserId")
        if (!noteId.isNullOrBlank()) { // Check if noteId is a valid non-empty string
            noteViewModel.getNoteById(noteId)?.let { note ->
                if (note.userId == currentUserId) {
                    Log.d(TAG, "NotesScreen: Loaded existing note: ${note.id}, Title: ${note.noteTitle}")
                    currentNoteId = note.id // Make sure the internal ID state is updated
                    titleText = note.noteTitle
                    noteText = note.noteDesc
                    note.imageUrls?.let { uriStringList ->
                        selectedImageUris = uriStringList.mapNotNull { uriString ->
                            try {
                                val uri = Uri.parse(uriString)
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                Log.d(TAG, "NotesScreen: Re-granted permission for URI: $uri")
                                uri
                            } catch (e: Exception) {
                                Log.e(TAG, "NotesScreen: Error parsing or re-granting permission for URI: $uriString - ${e.message}")
                                null
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
                    selectedColorResId = note.colorResId
                } else {
                    Log.w(TAG, "NotesScreen: Note $noteId found but does not belong to current user $currentUserId. Treating as new note.")
                    // Reset for new note creation
                    currentNoteId = ""
                    titleText = ""
                    noteText = ""
                    selectedImageUris = emptyList()
                    recordingFilePath = null
                    selectedColorResId = R.color.note_color_default
                }
            } ?: run {
                Log.d(TAG, "NotesScreen: Note with ID $noteId not found.")
                // If note not found, clear existing data
                currentNoteId = ""
                titleText = ""
                noteText = ""
                selectedImageUris = emptyList()
                recordingFilePath = null
                selectedColorResId = R.color.note_color_default
            }
        } else {
            // For new notes (noteId is null or blank), clear and prepare for new entry
            currentNoteId = "" // Ensure it's empty for a new note to trigger ID generation
            titleText = ""
            noteText = ""
            selectedImageUris = emptyList()
            recordingFilePath = null
            selectedColorResId = R.color.note_color_default
        }
    }

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

        // If currentNoteId is empty, generate a new one. Otherwise, use the existing one.
        val idToUse = if (currentNoteId.isEmpty()) UUID.randomUUID().toString() else currentNoteId

        val noteToSave = Note(
            id = idToUse, // Use the generated/existing String ID
            noteTitle = titleText.trim(),
            noteDesc = noteText.trim(),
            imageUrls = selectedImageUris.map { it.toString() },
            audioPath = recordingFilePath,
            colorResId = selectedColorResId,
            userId = currentUserId
            // isPinned and isArchived will use their default values if not explicitly handled
        )

        coroutineScope.launch {
            // NoteViewModel's insert method now handles both insert and update (upsert) logic
            noteViewModel.upsertNote(noteToSave)
            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
            isSaving = false
            navController.popBackStack()
        }
    }

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

                recordingFilePath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AudioPlayer(audioFilePath = path)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

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
            IconButton(onClick = onColorPickClick) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = "Pick Color",
                    tint = selectedColor
                )
            }

            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Filled.Image, contentDescription = "Add Image")
            }

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
                containerColor = colorResource(id = R.color.black), // Use the exact name from colors.xml
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()

            ) {
                Icon(Icons.Filled.Check, "Save Note")
            }
        }
    )
}