package NoteViewModel

import Repo.NoteRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData // This import might not be strictly needed if only LiveData is exposed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // This import is for the Factory, but it's okay here
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import backend.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // This import might not be strictly needed if only LiveData is exposed
import kotlinx.coroutines.flow.asStateFlow // This import might not be strictly needed if only LiveData is exposed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _allNotesFlow = repository.allNotes
    private val _searchTextFlow = MutableStateFlow("")

    val activeNotes: LiveData<List<Note>> = _allNotesFlow
        .combine(_searchTextFlow) { notes, searchText ->
            notes
                .filter { !it.isArchived }
                .filter {
                    if (searchText.isBlank()) true
                    else it.noteTitle.contains(searchText, ignoreCase = true) ||
                            it.noteDesc.contains(searchText, ignoreCase = true)
                }
                .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.timestamp })
        }.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default) // Explicitly specify Dispatcher for LiveData

    val archivedNotes: LiveData<List<Note>> = _allNotesFlow
        .map { notes ->
            notes.filter { it.isArchived }
                .sortedByDescending { it.timestamp }
        }.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)


    fun search(query: String) {
        _searchTextFlow.value = query
    }

    // THIS IS THE MISSING FUNCTION THAT NEEDS TO BE ADDED BACK
    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }

    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }

    fun togglePin(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        val updatedNote = note.copy(isPinned = !note.isPinned)
        repository.update(updatedNote)
    }

    fun toggleArchive(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        val updatedNote = note.copy(isArchived = !note.isArchived, isPinned = false)
        repository.update(updatedNote)
    }
}