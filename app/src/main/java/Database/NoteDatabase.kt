// Database/NoteDatabase.kt
package Database // Keeping package as 'Database' as per your provided code

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import backend.Note // Import Note from 'backend' package
import backend.Converters // Import Converters from 'backend' package
import Database.NoteDao // Import NoteDao from 'Database' package (same as NoteDatabase)

@Database(entities = [Note::class], version = 8, exportSchema = false) // Version incremented to 8
@TypeConverters(Converters::class)
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
                    .fallbackToDestructiveMigration() // This handles schema changes by rebuilding the database (wipes data)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}