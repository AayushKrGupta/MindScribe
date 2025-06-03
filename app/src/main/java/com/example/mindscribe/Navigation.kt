package com.example.mindscribe

import NavigationMenu.* // Assumed imports for ArchiveScreen, AboutScreen etc.
import Screens.* // Assumed imports for HomeScreen, LoginScreen, NotesScreen, ImagesScreen, AudioScreen etc.
import NoteViewModel.NoteViewModel
import NoteViewModel.NoteViewModelFactory // <--- IMPORTANT: This is the correct import for your ViewModel Factory
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mindscribe.ui.screens.ReminderScreen
import com.example.mindscribe.ui.screens.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel // Needed for viewModel() with factory

@Composable
fun Navigation(noteViewModelFactory: NoteViewModelFactory) { // <--- FIX: This type MUST be NoteViewModelFactory
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Home") {

        composable("Home") {
            // Instantiate ViewModel using the factory for HomeScreen
            val homeViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
            HomeScreen(navController = navController, noteViewModel = homeViewModel)
        }

        composable("Login") {
            LoginScreen(navController)
        }

        // Note screen for new note (noteId = -1)
        composable("note/-1") { backStackEntry ->
            // Instantiate ViewModel using the factory for NotesScreen
            val notesViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
            NotesScreen(
                navController = navController,
                noteViewModel = notesViewModel,
                navBackStackEntry = backStackEntry
            )
        }

        // Note screen for editing existing note
        composable(
            "note/{noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                defaultValue = "-1" // Default value as a String for StringType
            })
        ) { backStackEntry ->
            // Instantiate ViewModel using the factory for NotesScreen
            val notesViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
            NotesScreen(
                navController = navController,
                noteViewModel = notesViewModel,
                navBackStackEntry = backStackEntry
            )
        }

        composable("images") { ImagesScreen(navController) }
        composable("reminders") { ReminderScreen(navController) }

        composable("archive") {
            // Instantiate ViewModel using the factory for ArchiveScreen
            val archiveViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
            ArchiveScreen(navController = navController, noteViewModel = archiveViewModel)
        }

        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("Login2") { LoginScreen2(navController) }
        composable("audio") { AudioScreen(navController) }
    }
}