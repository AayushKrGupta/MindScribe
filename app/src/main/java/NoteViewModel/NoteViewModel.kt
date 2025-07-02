package com.example.mindscribe.viewmodel


import Repo.FirestoreRepository
import Repo.NoteRepository
import android.util.Log
import androidx.lifecycle.*
import backend.Note
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository,
    internal val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        const val TAG = "NoteViewModel"
        const val SYNC_TIMEOUT = 15_000L
        const val SYNC_COOLDOWN = 5_000L
        const val NOTES_DEBOUNCE = 300L
    }

    // Auth state
    internal val userId get() = auth.currentUser?.uid ?: "guest"

    // State flows
    private val _searchQuery = MutableStateFlow("")
    private val _forceRefresh = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(NoteUiState())
    private val _syncTrigger = Channel<Unit>(Channel.BUFFERED)

    val uiState = _uiState.asStateFlow()
    val syncTrigger = _syncTrigger.receiveAsFlow()

    private var lastSyncTime = 0L

    private val allNotesFlow = combine(
        localRepo.getAllNotesForUser(userId),
        if (userId != "guest") firestoreRepo.getNotesByUser(userId).catch { e ->
            Log.e(TAG, "Error fetching cloud notes", e)
            emit(emptyList())
        } else flowOf(emptyList()),
        _forceRefresh
    ) { localNotes, cloudNotes, _ ->
        mergeNotes(localNotes, cloudNotes).also {
            Log.d(TAG, "Merged ${it.size} notes (local:${localNotes.size}, cloud:${cloudNotes.size}) for user $userId")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeNotes = allNotesFlow
        .combine(_searchQuery.debounce(NOTES_DEBOUNCE)) { notes, query ->
            notes.filterNot { it.isArchived }
                .filter { note -> matchesQuery(note, query) }
                .sortedWith(noteComparator)
        }
        .asLiveData()

    val archivedNotes = allNotesFlow
        .map { notes -> notes.filter { it.isArchived } }
        .asLiveData()

    init {
        setupAuthListener()
        // Initial sync check
        viewModelScope.launch {
            if (userId != "guest") {
                _syncTrigger.send(Unit)
            }
        }
    }

    private fun setupAuthListener() {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                viewModelScope.launch {
                    Log.d(TAG, "Auth state changed, user: ${user.uid}")
                    _syncTrigger.send(Unit) // Always trigger sync on auth change
                }
            } ?: run {
                Log.d(TAG, "User logged out")
            }
        }
    }

    private fun shouldSync(): Boolean {
        val shouldSync = System.currentTimeMillis() - lastSyncTime > SYNC_COOLDOWN
        Log.d(TAG, "Should sync: $shouldSync")
        return shouldSync
    }

    private fun matchesQuery(note: Note, query: String): Boolean {
        return query.isBlank() ||
                note.noteTitle.contains(query, ignoreCase = true) ||
                note.noteDesc.contains(query, ignoreCase = true)
    }

    private val noteComparator = compareByDescending<Note> { it.isPinned }
        .thenByDescending { it.timestamp }

    private fun mergeNotes(local: List<Note>, remote: List<Note>): List<Note> {
        val merged = (local + remote)
            .groupBy { it.id }
            .map { (_, notes) ->
                notes.maxByOrNull { it.timestamp }?.apply {
                    // Ensure user ID is set correctly
                    if (userId != "guest") {
                        this.userId = userId
                    }
                } ?: notes.first()
            }
            .sortedByDescending { it.timestamp }

        Log.d(TAG, "Merged notes count: ${merged.size}")
        return merged
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    suspend fun getNoteById(id: String): Note? {
        return localRepo.getNoteById(id) ?: if (userId != "guest") {
            firestoreRepo.getNoteById(id)
        } else null
    }

    fun syncNotes() {
        if (userId == "guest" || _uiState.value.isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateSyncState(true, "Syncing notes...")
                withTimeout(SYNC_TIMEOUT) {
                    performSync()
                }
                updateSyncState(false, toast = "Notes synced")
            } catch (e: Exception) {
                handleSyncError(e)
            }
        }
    }

    private suspend fun performSync() {
        localRepo.syncWithCloud(userId)
        _forceRefresh.update { !it }
        lastSyncTime = System.currentTimeMillis()
    }

    private fun updateSyncState(
        loading: Boolean,
        status: String? = null,
        toast: String? = null
    ) {
        _uiState.update {
            it.copy(
                isLoading = loading,
                syncStatus = status,
                toastMessage = toast
            )
        }
    }

    private fun handleSyncError(e: Exception) {
        Log.e(TAG, "Sync failed", e)
        updateSyncState(
            loading = false,
            status = "Sync failed",
        )
    }

    fun upsertNote(note: Note) = viewModelScope.launch {
        val noteWithUser = note.copy(
            userId = note.userId.ifBlank { userId },
            timestamp = System.currentTimeMillis()
        )

        updateSyncState(true)
        try {
            localRepo.insert(noteWithUser)
            _forceRefresh.update { !it }
            updateSyncState(false, toast = "Note saved")
        } catch (e: Exception) {
            Log.e(TAG, "Insert failed", e)
            updateSyncState(
                false,
                toast = "Error: ${e.message ?: "Failed to save note"}"
            )
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        updateSyncState(true)
        try {
            localRepo.delete(note)
            _forceRefresh.update { !it }
            updateSyncState(false, toast = "Note deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            updateSyncState(
                false,
                toast = "Error: ${e.message ?: "Failed to delete note"}"
            )
        }
    }

    fun togglePin(note: Note) = upsertNote(
        note.copy(
            isPinned = !note.isPinned,
            isArchived = false
        )
    )

    fun toggleArchive(note: Note) = upsertNote(
        note.copy(
            isArchived = !note.isArchived,
            isPinned = false
        )
    )

    data class NoteUiState(
        val toastMessage: String? = null,
        val isLoading: Boolean = false,
        val syncStatus: String? = null
    )
}