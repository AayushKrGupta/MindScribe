package com.example.mindscribe.repository

import android.util.Log
import backend.Note
import com.example.mindscribe.R
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

    private companion object {
        const val TAG = "FirestoreRepository"
        const val MAX_RETRIES = 3
    }

    suspend fun upsertNote(note: Note, userId: String, retryCount: Int = 0) {
        try {
            val noteWithId = if (note.id.isEmpty()) {
                note.copy(id = generateId(), userId = userId)
            } else {
                note.copy(userId = userId)
            }

            notesCollection.document(noteWithId.id)
                .set(noteToMap(noteWithId))
                .await()

            Log.d(TAG, "Note upserted: ${noteWithId.id}")
        } catch (e: Exception) {
            if (retryCount < MAX_RETRIES) {
                Log.w(TAG, "Retrying upsert (attempt ${retryCount + 1})")
                upsertNote(note, userId, retryCount + 1)
            } else {
                Log.e(TAG, "Failed to upsert note", e)
                throw Exception("Network error. Please try again.")
            }
        }
    }

    suspend fun deleteNote(noteId: String) {
        try {
            notesCollection.document(noteId).delete().await()
            Log.d(TAG, "Note deleted: $noteId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete note", e)
            throw Exception("Couldn't delete note. Check connection.")
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
                        documentToNote(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse document ${doc.id}", e)
                        null
                    }
                }.also {
                    Log.d(TAG, "Fetched ${it.size} notes for user $userId")
                }
            }
    }

    suspend fun getNoteById(noteId: String): Note? {
        return try {
            val doc = notesCollection.document(noteId).get().await()
            if (doc.exists()) documentToNote(doc) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch note $noteId", e)
            null
        }
    }

    private fun generateId(): String = db.collection("temp").document().id

    private fun noteToMap(note: Note): HashMap<String, Any> = hashMapOf(
        "userId" to note.userId,
        "noteTitle" to note.noteTitle,
        "noteDesc" to note.noteDesc,
        "timestamp" to note.timestamp,
        "isPinned" to note.isPinned,
        "isArchived" to note.isArchived,
        "colorResId" to note.colorResId,
        "imageUrls" to (note.imageUrls ?: emptyList<String>()),
        "audioPath" to (note.audioPath ?: "")
    )

    private fun documentToNote(doc: com.google.firebase.firestore.DocumentSnapshot): Note {
        return Note(
            id = doc.id,
            userId = requireNotNull(doc.getString("userId")),
            noteTitle = doc.getString("noteTitle") ?: "",
            noteDesc = doc.getString("noteDesc") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            isPinned = doc.getBoolean("isPinned") ?: false,
            isArchived = doc.getBoolean("isArchived") ?: false,
            colorResId = doc.getLong("colorResId")?.toInt() ?: R.color.note_color_default,
            imageUrls = doc.get("imageUrls") as? List<String>,
            audioPath = doc.getString("audioPath")
        )
    }
}