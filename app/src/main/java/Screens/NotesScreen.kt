package Screens

import NoteViewModel.NoteViewModel
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.colorResource // Correct import for color resources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import backend.Note // Your Note data class
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ui.components.MediaRecorderHelper
import ui.components.AudioPlayer
import ui.components.ColorPaletteDialog // Your ColorPaletteDialog import
import kotlinx.coroutines.launch
import java.util.*
import android.Manifest
import androidx.compose.runtime.saveable.rememberSaveable
import java.io.File
import com.example.mindscribe.R // <--- IMPORTANT: Ensure this R import points to your app's R file


private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    noteViewModel: NoteViewModel = viewModel(),
    navBackStackEntry: NavBackStackEntry
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val noteId = navBackStackEntry.arguments?.getString("noteId")?.toIntOrNull()

    // State variables
    var titleText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedImageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var recordingFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Using R.color.note_color_7 as the default based on your Note data class
    var selectedColorResId by rememberSaveable { mutableStateOf(R.color.note_color_default) }
    val recorder = remember { MediaRecorderHelper(context) }

    // Permission launcher
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

    // Load existing note
    LaunchedEffect(noteId) {
        Log.d(TAG, "NotesScreen: LaunchedEffect for noteId: $noteId")
        if (noteId != null && noteId != -1) {
            noteViewModel.getNoteById(noteId)?.let { note ->
                Log.d(TAG, "NotesScreen: Loaded existing note: ${note.id}")
                titleText = note.noteTitle
                noteText = note.noteDesc
                selectedColorResId = note.colorResId // Load the color resource ID from the Note

                note.imageUrls?.let { uriStringList ->
                    selectedImageUris = uriStringList.mapNotNull { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            uri
                        } catch (e: Exception) {
                            null
                        }
                    }
                } ?: run { selectedImageUris = emptyList() }

                note.audioPath?.let { path ->
                    recordingFilePath = path
                }
            } ?: Log.d(TAG, "NotesScreen: Note with ID $noteId not found.")
        } else {
            // Reset for new notes
            titleText = ""
            noteText = ""
            selectedImageUris = emptyList()
            recordingFilePath = null
            selectedColorResId = R.color.note_color_default // Default color for new notes
        }
    }

    // Clean up recorder
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
            id = if (noteId == -1) 0 else noteId ?: 0,
            noteTitle = titleText.trim(),
            noteDesc = noteText.trim(),
            imageUrls = selectedImageUris.map { it.toString() },
            audioPath = recordingFilePath,
            colorResId = selectedColorResId // Save the color resource ID
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

    // Resolve the selected color resource ID to an actual Compose Color
    val resolvedSelectedColor = colorResource(id = selectedColorResId)

    Scaffold(
        topBar = {
            NotesTopAppBar(
                navController = navController,
                onExit = { navController.popBackStack() },
                backgroundColor = resolvedSelectedColor // Pass resolved color
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
                        isRecording = false // Stop recording regardless of path success for UI
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onImagesSelected = { uris -> selectedImageUris = uris },
                onColorPickClick = { showColorPicker = true },
                selectedColor = resolvedSelectedColor // Pass resolved color
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(resolvedSelectedColor) // Apply resolved color
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Input
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

                // Images
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

                // Audio Player
                recordingFilePath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AudioPlayer(audioFilePath = path)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Note Content
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

            // Color Picker Dialog
            if (showColorPicker) {
                ColorPaletteDialog(
                    onColorSelected = { colorResId ->
                        selectedColorResId = colorResId // Update the state with the resource ID
                        showColorPicker = false // Dismiss dialog after selection
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
    backgroundColor: Color // Expects a Compose Color
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor // Use the passed Compose Color
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
    selectedColor: Color // Expects a Compose Color
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // First, update the UI with all selected URIs (even if permissions aren't persisted yet)
            onImagesSelected(uris) // Update state with all selected URIs immediately

            // Then, attempt to take persistable URI permissions in the background or for long-term storage
            // This part is for *long-term access*, not for immediate display count.
            val successfullyPersistedUris = mutableListOf<Uri>()
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    successfullyPersistedUris.add(uri)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to take persistable permission for URI: $uri", e)
                    // If permission fails, we still want to show the image *now* if it's displayable
                    // But for *saving*, we might need to copy the file to app-private storage instead.
                    // For now, we'll let the original `uris` be used for `selectedImageUris` state.
                }
            }

            // Toast reflects the number of images the user *selected*,
            // not just those for which persistable permission was granted.
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
                    tint = selectedColor // Use the passed Compose Color
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

