// NoteViewModel.kt
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
import kotlinx.coroutines.Dispatchers // Make sure this is imported
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        localRepo.getAllNotesForUser(userId), // Data from local Room database
        firestoreRepo.getNotesByUser(userId)  // Data from Firestore (real-time Flow)
    ) { localNotes, cloudNotes ->
        // Merge strategy: Cloud notes override local ones based on ID.
        // For notes with the same ID, prefer the one with a newer timestamp or the cloud version.
        // This merge is for displaying the most up-to-date view.
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

    val archivedNotes: LiveData<List<Note>> = _allNotesFlow
        .map { notes ->
            notes.filter { it.isArchived } // Filter for archived notes
                .sortedByDescending { it.timestamp } // Sort archived notes by timestamp
        }.asLiveData()


    // --- THIS IS THE CRUCIAL PART TO ADD/CONFIRM ---
    init {
        // Observe Firestore notes and synchronize them to Room
        // This ensures Room is always up-to-date with Firestore data,
        // providing offline access and correct display on app restart.
        userId.let { currentUserId ->
            if (currentUserId != "guest") { // Only sync if a real user is logged in
                viewModelScope.launch(Dispatchers.IO) {
                    firestoreRepo.getNotesByUser(currentUserId).collect { cloudNotes ->
                        // Loop through notes from Firestore and insert/update them into Room
                        // 'insert' with OnConflictStrategy.REPLACE handles both new inserts and updates
                        cloudNotes.forEach { note ->
                            localRepo.insert(note)
                        }
                    }
                }
            }
        }
    }
    // --- END CRUCIAL PART ---


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
            // Then write locally to update UI immediately and ensure offline access
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
        }
    }


    // --- FUNCTIONS FOR PIN/ARCHIVE ---
    fun togglePin(note: Note) = viewModelScope.launch {
        if (note.userId == userId) {
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