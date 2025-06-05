package NoteViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    // StateFlow for current user
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // StateFlow for authentication state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Channel for authentication events
    private val _authEvents = Channel<AuthEvent>()
    val authEvents = _authEvents.receiveAsFlow()

    sealed class AuthEvent {
        data class SignInSuccess(val user: FirebaseUser) : AuthEvent()
        data class SignInFailure(val exception: Exception) : AuthEvent()
        object SignOutSuccess : AuthEvent()
        data class SignOutFailure(val exception: Exception) : AuthEvent()
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isLoggedIn.value = firebaseAuth.currentUser != null
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authEvents.send(AuthEvent.SignOutSuccess)
            } catch (e: Exception) {
                _authEvents.send(AuthEvent.SignOutFailure(e))
            }
        }
    }

    fun handleGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.let { user ->
                            viewModelScope.launch {
                                _authEvents.send(AuthEvent.SignInSuccess(user))
                            }
                        }
                    } else {
                        task.exception?.let { exception ->
                            viewModelScope.launch {
                                _authEvents.send(AuthEvent.SignInFailure(exception))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _authEvents.send(AuthEvent.SignInFailure(e))
            }
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
}