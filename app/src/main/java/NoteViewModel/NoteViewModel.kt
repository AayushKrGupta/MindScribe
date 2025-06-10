package NoteViewModel

import Repo.NoteRepository
import android.util.Log
import androidx.lifecycle.*
import backend.Note
import com.example.mindscribe.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteViewModel @Inject constructor(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    internal val userId get() = auth.currentUser?.uid ?: "guest"

    private val _searchTextFlow = MutableStateFlow("")
    private val _forceRefresh = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState = _uiState

    private val _allNotesFlow = combine(
        localRepo.getAllNotesForUser(userId),
        if (userId != "guest") firestoreRepo.getNotesByUser(userId) else flowOf(emptyList()),
        _forceRefresh
    ) { localNotes, cloudNotes, _ ->
        mergeNotes(localNotes, cloudNotes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allNotes = _allNotesFlow

    val activeNotes = allNotes
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
        }
        .asLiveData()

    val archivedNotes = allNotes
        .map { notes ->
            notes.filter { it.isArchived }
                .sortedByDescending { it.timestamp }
        }
        .asLiveData()

    // Add this in the init block after the existing code
    init {
        if (userId != "guest") {
            viewModelScope.launch {
                try {
                    // First push local notes to Firestore
                    localRepo.getAllNotesForUser(userId).collect { localNotes ->
                        localNotes.forEach { note ->
                            firestoreRepo.upsertNote(note, userId)
                        }
                    }

                    // Then pull from Firestore to ensure latest versions
                    firestoreRepo.getNotesByUser(userId).collect { cloudNotes ->
                        cloudNotes.forEach { cloudNote ->
                            localRepo.insert(cloudNote)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NoteViewModel", "Sync error", e)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Sync error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun mergeNotes(localNotes: List<Note>, cloudNotes: List<Note>): List<Note> {
        return (localNotes + cloudNotes)
            .groupBy { it.id }
            .map { (_, notesWithSameId) ->
                notesWithSameId.maxByOrNull { it.timestamp } ?: notesWithSameId.first()
            }
    }

    fun search(query: String) { _searchTextFlow.value = query }
    fun clearToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }
    suspend fun getNoteById(id: String): Note? = localRepo.getNoteById(id)

    fun insert(note: Note) = viewModelScope.launch {
        val noteWithUser = note.copy(
            userId = if (note.userId.isBlank()) userId else note.userId,
            timestamp = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            // 1. Save locally first for immediate UI update
            localRepo.insert(noteWithUser)
            _forceRefresh.value = !_forceRefresh.value

            // 2. Sync to Firestore in background
            firestoreRepo.upsertNote(noteWithUser, noteWithUser.userId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                toastMessage = "Note saved successfully"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                toastMessage = "Error saving note: ${e.message}"
            )
            // Fallback - ensure local save
            localRepo.insert(noteWithUser)
            _forceRefresh.value = !_forceRefresh.value
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            localRepo.delete(note)
            _forceRefresh.value = !_forceRefresh.value
            firestoreRepo.deleteNote(note.id)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                toastMessage = "Note deleted successfully"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                toastMessage = "Error deleting note: ${e.message}"
            )
            localRepo.delete(note)
            _forceRefresh.value = !_forceRefresh.value
        }
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(
            isPinned = !note.isPinned,
            isArchived = false,
            timestamp = System.currentTimeMillis()
        )
        insert(updatedNote)
    }

    fun toggleArchive(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(
            isArchived = !note.isArchived,
            isPinned = false,
            timestamp = System.currentTimeMillis()
        )
        insert(updatedNote)
    }

    data class NoteUiState(
        val toastMessage: String? = null,
        val isLoading: Boolean = false,
        val syncStatus: String? = null
    )

    class Factory @Inject constructor(
        private val localRepo: NoteRepository,
        private val firestoreRepo: FirestoreRepository,
        private val auth: FirebaseAuth
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteViewModel(localRepo, firestoreRepo, auth) as T
    }
}