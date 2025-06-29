package Repo

import Database.NoteDao
import android.util.Log
import backend.Note
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val firestoreRepo: FirestoreRepository
) {
    private companion object {
        const val TAG = "NoteRepository"
        const val BATCH_SIZE = 50 // Reduced batch size for better reliability
        const val MAX_RETRIES = 3
    }

    // Local DB operations remain mostly the same
    fun getAllNotesForUser(userId: String): Flow<List<Note>> =
        noteDao.getAllNotesForUser(userId)
            .catch { e -> Log.e(TAG, "Local DB read error", e) }

    suspend fun getNoteById(id: String): Note? =
        noteDao.getNoteById(id)

    suspend fun insert(note: Note) {
        val noteToSave = note.copy(
            timestamp = if (note.timestamp == 0L) System.currentTimeMillis() else note.timestamp
        )
        noteDao.insert(noteToSave)
        if (noteToSave.userId != "guest") {
            firestoreRepo.upsertNote(noteToSave, noteToSave.userId)
        }
    }

    suspend fun update(note: Note) {
        val updatedNote = note.copy(timestamp = System.currentTimeMillis())
        noteDao.update(updatedNote)
        if (updatedNote.userId != "guest") {
            firestoreRepo.upsertNote(updatedNote, updatedNote.userId)
        }
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
        if (note.userId != "guest") {
            firestoreRepo.deleteNote(note.id)
        }
    }

    // Improved sync implementation
    suspend fun syncWithCloud(userId: String, onProgress: (Int) -> Unit = {}) {
        if (userId == "guest") return

        try {
            Log.d(TAG, "Starting optimized sync for user: $userId")

            // 1. Get all notes in batches
            val localNotes = noteDao.getAllNotesForUser(userId).first()
            val cloudNotes = firestoreRepo.getNotesByUser(userId).first()

            // 2. Prepare sync operations
            val operations = prepareSyncOperations(localNotes, cloudNotes, userId)

            // 3. Execute in batches with progress updates
            executeSyncOperations(operations, onProgress)

            Log.d(TAG, "Sync completed successfully for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for user: $userId", e)
            throw Exception("Sync failed: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun prepareSyncOperations(
        localNotes: List<Note>,
        cloudNotes: List<Note>,
        userId: String
    ): SyncOperations {
        val operations = SyncOperations()

        // Find notes that need pushing to cloud
        localNotes.forEach { localNote ->
            val cloudNote = cloudNotes.find { it.id == localNote.id }
            if (cloudNote == null || localNote.timestamp > cloudNote.timestamp) {
                operations.addUpload(localNote)
            }
        }

        // Find notes that need pulling to local
        cloudNotes.forEach { cloudNote ->
            val localNote = localNotes.find { it.id == cloudNote.id }
            if (localNote == null || cloudNote.timestamp > localNote.timestamp) {
                operations.addDownload(cloudNote)
            }
        }

        return operations
    }

    private suspend fun executeSyncOperations(
        operations: SyncOperations,
        onProgress: (Int) -> Unit
    ) {
        val totalOperations = operations.upload.size + operations.download.size
        var completedOperations = 0

        // Process uploads in batches
        operations.upload.chunked(BATCH_SIZE).forEach { batch ->
            batch.forEach { note ->
                tryWithRetry(MAX_RETRIES) {
                    firestoreRepo.upsertNote(note, note.userId)
                    completedOperations++
                    onProgress(completedOperations * 100 / totalOperations)
                }
            }
        }

        // Process downloads in batches
        operations.download.chunked(BATCH_SIZE).forEach { batch ->
            noteDao.insertAll(batch) // Single transaction
            completedOperations += batch.size
            onProgress(completedOperations * 100 / totalOperations)
        }
    }

    private suspend fun <T> tryWithRetry(maxRetries: Int, block: suspend () -> T): T {
        var retryCount = 0
        var lastError: Exception? = null

        while (retryCount <= maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                retryCount++
                if (retryCount <= maxRetries) {
                    delay(2000L * retryCount) // Exponential backoff
                }
            }
        }

        throw lastError ?: Exception("Unknown error after retries")
    }

    private class SyncOperations {
        val upload = mutableListOf<Note>()
        val download = mutableListOf<Note>()

        fun addUpload(note: Note) { upload.add(note) }
        fun addDownload(note: Note) { download.add(note) }
    }
}