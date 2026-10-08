package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.UserRepository
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser, val profile: UserProfile?) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadProfile(user.uid)
            } else {
                _userProfile.value = null
                _uiState.value = AuthUiState.Idle
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun loadProfile(uid: String) {
        viewModelScope.launch {
            userRepository.observeProfile(uid).collect { profile ->
                _userProfile.value = profile
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    _uiState.value = AuthUiState.Authenticated(currentUser, profile)
                }
            }
        }
    }

    fun attemptAutoSignIn(context: Context) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadProfile(currentUser.uid)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val credentialManager = CredentialManager.create(context)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto-sign-in skipped: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error("Google Sign-In configuration missing")
            return
        }

        _uiState.value = AuthUiState.Loading
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        val credentialManager = CredentialManager.create(context)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        // Check if profile exists, if not initialize default
                        val existing = userRepository.getProfile(user.uid).getOrNull()
                        if (existing == null) {
                            val defaultUsername = (user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}")
                                .lowercase().replace("[^a-z0-9_]".toRegex(), "")
                            userRepository.saveProfile(
                                username = defaultUsername.ifEmpty { "user_${user.uid.take(5)}" },
                                displayName = user.displayName ?: "Khahish User",
                                photoUrl = user.photoUrl?.toString()
                            )
                        }
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled: ${e.message}", e)
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign-in failed")
            }
        }
    }

    fun saveUserProfile(
        username: String,
        displayName: String,
        bio: String?,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = userRepository.saveProfile(
                username = username,
                displayName = displayName,
                photoUrl = auth.currentUser?.photoUrl?.toString(),
                phoneNumber = phoneNumber,
                bio = bio
            )
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.localizedMessage ?: "Failed to update profile") }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(context)
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state: ${e.message}")
            }
            auth.signOut()
            _userProfile.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
    }
}
