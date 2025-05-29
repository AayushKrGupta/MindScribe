package com.example.mindscribe

import NavigationMenu.AboutScreen
import NavigationMenu.ArchiveScreen
import NavigationMenu.AudioScreen
import NavigationMenu.BookmarksScreen
import NavigationMenu.CalendarScreen
import NavigationMenu.ChatbotScreen
import NavigationMenu.DeletedScreen
import NavigationMenu.ImagesScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mindscribe.ui.screens.HomeScreen
import com.example.mindscribe.ui.screens.LoginScreen
import com.example.mindscribe.ui.screens.LoginScreen2
import com.example.mindscribe.ui.screens.NotesScreen
import com.example.mindscribe.ui.screens.SettingsScreen

@Composable
fun Nav(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "Home") {
        composable(route="Home"){
            HomeScreen(navController)
        }
        composable(route = "Login"){
            LoginScreen(
                navController
            )
        }

        composable(route = "note")
            { NotesScreen(navController) }
        composable(route = "audio") { AudioScreen(navController) }
        composable("images") { ImagesScreen(navController) }
        composable("calendar") { CalendarScreen(navController) }
        composable("bookmarks") { BookmarksScreen(navController) }
        composable("archive") { ArchiveScreen(navController) }
        composable("deleted") { DeletedScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("chatbot") { ChatbotScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("Login2"){ LoginScreen2(navController) }

    }
}

