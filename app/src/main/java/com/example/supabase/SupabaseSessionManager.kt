package com.example.supabase

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SupabaseUser(
    val id: String,
    val email: String,
    val username: String,
    val referralCode: String = "",
    val isAdmin: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

class SupabaseSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<SupabaseUser?>(loadStoredSession())
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    fun getUserId(): String? = _currentUser.value?.id

    fun getUserEmail(): String? = _currentUser.value?.email

    fun isUserAdmin(): Boolean {
        val email = _currentUser.value?.email ?: return false
        return email.equals(ADMIN_EMAIL, ignoreCase = true) || _currentUser.value?.isAdmin == true
    }

    fun saveSession(user: SupabaseUser) {
        val isAdminUser = user.email.equals(ADMIN_EMAIL, ignoreCase = true) || user.isAdmin
        val resolvedUser = user.copy(isAdmin = isAdminUser)

        prefs.edit()
            .putString(KEY_USER_ID, resolvedUser.id)
            .putString(KEY_EMAIL, resolvedUser.email)
            .putString(KEY_USERNAME, resolvedUser.username)
            .putString(KEY_REF_CODE, resolvedUser.referralCode)
            .putBoolean(KEY_IS_ADMIN, resolvedUser.isAdmin)
            .putString(KEY_ACCESS_TOKEN, resolvedUser.accessToken)
            .putString(KEY_REFRESH_TOKEN, resolvedUser.refreshToken)
            .apply()

        _currentUser.value = resolvedUser
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun loadStoredSession(): SupabaseUser? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val refCode = prefs.getString(KEY_REF_CODE, "") ?: ""
        val isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false) || email.equals(ADMIN_EMAIL, ignoreCase = true)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        return SupabaseUser(
            id = userId,
            email = email,
            username = username,
            referralCode = refCode,
            isAdmin = isAdmin,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    companion object {
        const val ADMIN_EMAIL = "kyu375586@gmail.com"
        private const val PREFS_NAME = "smart_tap_session_prefs"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_EMAIL = "session_email"
        private const val KEY_USERNAME = "session_username"
        private const val KEY_REF_CODE = "session_ref_code"
        private const val KEY_IS_ADMIN = "session_is_admin"
        private const val KEY_ACCESS_TOKEN = "session_access_token"
        private const val KEY_REFRESH_TOKEN = "session_refresh_token"

        @Volatile
        private var instance: SupabaseSessionManager? = null

        fun getInstance(context: Context): SupabaseSessionManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
