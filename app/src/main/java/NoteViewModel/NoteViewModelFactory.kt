// src/main/java/com/example/mindscribe/viewmodel/NoteViewModelFactory.kt
package NoteViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import Repo.NoteRepository
import com.example.mindscribe.repository.FirestoreRepository // Import FirestoreRepository
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth

// The factory must now accept all dependencies NoteViewModel needs
class NoteViewModelFactory(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository, // NEW: Add FirestoreRepository
    private val firebaseAuth: FirebaseAuth // NEW: Add FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            // Pass all three dependencies to the NoteViewModel constructor
            return NoteViewModel(localRepo, firestoreRepo, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}