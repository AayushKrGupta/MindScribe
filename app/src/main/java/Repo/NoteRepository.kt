package Repo

import Database.NoteDao
import android.util.Log
import backend.Note

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val firestoreRepo: FirestoreRepository
) {
    private companion object {
        const val TAG = "NoteRepository"
    }

    // Local DB operations
    fun getAllNotesForUser(userId: String): Flow<List<Note>> =
        noteDao.getAllNotesForUser(userId)
            .catch { e -> Log.e(TAG, "Local DB read error", e) }

    suspend fun getNoteById(id: String): Note? =
        noteDao.getNoteById(id)

    suspend fun insert(note: Note) {
        val noteToSave = note.copy(
            timestamp = if (note.timestamp == 0L) System.currentTimeMillis() else note.timestamp
        )

        try {
            // Local first strategy
            noteDao.insert(noteToSave)

            // Sync to cloud if authenticated
            if (noteToSave.userId != "guest") {
                firestoreRepo.upsertNote(noteToSave, noteToSave.userId)
                    .also { Log.d(TAG, "Note upserted to Firestore: ${noteToSave.id}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert failed", e)
            throw Exception("Failed to save note")
        }
    }

    suspend fun update(note: Note) {
        val updatedNote = note.copy(timestamp = System.currentTimeMillis())

        try {
            noteDao.update(updatedNote)
            if (updatedNote.userId != "guest") {
                firestoreRepo.upsertNote(updatedNote, updatedNote.userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update failed", e)
            throw Exception("Failed to update note")
        }
    }

    suspend fun delete(note: Note) {
        try {
            noteDao.delete(note)
            if (note.userId != "guest") {
                firestoreRepo.deleteNote(note.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            throw Exception("Failed to delete note")
        }
    }

    // Search functionality
    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>> =
        noteDao.searchNotesForUser(query, userId)
            .catch { e -> Log.e(TAG, "Search failed", e) }

    // Sync improvements
    suspend fun syncWithCloud(userId: String) {
        if (userId == "guest") return

        try {
            Log.d(TAG, "Starting sync for user: $userId")

            // 1. Push local changes to cloud
            noteDao.getAllNotesForUser(userId).collect { localNotes ->
                localNotes.forEach { localNote ->
                    try {
                        val cloudNote = firestoreRepo.getNoteById(localNote.id)
                        if (cloudNote == null || localNote.timestamp > cloudNote.timestamp) {
                            firestoreRepo.upsertNote(localNote, userId)
                            Log.d(TAG, "Pushed local note to cloud: ${localNote.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to push note ${localNote.id}", e)
                    }
                }
            }

            // 2. Pull cloud changes to local
            firestoreRepo.getNotesByUser(userId).collect { cloudNotes ->
                cloudNotes.forEach { cloudNote ->
                    try {
                        val localNote = noteDao.getNoteById(cloudNote.id)
                        if (localNote == null || cloudNote.timestamp > localNote.timestamp) {
                            noteDao.insert(cloudNote)
                            Log.d(TAG, "Pulled cloud note to local: ${cloudNote.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to pull note ${cloudNote.id}", e)
                    }
                }
            }

            Log.d(TAG, "Sync completed for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for user: $userId", e)
            throw Exception("Sync failed: ${e.message}")
        }
    }
}