// viewmodel/NoteViewModelFactory.kt
// This file should remain AS IS
// src/main/java/com/example/mindscribe/viewmodel/NoteViewModelFactory.kt
package NoteViewModel// Or whatever your correct package is

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import Repo.NoteRepository
import NoteViewModel.NoteViewModel // Import the actual NoteViewModel class

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}