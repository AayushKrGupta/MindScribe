package LoginScreen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindscribe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.runtime.*
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen2(navController: NavController, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    val currentUser = auth.currentUser
    val profileImageUrl = currentUser?.photoUrl?.toString()

    val gso = remember {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val performLogout: () -> Unit = {
        coroutineScope.launch {
            try {
                auth.signOut()
                googleSignInClient.signOut()
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                navController.navigate("Login") {
                    popUpTo("Home") { inclusive = false }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Bigger profile picture
            Image(
                painter = rememberAsyncImagePainter(
                    model = profileImageUrl ?: R.drawable.userprofile
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(150.dp)  // Increased from 70dp to 150dp
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentUser?.displayName ?: "User Name",
                fontSize = 22.sp,  // Slightly larger font
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentUser?.email ?: "user@example.com",
                fontSize = 16.sp,  // Slightly larger font
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(100.dp))  // Pushes content to center

            // Centered buttons column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logout button
                Button(
                    onClick = performLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logout),
                        contentDescription = "Log out",
                        modifier = Modifier.size(24.dp))


                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Log out", fontSize = 16.sp)

                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete account button
                Button(
                    onClick = {
                        Toast.makeText(context, "Account deletion not implemented yet. Requires re-authentication.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logoutuser),
                        contentDescription = "Delete my account",
                        modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete my account", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))  // Bottom padding
        }
    }
}