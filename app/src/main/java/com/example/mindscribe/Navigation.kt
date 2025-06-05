package com.example.mindscribe

import LoginScreen.LoginScreen
import LoginScreen.LoginScreen2
import NavigationMenu.*
import Screens.*
import NoteViewModel.NoteViewModel
import NoteViewModel.NoteViewModelFactory
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mindscribe.ui.screens.ReminderScreen
import com.example.mindscribe.ui.screens.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import Database.NoteDatabase
import NoteViewModel.AuthViewModel
import Repo.NoteRepository
import android.widget.Toast

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val database = remember { NoteDatabase.getDatabase(application) }
    val noteRepository = remember { NoteRepository(database.noteDao()) }

    // Handle authentication events
    LaunchedEffect(authViewModel) {
        authViewModel.authEvents.collect { event ->
            when (event) {
                is AuthViewModel.AuthEvent.SignInSuccess -> {
                    navController.navigate("Home") {
                        popUpTo("Login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthViewModel.AuthEvent.SignInFailure -> {
                    Toast.makeText(
                        context,
                        "Sign in failed: ${event.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthViewModel.AuthEvent.SignOutSuccess -> {
                    navController.navigate("Login") {
                        popUpTo("Login2") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthViewModel.AuthEvent.SignOutFailure -> {
                    Toast.makeText(
                        context,
                        "Sign out failed: ${event.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "Home"
    ) {
        composable("Login") {
            LoginScreen(
                navController = navController,
                onGoogleSignIn = { idToken: String->
                    authViewModel.handleGoogleSignInResult(idToken)
                }
            )
        }

        composable("Login2") {
            if (currentUser != null) {
                LoginScreen2(
                    navController = navController,
                    onSignOut = { authViewModel.signOut() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("Login") {
                        popUpTo("Login2") { inclusive = true }
                    }
                }
            }
        }

        composable("Home") {
            val userId = currentUser?.uid ?: "guest"
            val homeViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(noteRepository, userId)
            )

            HomeScreen(
                navController = navController,
                noteViewModel = homeViewModel,
                onAccountClick = {
                    if (currentUser != null) {
                        navController.navigate("Login2")
                    } else {
                        navController.navigate("Login")
                    }
                }
            )
        }


        composable(
            "note/{noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) { backStackEntry ->
            val userId = currentUser?.uid ?: "guest"
            val notesViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(noteRepository, userId)
            )
            NotesScreen(
                navController = navController,
                noteViewModel = notesViewModel,
                noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull() ?: -1
            )
        }

        composable("images") { ImagesScreen(navController) }
        composable("reminders") { ReminderScreen(navController) }
        composable("archive") {
            val userId = currentUser?.uid ?: "guest"
            val archiveViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(noteRepository, userId)
            )
            ArchiveScreen(navController = navController, noteViewModel = archiveViewModel)
        }
        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("audio") { AudioScreen(navController) }
    }
}