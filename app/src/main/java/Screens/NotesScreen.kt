package Screens

import android.Manifest
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController
import backend.Note // Assuming 'backend' is correct for Note data class
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ui.components.MediaRecorderHelper // Assuming 'ui.components' is correct
import ui.components.AudioPlayer // Assuming 'ui.components' is correct
import ui.components.ColorPaletteDialog // Assuming 'ui.components' is correct
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.example.mindscribe.R
import com.example.mindscribe.viewmodel.NoteViewModel // Assuming 'viewmodel' is correct
import java.util.UUID
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private const val TAG = "NoteAppDebug"

sealed class ListType {
    object Numbered : ListType()
    object Bullet : ListType()
    object Tick : ListType()

    companion object {
        fun fromString(value: String): ListType? {
            return when (value) {
                "Numbered" -> Numbered
                "Bullet" -> Bullet
                "Tick" -> Tick
                else -> null
            }
        }
    }
}

val ListTypeSaver = Saver<ListType?, String>(
    save = { listType ->
        when (listType) {
            is ListType.Numbered -> "Numbered"
            is ListType.Bullet -> "Bullet"
            is ListType.Tick -> "Tick"
            null -> "null"
        }
    },
    restore = { saved ->
        ListType.fromString(saved)
    }
)

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
    var noteTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedImageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var recordingFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var currentNoteId by rememberSaveable { mutableStateOf(noteId ?: "") }
    var selectedColorResId by rememberSaveable { mutableStateOf(R.color.note_color_default) }
    var currentListType by rememberSaveable(stateSaver = ListTypeSaver) { mutableStateOf<ListType?>(null) }

    val recorder = remember { MediaRecorderHelper(context) }

    val scrollState = rememberScrollState()
    val scrollCoroutineScope = rememberCoroutineScope()


    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val path = recorder.startRecording()
            recordingFilePath = path
            isRecording = path != null
            Toast.makeText(context,
                if (path != null) "Recording..." else "Failed to start recording",
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    fun resetNoteState() {
        currentNoteId = ""
        titleText = ""
        noteTextFieldValue = TextFieldValue("")
        selectedImageUris = emptyList()
        recordingFilePath = null
        selectedColorResId = R.color.note_color_default
        currentListType = null
    }


    LaunchedEffect(noteId, currentUserId) {
        Log.d(TAG, "Loading note data for ID: $noteId")

        if (!noteId.isNullOrBlank()) {
            noteViewModel.getNoteById(noteId)?.let { note ->
                if (note.userId == currentUserId) {
                    currentNoteId = note.id
                    titleText = note.noteTitle
                    noteTextFieldValue = TextFieldValue(note.noteDesc)
                    selectedColorResId = note.colorResId
                    recordingFilePath = note.audioPath

                    // Detect list type from existing text
                    currentListType = when {
                        note.noteDesc.lines().any { it.startsWith("• ") } -> ListType.Bullet
                        note.noteDesc.lines().any { it.matches("^\\d+\\.\\s.*".toRegex()) } -> ListType.Numbered
                        note.noteDesc.lines().any { it.startsWith("✓ ") } -> ListType.Tick
                        else -> null
                    }

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

        if (isSaving) return

        isSaving = true

        // --- MODIFIED CODE START ---
        // Changed to simply map the original Uri to String, relying on persistable URI permissions
        val savedImageUris = selectedImageUris.map { it.toString() }
        // --- MODIFIED CODE END ---

        val idToUse = if (currentNoteId.isEmpty()) UUID.randomUUID().toString() else currentNoteId

        val noteToSave = Note(
            id = idToUse,
            noteTitle = titleText.trim(),
            noteDesc = noteTextFieldValue.text.trim(),
            imageUrls = savedImageUris,
            audioPath = recordingFilePath,
            colorResId = selectedColorResId,
            userId = currentUserId,
            timestamp = System.currentTimeMillis()
        )

        coroutineScope.launch {
            try {
                noteViewModel.upsertNote(noteToSave)
                Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving note: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error saving note", e)
            } finally {
                isSaving = false
            }
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
                selectedColor = resolvedSelectedColor,
                onListFormatClick = { type ->
                    currentListType = type
                    val currentText = noteTextFieldValue.text
                    val newText: String
                    val newCursorPosition: Int

                    if (currentText.isEmpty() || currentText.isBlank()) {
                        val prefix = when (type) {
                            ListType.Numbered -> "1. "
                            ListType.Bullet -> "• "
                            ListType.Tick -> "✓ "
                            null -> ""
                        }
                        newText = prefix
                        newCursorPosition = prefix.length
                    } else {
                        val lines = currentText.split("\n").toMutableList()
                        val lastLineIndex = lines.lastIndex
                        val currentLine = lines.getOrElse(lastLineIndex) { "" }

                        when (type) {
                            ListType.Numbered -> {
                                if (!currentLine.matches("^\\d+\\.\\s.*".toRegex())) {
                                    val cleanedLine = currentLine.removePrefix("• ").removePrefix("✓ ")
                                    lines[lastLineIndex] = "1. $cleanedLine"
                                }
                            }
                            ListType.Bullet -> {
                                if (!currentLine.startsWith("• ")) {
                                    val cleanedLine = currentLine.replaceFirst("^\\d+\\.\\s".toRegex(), "").removePrefix("✓ ")
                                    lines[lastLineIndex] = "• $cleanedLine"
                                }
                            }
                            ListType.Tick -> {
                                if (!currentLine.startsWith("✓ ")) {
                                    val cleanedLine = currentLine.replaceFirst("^\\d+\\.\\s".toRegex(), "").removePrefix("• ")
                                    lines[lastLineIndex] = "✓ $cleanedLine"
                                }
                            }
                            null -> {
                                // Remove list formatting if a type was previously applied
                                lines[lastLineIndex] = currentLine
                                    .removePrefix("• ")
                                    .removePrefix("✓ ")
                                    .replaceFirst("^\\d+\\.\\s".toRegex(), "")
                            }
                        }
                        newText = lines.joinToString("\n")
                        newCursorPosition = newText.length
                    }
                    noteTextFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursorPosition))
                },
                currentListType = currentListType
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
                    .verticalScroll(scrollState)
            ) {
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = MaterialTheme.typography.headlineMedium.copy( // Using Material3 typography for title
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (titleText.isEmpty()) {
                                Text(
                                    "Title",
                                    style = MaterialTheme.typography.headlineMedium.copy( // Match placeholder style
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(30.dp))

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
                                    .data(uri) // Coil will load directly from the URI
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
                    value = noteTextFieldValue,
                    onValueChange = { newTextFieldValue ->
                        val oldText = noteTextFieldValue.text
                        val newText = newTextFieldValue.text

                        // Check if a new line was added (Enter key pressed)
                        if (currentListType != null && newText.length > oldText.length && newText.endsWith('\n')) {
                            val lines = newText.split("\n").toMutableList()
                            // The index of the line *before* the new empty line created by pressing Enter
                            val lastEffectiveLineIndex = lines.lastIndex - 1

                            // If there's no previous line (e.g., just entered a list and pressed enter immediately)
                            if (lastEffectiveLineIndex < 0) {
                                noteTextFieldValue = newTextFieldValue
                                return@BasicTextField
                            }

                            val previousLineContent = lines.getOrElse(lastEffectiveLineIndex) { "" }.trim()

                            // Determine if we should exit the list (user pressed Enter on an empty list item)
                            val shouldExitList = when (currentListType) {
                                ListType.Numbered -> previousLineContent.matches("^\\d+\\.\\s*$".toRegex())
                                ListType.Bullet -> previousLineContent == "•"
                                ListType.Tick -> previousLineContent == "✓"
                                null -> false // Should not happen here
                            }

                            if (shouldExitList) {
                                // If the previous line only contained the marker, remove it and disable list mode
                                lines[lastEffectiveLineIndex] = "" // Clear the marker
                                noteTextFieldValue = TextFieldValue(
                                    text = lines.joinToString("\n").trimEnd('\n'), // Remove trailing empty line if it resulted from clearing
                                    selection = TextRange(lines.joinToString("\n").trimEnd('\n').length)
                                )
                                currentListType = null // Exit list mode
                                return@BasicTextField
                            }

                            val extraNewline = if (currentListType == ListType.Numbered) "\n" else ""


                            val prefix = when (currentListType) {
                                ListType.Numbered -> {
                                    val match = "^(\\d+)\\.\\s.*".toRegex().find(previousLineContent)
                                    if (match != null) {
                                        val number = match.groupValues[1].toInt()
                                        "${number + 1}. "
                                    } else {
                                        // This case handles starting a numbered list or if a previous line was not numbered but currentListType is.
                                        "1. "
                                    }
                                }
                                ListType.Bullet -> "• "
                                ListType.Tick -> "✓ "
                                null -> "" // Should not happen as currentListType is checked
                            }

                            // The `lines` list currently has the old content + an empty new line from user Enter.
                            // We need to insert the extraNewline and the new prefix into the last empty line.
                            // The `lines.lastIndex` refers to the index of the newly added empty line.
                            lines[lines.lastIndex] = extraNewline + prefix

                            val finalString = lines.joinToString("\n")
                            // Position the cursor at the end of the newly inserted prefix
                            val newCursorPosition = finalString.length

                            noteTextFieldValue = TextFieldValue(
                                text = finalString,
                                selection = TextRange(newCursorPosition)
                            )
                            scrollCoroutineScope.launch {
                                scrollState.scrollTo(scrollState.maxValue)
                            }
                            return@BasicTextField
                        }
                        // Default update for any other text changes (e.g., typing normally)
                        noteTextFieldValue = newTextFieldValue
                        scrollCoroutineScope.launch {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    },
                    textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteTextFieldValue.text.isEmpty()) {
                                Text("Write your note here....", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// Rest of your code (NotesTopAppBar, NotesBottomAppBar, ListOptionItem) remains the same.
// ... (Your existing NotesTopAppBar, NotesBottomAppBar, ListOptionItem functions go here)
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
                Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Back")
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
    selectedColor: Color,
    onListFormatClick: (ListType?) -> Unit,
    currentListType: ListType?
) {
    // Photo Picker for Android 13+
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            try {
                uris.forEach { uri ->
                    // IMPORTANT: Take persistable URI permissions
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                onImagesSelected(uris)
                Toast.makeText(context, "${uris.size} images selected", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Photo Picker: Selected URIs: $uris") // Add logging
            } catch (e: Exception) {
                Log.e(TAG, "Photo Picker: Error taking permissions or processing URIs", e) // Log errors
                Toast.makeText(context, "Error selecting images: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    var isFormatMenuExpanded by remember { mutableStateOf(false) }
    var listFormatButtonX by remember { mutableStateOf(0) }
    var listFormatButtonY by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    BottomAppBar(
        actions = {
            // Pick Image Button
            IconButton(onClick = { pickImages() }) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = "Attach Image"
                )
            }

            // Record Audio Button
            IconButton(onClick = { onToggleRecording(isRecording) }) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                )
            }

            // Color Picker Button
            IconButton(onClick = onColorPickClick) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = "Pick Color",
                    tint = selectedColor
                )
            }

            // List Format Button - Now tracks its position
            IconButton(
                onClick = { isFormatMenuExpanded = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (currentListType != null) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        color = if (currentListType != null) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        },
                        shape = MaterialTheme.shapes.medium
                    )
                    .onGloballyPositioned { coordinates ->
                        // Get the position of the button on the screen
                        listFormatButtonX = coordinates.positionInWindow().x.toInt()
                        listFormatButtonY = coordinates.positionInWindow().y.toInt()
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatListBulleted,
                    contentDescription = "List Formatting Options",
                    tint = if (currentListType != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Custom Horizontal List Format Popup
            if (isFormatMenuExpanded) {
                Popup(
                    onDismissRequest = { isFormatMenuExpanded = false },
                    properties = PopupProperties(focusable = true),
                    // Position the popup relative to the button
                    // Adjust offset as needed for desired placement
                    offset = IntOffset(
                        x = listFormatButtonX - with(density) { 100.dp.roundToPx() }, // Adjust X to center or align
                        y = listFormatButtonY - with(density) { 80.dp.roundToPx() } // Adjust Y to be above the button
                    )
                ) {
                    // This Surface is the actual "card" for the dropdown
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Consistent rounding for the entire popup
                        tonalElevation = 8.dp, // Elevation for a subtle lift
                        color = MaterialTheme.colorScheme.surfaceContainerHigh, // Background color
                        modifier = Modifier
                            .shadow( // Apply shadow to this Surface
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp), // Shadow shape matches the surface
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .clip(RoundedCornerShape(16.dp)) // Ensure content is clipped by the shape
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp) // Padding inside the main container
                                .width(IntrinsicSize.Max), // Ensure row takes only necessary width
                            horizontalArrangement = Arrangement.spacedBy(4.dp) // Spacing between items
                        ) {
                            // Bullet List Option
                            ListOptionItem(
                                icon = {
                                    Text(
                                        "•",
                                        fontSize = 28.sp,
                                        modifier = Modifier.height(32.dp)
                                    )
                                },
                                label = "Bullet",
                                isSelected = currentListType is ListType.Bullet,
                                onClick = {
                                    onListFormatClick(ListType.Bullet)
                                    isFormatMenuExpanded = false
                                }
                            )

                            // Divider
                            androidx.compose.material3.Divider(
                                modifier = Modifier
                                    .height(64.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Numbered List Option
                            ListOptionItem(
                                icon = {
                                    Text(
                                        "1.",
                                        fontSize = 20.sp,
                                        modifier = Modifier.height(32.dp)
                                    )
                                },
                                label = "Numbered",
                                isSelected = currentListType is ListType.Numbered,
                                onClick = {
                                    onListFormatClick(ListType.Numbered)
                                    isFormatMenuExpanded = false
                                }
                            )

                            // Divider
                            androidx.compose.material3.Divider(
                                modifier = Modifier
                                    .height(64.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Tick List Option
                            ListOptionItem(
                                icon = {
                                    Text(
                                        "✓",
                                        fontSize = 28.sp,
                                        modifier = Modifier.height(32.dp)
                                    )
                                },
                                label = "Tick",
                                isSelected = currentListType is ListType.Tick,
                                onClick = {
                                    onListFormatClick(ListType.Tick)
                                    isFormatMenuExpanded = false
                                }
                            )

                            // Divider
                            androidx.compose.material3.Divider(
                                modifier = Modifier
                                    .height(64.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Clear List Format Option
                            ListOptionItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Close, // Using a generic clear icon
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = "Clear",
                                isSelected = currentListType == null,
                                onClick = {
                                    onListFormatClick(null) // Pass null to clear formatting
                                    isFormatMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSaveClick) {
                Icon(Icons.Filled.Save, "Save Note")
            }
        }
    )
}

@Composable
fun ListOptionItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)) // Clip individual items for ripple effect
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp), // Fixed size for consistent icon alignment
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}