// backend/NoteDatabase.kt
package Database // CHANGED: Assuming your database package is 'backend' for consistency

// CHANGED: Assuming NoteDao is in 'backend'
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // ADDED: Import TypeConverters
import backend.Note
import backend.Converters // ADDED: Import your Converters class

@Database(entities = [Note::class], version = 7, exportSchema = false) // 'exportSchema = false' is often added for dev
@TypeConverters(Converters::class) // ADDED: Tell Room to use your Converters
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .fallbackToDestructiveMigration() // This line handles schema changes by rebuilding the database (wipes data)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}