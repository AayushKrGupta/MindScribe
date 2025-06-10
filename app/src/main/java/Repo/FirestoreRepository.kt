package com.example.mindscribe.repository

import backend.Note

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRepository @Inject constructor() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val notesCollection = db.collection("notes")

    private fun generateId(): String = db.collection("temp").document().id

    suspend fun upsertNote(note: Note, userId: String) {
        try {
            val noteWithId = if (note.id.isEmpty()) {
                note.copy(id = generateId(), userId = userId)
            } else {
                note.copy(userId = userId)
            }
            notesCollection.document(noteWithId.id)
                .set(noteWithId)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to save note: ${e.message}")
        }
    }

    suspend fun deleteNote(noteId: String) {
        try {
            notesCollection.document(noteId).delete().await()
        } catch (e: Exception) {
            throw Exception("Failed to delete note: ${e.message}")
        }
    }

    fun getNotesByUser(userId: String): Flow<List<Note>> {
        return notesCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Note::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    companion object
}