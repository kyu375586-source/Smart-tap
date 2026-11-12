package com.example.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {
    private val sessionManager = SupabaseSessionManager.getInstance(context)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String = SupabaseConfig.getSupabaseUrl(context).trimEnd('/')
    private fun getAnonKey(): String = SupabaseConfig.getAnonKey(context)

    private fun getAuthHeader(): String {
        val token = sessionManager.currentUser.value?.accessToken
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${getAnonKey()}"
    }

    // -------------------------------------------------------------
    // AUTHENTICATION (GoTrue REST API)
    // -------------------------------------------------------------

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        username: String,
        referralCode: String
    ): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/auth/v1/signup"
            val meta = JSONObject().apply {
                put("username", username)
                put("referral_code", referralCode)
            }
            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("data", meta)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errObj = runCatching { JSONObject(respStr) }.getOrNull()
                val msg = errObj?.optString("msg")
                    ?: errObj?.optString("error_description")
                    ?: errObj?.optString("message")
                    ?: "Registration failed (HTTP ${response.code})"
                return@withContext Result.failure(Exception(msg))
            }

            val json = JSONObject(respStr)
            val userObj = json.optJSONObject("user") ?: json
            val uid = userObj.optString("id", "")
            val uEmail = userObj.optString("email", email)
            val accessToken = json.optString("access_token", null)
            val refreshToken = json.optString("refresh_token", null)

            val isAdmin = uEmail.equals(SupabaseSessionManager.ADMIN_EMAIL, ignoreCase = true)
            val ref = if (referralCode.isNotBlank()) referralCode else "REF${(100000..999999).random()}"

            val user = SupabaseUser(
                id = uid,
                email = uEmail,
                username = username,
                referralCode = ref,
                isAdmin = isAdmin,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
            sessionManager.saveSession(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "SignUp error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/auth/v1/token?grant_type=password"
            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errObj = runCatching { JSONObject(respStr) }.getOrNull()
                val msg = errObj?.optString("error_description")
                    ?: errObj?.optString("msg")
                    ?: errObj?.optString("message")
                    ?: "Invalid email or password."
                return@withContext Result.failure(Exception(msg))
            }

            val json = JSONObject(respStr)
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            val userObj = json.optJSONObject("user")
            val uid = userObj?.optString("id") ?: ""
            val uEmail = userObj?.optString("email") ?: email
            val metadata = userObj?.optJSONObject("user_metadata")
            val username = metadata?.optString("username") ?: uEmail.substringBefore("@")
            val refCode = metadata?.optString("referral_code") ?: "REF${(100000..999999).random()}"

            val isAdmin = uEmail.equals(SupabaseSessionManager.ADMIN_EMAIL, ignoreCase = true)

            val user = SupabaseUser(
                id = uid,
                email = uEmail,
                username = username,
                referralCode = refCode,
                isAdmin = isAdmin,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
            sessionManager.saveSession(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "SignIn error", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.currentUser.value?.accessToken
            if (!token.isNullOrBlank()) {
                val url = "${getBaseUrl()}/auth/v1/logout"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", getAnonKey())
                    .addHeader("Authorization", "Bearer $token")
                    .post(ByteArray(0).toRequestBody(null))
                    .build()
                runCatching { httpClient.newCall(request).execute() }
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    // -------------------------------------------------------------
    // POSTGREST DATABASE APIS
    // -------------------------------------------------------------

    private fun buildRestRequest(
        path: String,
        method: String = "GET",
        jsonBody: String? = null,
        prefer: String? = null
    ): Request {
        val url = "${getBaseUrl()}/rest/v1/$path"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", getAnonKey())
            .addHeader("Authorization", getAuthHeader())
            .addHeader("Content-Type", "application/json")

        prefer?.let { builder.addHeader("Prefer", it) }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete((jsonBody ?: "{}").toRequestBody(jsonMediaType))
        }

        return builder.build()
    }

    suspend fun fetchUserProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("user_profiles?id=eq.$userId&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            if (arr.length() == 0) return@withContext Result.success(null)

            val o = arr.getJSONObject(0)
            val profile = UserProfile(
                id = o.optString("id", userId),
                userId = o.optString("id", userId),
                username = o.optString("username", ""),
                email = o.optString("email", ""),
                avatarUrl = o.optString("avatar_url", ""),
                referralCode = o.optString("referral_code", ""),
                referredBy = o.optString("referred_by", ""),
                role = if (o.optBoolean("is_admin", false)) "admin" else "user",
                status = "active"
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", profile.userId)
                put("email", profile.email ?: "")
                put("username", profile.username)
                put("avatar_url", profile.photoUrl ?: "")
                put("referral_code", profile.referralCode)
                put("referred_by", profile.referredBy ?: "")
                put("is_admin", profile.role == "admin" || profile.email.equals(SupabaseSessionManager.ADMIN_EMAIL, ignoreCase = true))
            }
            val req = buildRestRequest("user_profiles", "POST", json.toString(), "resolution=merge-duplicates")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserBalance(userId: String): Result<UserBalance?> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("user_balances?user_id=eq.$userId&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            if (arr.length() == 0) return@withContext Result.success(null)

            val o = arr.getJSONObject(0)
            val balance = UserBalance(
                id = o.optString("user_id", userId),
                userId = o.optString("user_id", userId),
                coinBalance = o.optLong("current_balance", 0L),
                lifetimeCoins = o.optLong("total_earned", 0L),
                withdrawableBalance = o.optLong("current_balance", 0L),
                pendingCoins = 0L,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertUserBalance(
        userId: String,
        coinsEarned: Long,
        totalCoins: Long,
        totalTaps: Long,
        energyRemaining: Long,
        energyMax: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("current_balance", totalCoins)
                put("total_earned", totalCoins)
                put("total_taps", totalTaps)
                put("energy_current", energyRemaining)
                put("energy_max", energyMax)
                put("last_tap_timestamp", System.currentTimeMillis())
            }
            val req = buildRestRequest("user_balances", "POST", json.toString(), "resolution=merge-duplicates")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTasks(): Result<List<TaskItem>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("tasks?is_active=eq.true&order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<TaskItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TaskItem(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        description = o.optString("description"),
                        category = o.optString("category", "daily"),
                        rewardCoins = o.optLong("reward_amount", 500L),
                        requirements = o.optString("requirements", ""),
                        actionUrl = o.optString("action_url", ""),
                        isActive = o.optBoolean("is_active", true)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitTaskProof(submission: TaskSubmission): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("task_id", submission.taskId)
                put("user_id", submission.userId)
                put("user_email", submission.username)
                put("task_title", submission.taskTitle)
                put("proof_text", submission.proofText)
                put("status", "PENDING")
                put("reward_amount", submission.rewardCoins)
            }
            val req = buildRestRequest("task_submissions", "POST", json.toString(), "return=minimal")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserSubmissions(userId: String): Result<List<TaskSubmission>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("task_submissions?user_id=eq.$userId&order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<TaskSubmission>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TaskSubmission(
                        id = o.optString("id"),
                        taskId = o.optString("task_id"),
                        userId = o.optString("user_id"),
                        username = o.optString("user_email"),
                        taskTitle = o.optString("task_title"),
                        proofText = o.optString("proof_text"),
                        status = o.optString("status", "pending").lowercase(),
                        rewardCoins = o.optLong("reward_amount", 0L)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawal(reqData: WithdrawalRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("user_id", reqData.userId)
                put("user_email", reqData.username)
                put("amount_coins", reqData.coinAmount)
                put("amount_usd", reqData.withdrawalAmount)
                put("payment_method", reqData.paymentMethod)
                put("account_details", reqData.paymentDetails)
                put("status", "PENDING")
                put("admin_notes", reqData.adminNotes)
            }
            val req = buildRestRequest("withdrawal_requests", "POST", json.toString(), "return=minimal")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserWithdrawals(userId: String): Result<List<WithdrawalRequest>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("withdrawal_requests?user_id=eq.$userId&order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<WithdrawalRequest>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WithdrawalRequest(
                        id = o.optString("id"),
                        userId = o.optString("user_id"),
                        username = o.optString("user_email"),
                        coinAmount = o.optLong("amount_coins"),
                        withdrawalAmount = o.optDouble("amount_usd"),
                        paymentMethod = o.optString("payment_method"),
                        paymentDetails = o.optString("account_details"),
                        status = o.optString("status", "Pending"),
                        adminNotes = o.optString("admin_notes")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLeaderboard(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("user_balances?order=current_balance.desc&limit=50&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<UserProfile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    UserProfile(
                        id = o.optString("user_id"),
                        userId = o.optString("user_id"),
                        username = "Player_${o.optString("user_id").take(6)}",
                        totalEarnings = o.optLong("total_earned", 0L),
                        currentLevel = ((o.optLong("total_earned", 0L) / 5000) + 1).toInt().coerceAtMost(100)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAppConfig(): Result<AppConfiguration> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("app_configuration?id=eq.default&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            if (arr.length() == 0) return@withContext Result.success(AppConfiguration())

            val o = arr.getJSONObject(0)
            val config = AppConfiguration(
                id = o.optString("id", "default"),
                coinsPerTap = o.optLong("coins_per_tap", 1L),
                baseEnergy = o.optLong("base_energy", 1000L),
                energyRegenPerSec = o.optLong("energy_regen_rate", 2L),
                coinToUsdRate = o.optDouble("withdrawal_rate", 0.0001),
                minWithdrawalCoins = o.optLong("min_withdrawal_coins", 50000L),
                maintenanceMode = o.optBoolean("maintenance_mode", false),
                bannerNotice = o.optString("banner_announcement", "")
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppConfig(config: AppConfiguration): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("coins_per_tap", config.coinsPerTap)
                put("base_energy", config.baseEnergy)
                put("energy_regen_rate", config.energyRegenPerSec)
                put("withdrawal_rate", config.coinToUsdRate)
                put("min_withdrawal_coins", config.minWithdrawalCoins)
                put("maintenance_mode", config.maintenanceMode)
                put("banner_announcement", config.bannerNotice)
            }
            val req = buildRestRequest("app_configuration?id=eq.default", "PATCH", json.toString())
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAnnouncements(): Result<List<AnnouncementItem>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("announcements?is_active=eq.true&order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<AnnouncementItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AnnouncementItem(
                        id = o.optString("id"),
                        heading = o.optString("title"),
                        description = o.optString("message"),
                        isActive = o.optBoolean("is_active", true)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // ADMIN ENDPOINTS
    // -------------------------------------------------------------

    suspend fun fetchAllUsersAdmin(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("user_profiles?order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<UserProfile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    UserProfile(
                        id = o.optString("id"),
                        userId = o.optString("id"),
                        username = o.optString("username"),
                        email = o.optString("email"),
                        role = if (o.optBoolean("is_admin")) "admin" else "user",
                        referralCode = o.optString("referral_code")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllSubmissionsAdmin(): Result<List<TaskSubmission>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("task_submissions?order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<TaskSubmission>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TaskSubmission(
                        id = o.optString("id"),
                        taskId = o.optString("task_id"),
                        userId = o.optString("user_id"),
                        username = o.optString("user_email"),
                        taskTitle = o.optString("task_title"),
                        proofText = o.optString("proof_text"),
                        status = o.optString("status", "PENDING").lowercase(),
                        rewardCoins = o.optLong("reward_amount", 0L)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllWithdrawalsAdmin(): Result<List<WithdrawalRequest>> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("withdrawal_requests?order=created_at.desc&select=*")
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val list = mutableListOf<WithdrawalRequest>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WithdrawalRequest(
                        id = o.optString("id"),
                        userId = o.optString("user_id"),
                        username = o.optString("user_email"),
                        coinAmount = o.optLong("amount_coins"),
                        withdrawalAmount = o.optDouble("amount_usd"),
                        paymentMethod = o.optString("payment_method"),
                        paymentDetails = o.optString("account_details"),
                        status = o.optString("status", "Pending"),
                        adminNotes = o.optString("admin_notes")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWithdrawalStatus(id: String, status: String, adminNotes: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("status", status)
                put("admin_notes", adminNotes)
            }
            val req = buildRestRequest("withdrawal_requests?id=eq.$id", "PATCH", json.toString())
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskSubmissionStatus(id: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("status", status.uppercase())
            }
            val req = buildRestRequest("task_submissions?id=eq.$id", "PATCH", json.toString())
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTask(task: TaskItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                if (task.id.isNotBlank()) put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("reward_amount", task.rewardCoins)
                put("requirements", task.requirements)
                put("is_active", task.isActive)
            }
            val method = if (task.id.isNotBlank()) "PATCH" else "POST"
            val path = if (task.id.isNotBlank()) "tasks?id=eq.${task.id}" else "tasks"
            val req = buildRestRequest(path, method, json.toString())
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("tasks?id=eq.$id", "DELETE")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(item: AnnouncementItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", item.heading)
                put("message", item.description)
                put("is_active", item.isActive)
            }
            val req = buildRestRequest("announcements", "POST", json.toString())
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRestRequest("announcements?id=eq.$id", "DELETE")
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
