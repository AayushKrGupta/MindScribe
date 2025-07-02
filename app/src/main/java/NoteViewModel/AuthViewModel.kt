package com.example.mindscribe.viewmodel

import Repo.NoteRepository
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val auth: FirebaseAuth  // Injected FirebaseAuth
) : ViewModel() {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authEvents = Channel<AuthEvent>()
    val authEvents = _authEvents.receiveAsFlow()

    private val _syncTrigger = Channel<Unit>(Channel.BUFFERED)
    val syncTrigger = _syncTrigger.receiveAsFlow()
    private var lastSyncTime = 0L

    sealed class AuthEvent {
        data class SignInSuccess(val user: FirebaseUser) : AuthEvent()
        data class SignInFailure(val exception: Exception) : AuthEvent()
        object SignOutSuccess : AuthEvent()
        data class SignOutFailure(val exception: Exception) : AuthEvent()
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isLoggedIn.value = user != null

            if (user != null) {
                viewModelScope.launch {
                    if (System.currentTimeMillis() - lastSyncTime > 5000) { // 5-second cooldown
                        lastSyncTime = System.currentTimeMillis()
                        Log.d(TAG, "Triggering sync for user: ${user.uid}")
                        _syncTrigger.send(Unit)
                        try {
                            noteRepository.syncWithCloud(user.uid)
                        } catch (e: Exception) {
                            Log.e(TAG, "Sync failed", e)
                        }
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authEvents.send(AuthEvent.SignOutSuccess)
            } catch (e: Exception) {
                _authEvents.send(AuthEvent.SignOutFailure(e))
                Log.e(TAG, "Sign out failed", e)
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
                                Log.d(TAG, "Google sign-in success: ${user.uid}")
                                _authEvents.send(AuthEvent.SignInSuccess(user))
                            }
                        }
                    } else {
                        task.exception?.let { exception ->
                            viewModelScope.launch {
                                Log.e(TAG, "Google sign-in failed", exception)
                                _authEvents.send(AuthEvent.SignInFailure(exception))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in error", e)
                _authEvents.send(AuthEvent.SignInFailure(e))
            }
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    companion object {
        private const val TAG = "AuthViewModel"
    }
}