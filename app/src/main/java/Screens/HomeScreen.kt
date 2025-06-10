package Screens

import NoteViewModel.AuthViewModel
import NoteViewModel.NoteViewModel
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import backend.Note
import com.example.mindscribe.ui.components.NavigationDrawerContent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    noteViewModel: NoteViewModel,
    onAccountClick: () -> Unit,
    authViewModel: AuthViewModel? = null
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var selectedItem by remember { mutableStateOf("home") }
    var searchText by remember { mutableStateOf("") }
    val notes by noteViewModel.activeNotes.observeAsState(emptyList())
    val currentUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoading by noteViewModel.uiState.collectAsState()

    // Handle search text changes with debounce
    LaunchedEffect(searchText) {
        noteViewModel.search(searchText)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState,
                selectedItem = selectedItem,  // Now passing String
                onItemSelected = { newItem -> selectedItem = newItem },  // Handle selection changes
                currentRoute = "home"
            )
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }

                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxHeight(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (searchText.isEmpty()) {
                                                Text(
                                                    "Search",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 18.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                IconButton(onClick = onAccountClick) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Account",
                                        tint = if (currentUser != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("note/-1") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Add Note")
                    }
                }
            ) { innerPadding ->
                if (isLoading.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (notes.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val message = if (searchText.isBlank()) "You have no active notes"
                                    else "No notes found for '$searchText'"
                                    Text(message, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        } else {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { navController.navigate("note/${note.id}") },
                                    onDelete = { noteViewModel.delete(it) },
                                    onTogglePin = { noteViewModel.togglePin(it) },
                                    onToggleArchive = { noteViewModel.toggleArchive(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleArchive: (Note) -> Unit
) {
    val displayDescription = if (note.noteDesc.isBlank()) "No text" else note.noteDesc
    val formattedDateTime = remember(note.timestamp) {
        SimpleDateFormat("MMMM d, hh:mm a", Locale.getDefault()).format(Date(note.timestamp))
    }
    val cardBackgroundColor = colorResource(id = note.colorResId)
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxHeight()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showOptionsMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.noteTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!note.audioPath.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Contains Audio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (!note.imageUrls.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Contains Image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (note.noteTitle.isNotBlank() || note.noteDesc.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "Contains Text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            // Long press menu options
            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false },
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                    onClick = {
                        onTogglePin(note)
                        showOptionsMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin
                            else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin"
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isArchived) "Unarchive Note" else "Archive Note") },
                    onClick = {
                        onToggleArchive(note)
                        showOptionsMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Archive,
                            contentDescription = "Archive"
                        )
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onDelete(note)
                        showOptionsMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}