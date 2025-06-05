// viewmodel/NoteViewModelFactory.kt
// src/main/java/com/example/mindscribe/viewmodel/NoteViewModelFactory.kt
package NoteViewModel // Or whatever your correct package is for NoteViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import Repo.NoteRepository
import NoteViewModel.NoteViewModel // Import the actual NoteViewModel class

// The factory must now also accept the userId
class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val userId: String // <-- NEW: Add userId to the factory's constructor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass both repository AND userId to the NoteViewModel constructor
            return NoteViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}