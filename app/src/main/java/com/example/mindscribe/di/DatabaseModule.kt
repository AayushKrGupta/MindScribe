// app/src/main/java/com/example/mindscribe/di/DatabaseModule.kt
package com.example.mindscribe.di

import android.content.Context
import androidx.room.Room
import Database.NoteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNoteDatabase(
        @ApplicationContext context: Context
    ): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            "note_database"
        )
            .fallbackToDestructiveMigration() // Keeps your migration strategy
            .build()
    }

    @Provides
    fun provideNoteDao(database: NoteDatabase) = database.noteDao()
}