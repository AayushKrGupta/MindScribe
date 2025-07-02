package Database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import backend.Note
import backend.Converters

@Database(entities = [Note::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao


}