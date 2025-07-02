package com.example.mindscribe

import LoginScreen.LoginScreen
import LoginScreen.LoginScreen2
import NavigationMenu.*
import Screens.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mindscribe.viewmodel.AuthViewModel
import com.example.mindscribe.viewmodel.NoteViewModel
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindscribe.data.ReminderDatabase
import com.example.mindscribe.repository.ReminderRepository
import com.example.mindscribe.ui.screens.SettingsScreen
import com.example.mindscribe.viewmodel.AuthViewModel.AuthEvent
import com.example.mindscribe.viewmodel.ReminderViewModel
import com.example.mindscribe.viewmodel.ReminderViewModelFactory

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    // Handle authentication events
    LaunchedEffect(authViewModel) {
        authViewModel.authEvents.collect { event ->
            when (event) {
                is AuthEvent.SignInSuccess -> {
                    navController.navigate("Home") {
                        popUpTo("Home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthEvent.SignInFailure -> {
                    Toast.makeText(
                        context,
                        "Sign in failed: ${event.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthEvent.SignOutSuccess -> {
                    // After sign out, stay on Home screen as guest
                    navController.navigate("Home") {
                        popUpTo("Home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthEvent.SignOutFailure -> {
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
                onGoogleSignIn = { idToken ->
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
                    navController.navigate("Home") {
                        popUpTo("Login2") { inclusive = true }
                    }
                }
            }
        }

        composable("Home") {
            val noteViewModel: NoteViewModel = hiltViewModel()

            HomeScreen(
                navController = navController,
                noteViewModel = noteViewModel,
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
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            val noteViewModel: NoteViewModel = hiltViewModel()
            NotesScreen(
                navController = navController,
                noteViewModel = noteViewModel,
                noteId = backStackEntry.arguments?.getString("noteId")
            )
        }

        composable("images") { ImagesScreen(navController) }
        composable("reminders") {
            val repository = ReminderRepository(
                ReminderDatabase.getDatabase(LocalContext.current).reminderDao()
            )
            val viewModel: ReminderViewModel = viewModel(
                factory = ReminderViewModelFactory(repository)
            )
            ReminderScreen(navController, viewModel)
        }
        composable("archive") {
            val noteViewModel: NoteViewModel = hiltViewModel()
            ArchiveScreen(navController = navController, noteViewModel = noteViewModel)
        }
        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("audio") { AudioScreen(navController) }
    }
}