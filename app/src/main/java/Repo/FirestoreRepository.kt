package Repo
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first

class FirestoreRepository @Inject constructor() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val notesCollection = db.collection("notes")

    private companion object {
        const val TAG = "FirestoreRepository"
        const val MAX_RETRIES = 3
        const val BATCH_LIMIT = 500
    }
    suspend fun syncWithCloud(userId: String, localNotes: List<Note>) {
        try {
            val remoteNotes = getNotesByUser(userId).first()

            val notesToUpdate = mutableListOf<Note>()
            val notesToCreate = mutableListOf<Note>()
            val notesToDelete = mutableListOf<String>()

            localNotes.forEach { localNote ->
                remoteNotes.find { it.id == localNote.id }?.let { remoteNote ->
                    if (localNote.timestamp > remoteNote.timestamp) {
                        notesToUpdate.add(localNote)
                    }
                } ?: run {
                    notesToCreate.add(localNote)
                }
            }
            remoteNotes.forEach { remoteNote ->
                if (localNotes.none { it.id == remoteNote.id }) {
                    notesToDelete.add(remoteNote.id)
                }
            }
            val batches = listOf(
                notesToCreate.chunked(BATCH_LIMIT),
                notesToUpdate.chunked(BATCH_LIMIT),
                notesToDelete.chunked(BATCH_LIMIT)
            ).flatten()

            batches.forEach { batch ->
                when (batch.firstOrNull()) {
                    is Note -> {
                        val notesBatch = batch.filterIsInstance<Note>()
                        notesBatch.forEach { note ->
                            upsertNote(note, userId)
                        }
                    }
                    is String -> {
                        val idsBatch = batch.filterIsInstance<String>()
                        idsBatch.forEach { id ->
                            deleteNote(id)
                        }
                    }
                }
            }

            Log.d(TAG, "Sync completed: ${notesToCreate.size} created, " +
                    "${notesToUpdate.size} updated, ${notesToDelete.size} deleted")

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            throw Exception("Sync failed: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun upsertNote(note: Note, userId: String, retryCount: Int = 0) {
        try {
            val noteWithId = if (note.id.isEmpty()) {
                note.copy(id = generateId(), userId = userId)
            } else {
                note.copy(userId = userId)
            }

            // Ensure timestamp is set
            val noteToSave = if (noteWithId.timestamp == 0L) {
                noteWithId.copy(timestamp = System.currentTimeMillis())
            } else {
                noteWithId
            }

            notesCollection.document(noteToSave.id)
                .set(noteToMap(noteToSave))
                .await()

            Log.d(TAG, "Note upserted: ${noteToSave.id}")
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