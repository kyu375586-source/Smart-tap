package com.example.auth

import android.content.Context
import com.example.supabase.SupabaseClient
import com.example.supabase.SupabaseSessionManager
import com.example.supabase.SupabaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object AuthManager {
    fun attemptAutoSignIn(
        context: Context,
        onAuthSuccess: (SupabaseUser) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val sessionManager = SupabaseSessionManager.getInstance(context)
        val user = sessionManager.currentUser.value
        if (user != null && user.id.isNotBlank()) {
            onAuthSuccess(user)
        } else {
            onUnauthenticated()
        }
    }

    fun signInWithEmail(
        context: Context,
        email: String,
        pass: String,
        onAuthSuccess: (SupabaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        val client = SupabaseClient(context)
        scope.launch {
            val result = client.signInWithEmail(email.trim(), pass)
            result.fold(
                onSuccess = { user -> onAuthSuccess(user) },
                onFailure = { err -> onAuthError(err.message ?: "Invalid email or password.") }
            )
        }
    }

    fun signUpWithEmail(
        context: Context,
        email: String,
        pass: String,
        username: String,
        referralCode: String,
        onAuthSuccess: (SupabaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        val client = SupabaseClient(context)
        scope.launch {
            val result = client.signUpWithEmail(email.trim(), pass, username.trim(), referralCode.trim())
            result.fold(
                onSuccess = { user -> onAuthSuccess(user) },
                onFailure = { err -> onAuthError(err.message ?: "Sign up failed. Please check inputs.") }
            )
        }
    }

    fun signOut(
        context: Context,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        val client = SupabaseClient(context)
        scope.launch {
            client.signOut()
            onSignOutComplete()
        }
    }
}
