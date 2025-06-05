package NoteViewModel

import Repo.NoteRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import backend.Note
import com.example.mindscribe.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map // Make sure this is imported
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteViewModel @Inject constructor(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    internal val userId get() = auth.currentUser?.uid ?: "guest"

    private val _searchTextFlow = MutableStateFlow("")

    // UI State
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState = _uiState

    // Notes flow combining local and Firestore data
    private val _allNotesFlow = combine(
        localRepo.getAllNotesForUser(userId),
        firestoreRepo.getNotesByUser(userId)
    ) { localNotes, cloudNotes ->
        // Merge strategy: Cloud notes override local ones based on ID.
        // For notes with the same ID, prefer the one with a newer timestamp or the cloud version.
        (localNotes + cloudNotes)
            .groupBy { it.id }
            .map { (_, notesWithSameId) ->
                notesWithSameId.maxByOrNull { it.timestamp } ?: notesWithSameId.first()
            }
    }

    val activeNotes: LiveData<List<Note>> = _allNotesFlow
        .combine(_searchTextFlow) { notes, searchText ->
            notes.filter { !it.isArchived } // Filter out archived notes from active view
                .filter {
                    searchText.isBlank() ||
                            it.noteTitle.contains(searchText, ignoreCase = true) ||
                            it.noteDesc.contains(searchText, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
        }.asLiveData()

    // --- ADDED THIS SECTION FOR ARCHIVED NOTES ---
    val archivedNotes: LiveData<List<Note>> = _allNotesFlow
        .map { notes ->
            notes.filter { it.isArchived } // Filter for archived notes
                .sortedByDescending { it.timestamp } // Sort archived notes by timestamp
        }.asLiveData()
    // ---------------------------------------------


    // Search function that updates the search text flow
    fun search(query: String) {
        _searchTextFlow.value = query
    }

    suspend fun getNoteById(id: String): Note? {
        return localRepo.getNoteById(id)
    }

    // Existing methods with Firestore integration
    fun insert(note: Note) = viewModelScope.launch {
        val noteWithUser = note.copy(userId = userId)
        try {
            // Write to Firestore first
            firestoreRepo.upsertNote(noteWithUser, userId)
            // Then write locally
            localRepo.insert(noteWithUser)
            _uiState.value = _uiState.value.copy(
                toastMessage = "Note saved to cloud"
            )
        } catch (e: Exception) {
            // Fallback to local only
            localRepo.insert(noteWithUser)
            _uiState.value = _uiState.value.copy(
                toastMessage = "Saved offline (will sync later)"
            )
        }
    }

    fun update(note: Note) = viewModelScope.launch {
        if (note.userId == userId) {
            try {
                firestoreRepo.upsertNote(note, userId)
                localRepo.update(note)
            } catch (e: Exception) {
                localRepo.update(note)
            }
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        if (note.userId == userId) {
            try {
                firestoreRepo.deleteNote(note.id)
                localRepo.delete(note)
                _uiState.value = _uiState.value.copy(toastMessage = "Note deleted from cloud")
            } catch (e: Exception) {
                localRepo.delete(note)
                _uiState.value = _uiState.value.copy(toastMessage = "Note deleted offline (will sync later)")
            }
            // If a note is deleted, ensure it's removed from both local and cloud view models
            // This is handled by the _allNotesFlow re-collection.
        }
    }


    // --- FUNCTIONS FOR PIN/ARCHIVE ---
    fun togglePin(note: Note) = viewModelScope.launch {
        if (note.userId == userId) {
            // When toggling pin, make sure it's not archived.
            // Also update timestamp to bring it to the top of active notes.
            val updatedNote = note.copy(
                isPinned = !note.isPinned,
                isArchived = false, // Unarchive if pinned
                timestamp = System.currentTimeMillis()
            )
            update(updatedNote) // Use the existing update logic
        }
    }

    fun toggleArchive(note: Note) = viewModelScope.launch {
        if (note.userId == userId) {
            // When toggling archive, also unpin it if it was pinned.
            // Update timestamp for sorting.
            val updatedNote = note.copy(
                isArchived = !note.isArchived,
                isPinned = false, // Unpin if archived
                timestamp = System.currentTimeMillis()
            )
            update(updatedNote) // Use the existing update logic
        }
    }
    // -------------------------------------


    data class NoteUiState(
        val toastMessage: String? = null,
        val isLoading: Boolean = false
    )

    class Factory @Inject constructor(
        private val localRepo: NoteRepository,
        private val firestoreRepo: FirestoreRepository,
        private val auth: FirebaseAuth
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteViewModel(localRepo, firestoreRepo, auth) as T
        }
    }
}