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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteViewModel @Inject constructor(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    internal val userId get() = auth.currentUser?.uid ?: "guest"

    // Add this missing declaration
    private val _searchTextFlow = MutableStateFlow("")

    // UI State
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState = _uiState

    // Notes flow combining local and Firestore data
    private val _allNotesFlow = combine(
        localRepo.getAllNotesForUser(userId),
        firestoreRepo.getNotesByUser(userId)
    ) { localNotes, cloudNotes ->
        // Merge strategy: Cloud notes override local ones
        (localNotes + cloudNotes)
            .groupBy { it.id }
            .map { (_, notes) ->
                notes.maxByOrNull { it.timestamp } ?: notes.first()
            }
    }

    val activeNotes: LiveData<List<Note>> = _allNotesFlow
        .combine(_searchTextFlow) { notes, searchText ->
            notes.filter { !it.isArchived }
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

    // Search function that updates the search text flow
    fun search(query: String) {
        _searchTextFlow.value = query
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

    // Add this new data class
    data class NoteUiState(
        val toastMessage: String? = null,
        val isLoading: Boolean = false
    )

    // Factory remains similar but updated for DI
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