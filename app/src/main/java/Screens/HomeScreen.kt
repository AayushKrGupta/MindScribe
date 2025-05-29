package com.example.mindscribe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.mindscribe.ui.components.NavigationDrawerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // ✅ Use String instead of Triple
    var selectedItem = remember { mutableStateOf("home") } // Holds only route names

    // For Account Dropdown Menu
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val accountList = listOf("user1@gmail.com", "user2@gmail.com", "Add another account", "Manage accounts")

    var searchText by remember { mutableStateOf("") }



    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(navController, drawerState, selectedItem, currentRoute = "home") // ✅ Pass currentRoute
        },
        content = {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .background(Color.White, shape = RoundedCornerShape(24.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drawer Icon
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }

                                // Search Bar
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (searchText.isEmpty()) {
                                                Text("Search", color = Color.Gray, fontSize = 18.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                // Account Icon Dropdown Menu
                                Box {
                                    IconButton(onClick = { navController.navigate(route = "Login") }) {
                                        Icon(Icons.Filled.AccountCircle, contentDescription = "Accounts")
                                    }

                                    DropdownMenu(
                                        expanded = accountMenuExpanded,
                                        onDismissRequest = { accountMenuExpanded = false }
                                    ) {
                                        accountList.forEach { account ->
                                            DropdownMenuItem(
                                                text = { Text(account) },
                                                onClick = { accountMenuExpanded = false } // Handle account switch
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("note") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Add Note")
                    }
                }
            ) { innerPadding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val note = ""
                    Text(text = "Write your first $note!", fontSize = 20.sp)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    )
}
