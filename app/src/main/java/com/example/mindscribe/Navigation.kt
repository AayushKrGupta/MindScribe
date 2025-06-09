package com.example.mindscribe

import LoginScreen.LoginScreen
import LoginScreen.LoginScreen2
import NavigationMenu.*
import Screens.*
import NoteViewModel.NoteViewModel
import NoteViewModel.NoteViewModelFactory // Ensure this import points to your updated factory
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
import com.google.firebase.auth.FirebaseAuth // Make sure FirebaseAuth is imported
import com.example.mindscribe.repository.FirestoreRepository // Make sure FirestoreRepository is imported


@Composable
fun Navigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Instantiate your repositories and FirebaseAuth once
    val database = remember { NoteDatabase.getDatabase(application) }
    val noteRepository = remember { NoteRepository(database.noteDao()) }
    val firestoreRepository = remember { FirestoreRepository() } // Instantiate FirestoreRepository
    val firebaseAuth = remember { FirebaseAuth.getInstance() } // Get FirebaseAuth instance

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
            // No userId needed here as NoteViewModelFactory already injects FirebaseAuth,
            // and NoteViewModel gets userId from auth.currentUser?.uid internally.
            val homeViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(
                    noteRepository,
                    firestoreRepository,
                    firebaseAuth
                )
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
                },
                authViewModel = authViewModel // Keep this for HomeScreen's UI logic
            )
        }


        composable(
            "note/{noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            // No userId needed here for the factory, as NoteViewModel gets it internally.
            val notesViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(
                    noteRepository,
                    firestoreRepository,
                    firebaseAuth
                )
            )
            NotesScreen(
                navController = navController,
                noteViewModel = notesViewModel,
                noteId = backStackEntry.arguments?.getString("noteId")
            )
        }

        composable("images") { ImagesScreen(navController) }
        composable("reminders") { ReminderScreen(navController) }
        composable("archive") {
            // No userId needed here for the factory, as NoteViewModel gets it internally.
            val archiveViewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(
                    noteRepository,
                    firestoreRepository,
                    firebaseAuth
                )
            )
            ArchiveScreen(navController = navController, noteViewModel = archiveViewModel)
        }
        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("audio") { AudioScreen(navController) }
    }
}