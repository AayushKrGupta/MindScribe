package Screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import java.io.FileOutputStream
import com.example.mindscribe.R
import com.example.mindscribe.viewmodel.NoteViewModel
import java.util.UUID

private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    noteViewModel: NoteViewModel,
    noteId: String?
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
    var currentNoteId by rememberSaveable { mutableStateOf(noteId ?: "") }
    var selectedColorResId by rememberSaveable { mutableStateOf(R.color.note_color_default) }

    val recorder = remember { MediaRecorderHelper(context) }


    // Record audio permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val path = recorder.startRecording()
            recordingFilePath = path
            isRecording = path != null
            Toast.makeText(context, if (path != null) "Recording..." else "Failed to start recording",
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    // Add this function inside NotesScreen but before LaunchedEffect
    fun resetNoteState() {
        currentNoteId = ""
        titleText = ""
        noteText = ""
        selectedImageUris = emptyList()
        recordingFilePath = null
        selectedColorResId = R.color.note_color_default
    }
    // Note loading logic
    LaunchedEffect(noteId, currentUserId) {
        Log.d(TAG, "Loading note data for ID: $noteId")

        if (!noteId.isNullOrBlank()) {
            noteViewModel.getNoteById(noteId)?.let { note ->
                if (note.userId == currentUserId) {
                    currentNoteId = note.id
                    titleText = note.noteTitle
                    noteText = note.noteDesc
                    selectedColorResId = note.colorResId
                    recordingFilePath = note.audioPath

                    note.imageUrls?.let { uriStrings ->
                        selectedImageUris = uriStrings.mapNotNull { uriString ->
                            try {
                                Uri.parse(uriString)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } ?: run {
                        selectedImageUris = emptyList()
                    }
                } else {
                    resetNoteState()
                }
            } ?: run {
                resetNoteState()
            }
        } else {
            resetNoteState()
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

        // Process image URIs - copy Photo Picker content to app storage
        val savedImageUris = selectedImageUris.map { uri ->
            if (uri.toString().startsWith("content://media/picker")) {
                try {
                    val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Uri.fromFile(file).toString()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving image", e)
                    uri.toString()
                }
            } else {
                uri.toString()
            }
        }

        val idToUse = if (currentNoteId.isEmpty()) UUID.randomUUID().toString() else currentNoteId

        val noteToSave = Note(
            id = idToUse,
            noteTitle = titleText.trim(),
            noteDesc = noteText.trim(),
            imageUrls = savedImageUris,
            audioPath = recordingFilePath,
            colorResId = selectedColorResId,
            userId = currentUserId
        )

        coroutineScope.launch {
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
                        recorder.stopRecording()
                        isRecording = false
                        Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
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
                        AudioPlayer(
                            audioFilePath = path,
                            isRecording = isRecording
                        )
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
                    onClick = { menuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Name") },
                    onClick = { menuExpanded = false }
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
    // Photo Picker for Android 13+
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)
            Toast.makeText(context, "${uris.size} images selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Traditional file picker for older versions
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            try {
                uris.forEach { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                onImagesSelected(uris)
                Toast.makeText(context, "${uris.size} images selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error taking permissions", e)
            }
        }
    }

    fun pickImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            galleryLauncher.launch("image/*")
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

            IconButton(onClick = { pickImages() }) {
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
                containerColor = colorResource(id = R.color.black),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Check, "Save Note")
            }
        }
    )
}