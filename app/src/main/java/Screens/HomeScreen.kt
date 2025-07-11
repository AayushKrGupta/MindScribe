package Screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import backend.Note
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.mindscribe.R
import com.example.mindscribe.ui.components.NavigationDrawerContent
import com.example.mindscribe.viewmodel.NoteViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "NoteAppDebug"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    noteViewModel: NoteViewModel,
    onAccountClick: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    var selectedItem by remember { mutableStateOf("home") }
    var searchText by remember { mutableStateOf("") }
    val notes by noteViewModel.activeNotes.observeAsState(emptyList())
    val uiState by noteViewModel.uiState.collectAsState()
    val auth = FirebaseAuth.getInstance()

    val currentUser1 = auth.currentUser
    val profileImageUrl = currentUser1?.photoUrl?.toString()

    val currentUser by produceState<FirebaseUser?>(initialValue = noteViewModel.auth.currentUser) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            value = auth.currentUser
            if (value != null) {
                scope.launch {
                    noteViewModel.syncNotes()
                }
            }
        }
        noteViewModel.auth.addAuthStateListener(listener)
        awaitDispose {
            noteViewModel.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            noteViewModel.syncNotes()
        }
    }

    LaunchedEffect(searchText) {
        noteViewModel.search(searchText)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            noteViewModel.clearToast()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState,
                selectedItem = selectedItem,
                onItemSelected = { newItem -> selectedItem = newItem },
                currentRoute = "home"
            )
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = searchText,
                                        onValueChange = { searchText = it },
                                        textStyle = TextStyle(
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxHeight(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (searchText.isEmpty()) {
                                                    Text(
                                                        "Search",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = onAccountClick,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (!profileImageUrl.isNullOrEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = profileImageUrl,
                                                error = painterResource(id = R.drawable.userprofile)
                                            ),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = "Account",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                floatingActionButton = {
                    // --- FAB Modernization START ---
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("note/-1") },
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp,
                            focusedElevation = 8.dp,
                            hoveredElevation = 8.dp
                        ),
                        icon = {
                            Icon(
                                Icons.Filled.EditNote,
                                contentDescription = "Add Note"
                            )
                        },
                        text = { Text("Add Note") }
                    )

                    /*
                    // Alternative: SmallFloatingActionButton for a simpler circular FAB
                    SmallFloatingActionButton(
                        onClick = { navController.navigate("note/-1") },
                        modifier = Modifier
                            .size(56.dp) // Standard size for Small FAB
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = "Add Note",
                            modifier = Modifier.size(24.dp) // Smaller icon for Small FAB
                        )
                    }
                    */
                }
            ) { innerPadding ->
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
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Spacer(modifier = Modifier.height(250.dp))
                                    if (searchText.isBlank()) {
                                        Image(
                                            painter = painterResource(id = R.drawable.addnote),
                                            contentDescription = "Add Note",
                                            modifier = Modifier.size(215.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Empty Note!",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Add your first note to start...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        Text(
                                            "No notes found for '$searchText'",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
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
    val hasImage = !note.imageUrls.isNullOrEmpty()
    val cardHeight = if (hasImage) 300.dp else 150.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
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
                if (!note.imageUrls.isNullOrEmpty()) {
                    note.imageUrls?.firstOrNull()?.let { firstImageUrl ->
                        AsyncImage(
                            model = firstImageUrl,
                            contentDescription = "Note Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

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
                    if (hasImage) {
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

            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
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
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}