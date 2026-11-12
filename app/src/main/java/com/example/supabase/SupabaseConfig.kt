package com.example.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object SupabaseConfig {
    private const val PREFS_NAME = "smart_tap_supabase_prefs"
    private const val KEY_URL = "custom_supabase_url"
    private const val KEY_ANON_KEY = "custom_supabase_anon_key"

    // Default configuration from BuildConfig (injected via .env / Secrets Gradle Plugin)
    private val DEFAULT_URL: String = try {
        BuildConfig.SUPABASE_URL
    } catch (_: Throwable) {
        "https://your-project.supabase.co"
    }

    private val DEFAULT_ANON_KEY: String = try {
        BuildConfig.SUPABASE_ANON_KEY
    } catch (_: Throwable) {
        ""
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSupabaseUrl(context: Context): String {
        val custom = getPrefs(context).getString(KEY_URL, null)
        return if (!custom.isNullOrBlank()) custom.trim() else DEFAULT_URL.trim()
    }

    fun getAnonKey(context: Context): String {
        val custom = getPrefs(context).getString(KEY_ANON_KEY, null)
        return if (!custom.isNullOrBlank()) custom.trim() else DEFAULT_ANON_KEY.trim()
    }

    fun setCustomConfig(context: Context, url: String, anonKey: String) {
        getPrefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_ANON_KEY, anonKey.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getSupabaseUrl(context)
        val key = getAnonKey(context)
        return url.startsWith("https://") &&
                !url.contains("your-project.supabase.co") &&
                key.isNotBlank() &&
                !key.contains("dummy")
    }
}
