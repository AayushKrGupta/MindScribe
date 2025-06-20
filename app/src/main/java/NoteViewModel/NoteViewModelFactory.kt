package com.example.mindscribe.viewmodel

import Repo.NoteRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mindscribe.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class NoteViewModelFactory @Inject constructor(
    private val localRepo: NoteRepository,
    private val firestoreRepo: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(
                localRepo = localRepo,
                firestoreRepo = firestoreRepo,
                auth = firebaseAuth
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}