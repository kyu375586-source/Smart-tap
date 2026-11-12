package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.*
import com.example.supabase.SupabaseClient
import com.example.supabase.SupabaseSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SmartTapRepository(private val context: Context) {
    private val client = SupabaseClient(context)
    private val sessionManager = SupabaseSessionManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("smart_tap_repo_prefs", Context.MODE_PRIVATE)

    // In-memory reactive state streams
    private val _appConfig = MutableStateFlow(loadCachedAppConfig())
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _userBalance = MutableStateFlow(UserBalance())
    private val _energyRecord = MutableStateFlow(EnergyRecord())
    private val _dailyCheckin = MutableStateFlow(DailyCheckin())
    private val _tasks = MutableStateFlow<List<TaskItem>>(defaultTasks())
    private val _userSubmissions = MutableStateFlow<List<TaskSubmission>>(emptyList())
    private val _boosters = MutableStateFlow<List<Booster>>(defaultBoosters())
    private val _userBoosters = MutableStateFlow<List<BoosterPurchase>>(emptyList())
    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    private val _leaderboard = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _announcements = MutableStateFlow<List<AnnouncementItem>>(defaultAnnouncements())

    // Admin state streams
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _allSubmissions = MutableStateFlow<List<TaskSubmission>>(emptyList())
    private val _allWithdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())

    init {
        refreshGlobalData()
    }

    fun isCurrentUserAdmin(): Boolean = sessionManager.isUserAdmin()

    private fun refreshGlobalData() {
        scope.launch {
            client.fetchAppConfig().onSuccess { _appConfig.value = it }
            client.fetchAnnouncements().onSuccess { if (it.isNotEmpty()) _announcements.value = it }
            client.fetchTasks().onSuccess { if (it.isNotEmpty()) _tasks.value = it }
            client.fetchLeaderboard().onSuccess { if (it.isNotEmpty()) _leaderboard.value = it }
        }
    }

    // -------------------------------------------------------------
    // REMOTE CONFIGURATION
    // -------------------------------------------------------------
    fun observeAppConfig(): Flow<AppConfiguration> = _appConfig.asStateFlow()

    suspend fun saveAppConfig(config: AppConfiguration) {
        _appConfig.value = config
        saveCachedAppConfig(config)
        client.updateAppConfig(config)
    }

    private fun loadCachedAppConfig(): AppConfiguration {
        return AppConfiguration(
            coinsPerTap = prefs.getLong("config_coins_per_tap", 1L),
            baseEnergy = prefs.getLong("config_base_energy", 1000L),
            energyRegenPerSec = prefs.getLong("config_energy_regen", 3L),
            coinToUsdRate = prefs.getFloat("config_rate", 0.0001f).toDouble(),
            minWithdrawalCoins = prefs.getLong("config_min_withdrawal", 50000L),
            maintenanceMode = prefs.getBoolean("config_maintenance", false),
            bannerNotice = prefs.getString("config_banner", "Welcome to SMART TAP! Tap to earn digital rewards.") ?: ""
        )
    }

    private fun saveCachedAppConfig(config: AppConfiguration) {
        prefs.edit()
            .putLong("config_coins_per_tap", config.coinsPerTap)
            .putLong("config_base_energy", config.baseEnergy)
            .putLong("config_energy_regen", config.energyRegenPerSec)
            .putFloat("config_rate", config.coinToUsdRate.toFloat())
            .putLong("config_min_withdrawal", config.minWithdrawalCoins)
            .putBoolean("config_maintenance", config.maintenanceMode)
            .putString("config_banner", config.bannerNotice)
            .apply()
    }

    // -------------------------------------------------------------
    // USER PROFILE & INITIALIZATION
    // -------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> = _userProfile.asStateFlow()

    suspend fun initializeUserIfMissing(
        userId: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        val cachedCoins = prefs.getLong("user_coins_$userId", 100L)
        val cachedTotal = prefs.getLong("user_total_$userId", 100L)
        val cachedTaps = prefs.getLong("user_taps_$userId", 0L)
        val cachedEnergy = prefs.getLong("user_energy_$userId", _appConfig.value.baseEnergy)

        val initialProfile = UserProfile(
            id = userId,
            userId = userId,
            username = displayName ?: email?.substringBefore("@") ?: "Player_${userId.take(6)}",
            email = email,
            photoUrl = photoUrl,
            referralCode = "REF${userId.take(6).uppercase()}",
            role = if (email.equals(SupabaseSessionManager.ADMIN_EMAIL, ignoreCase = true)) "admin" else "user",
            status = "active",
            totalTaps = cachedTaps,
            totalEarnings = cachedTotal
        )
        _userProfile.value = initialProfile
        _userBalance.value = UserBalance(
            id = userId,
            userId = userId,
            coinBalance = cachedCoins,
            lifetimeCoins = cachedTotal,
            withdrawableBalance = cachedCoins
        )
        _energyRecord.value = EnergyRecord(
            id = userId,
            userId = userId,
            currentEnergy = cachedEnergy,
            maxEnergy = _appConfig.value.baseEnergy
        )

        // Try fetch from Supabase
        client.fetchUserProfile(userId).onSuccess { fetched ->
            if (fetched != null) {
                _userProfile.value = fetched
            } else {
                client.upsertUserProfile(initialProfile)
            }
        }

        client.fetchUserBalance(userId).onSuccess { fetchedBalance ->
            if (fetchedBalance != null) {
                _userBalance.value = fetchedBalance
            }
        }

        client.fetchUserSubmissions(userId).onSuccess { _userSubmissions.value = it }
        client.fetchUserWithdrawals(userId).onSuccess { _withdrawals.value = it }

        if (isCurrentUserAdmin()) {
            refreshAdminData()
        }
    }

    fun observeUserBalance(userId: String): Flow<UserBalance> = _userBalance.asStateFlow()

    suspend fun syncTaps(
        userId: String,
        tapsToAdd: Int,
        coinsEarned: Long,
        remainingEnergy: Long,
        maxEnergy: Long
    ) {
        val currentBalance = _userBalance.value
        val newBalance = currentBalance.coinBalance + coinsEarned
        val newLifetime = currentBalance.lifetimeCoins + coinsEarned
        val currentProfile = _userProfile.value
        val newTotalTaps = (currentProfile?.totalTaps ?: 0L) + tapsToAdd

        _userBalance.value = currentBalance.copy(
            coinBalance = newBalance,
            lifetimeCoins = newLifetime,
            withdrawableBalance = newBalance
        )
        _energyRecord.value = _energyRecord.value.copy(
            currentEnergy = remainingEnergy,
            maxEnergy = maxEnergy
        )
        _userProfile.value = currentProfile?.copy(
            totalTaps = newTotalTaps,
            totalEarnings = newLifetime
        )

        // Cache locally
        prefs.edit()
            .putLong("user_coins_$userId", newBalance)
            .putLong("user_total_$userId", newLifetime)
            .putLong("user_taps_$userId", newTotalTaps)
            .putLong("user_energy_$userId", remainingEnergy)
            .apply()

        // Sync to Supabase
        client.upsertUserBalance(userId, coinsEarned, newBalance, newTotalTaps, remainingEnergy, maxEnergy)
    }

    suspend fun adjustUserBalanceAdmin(userId: String, amountCoins: Long, reason: String) {
        val cur = _userBalance.value
        val adjusted = (cur.coinBalance + amountCoins).coerceAtLeast(0L)
        _userBalance.value = cur.copy(coinBalance = adjusted, lifetimeCoins = (cur.lifetimeCoins + amountCoins).coerceAtLeast(0L))
        prefs.edit().putLong("user_coins_$userId", adjusted).apply()
        client.upsertUserBalance(userId, amountCoins, adjusted, 0, 1000, 1000)
    }

    fun observeEnergy(userId: String): Flow<EnergyRecord> = _energyRecord.asStateFlow()

    suspend fun recordRewardedAdRecharge(userId: String) {
        val max = _appConfig.value.baseEnergy
        _energyRecord.value = _energyRecord.value.copy(currentEnergy = max)
        val cur = _userBalance.value
        val bonus = 250L
        _userBalance.value = cur.copy(coinBalance = cur.coinBalance + bonus, lifetimeCoins = cur.lifetimeCoins + bonus)

        prefs.edit()
            .putLong("user_energy_$userId", max)
            .putLong("user_coins_$userId", _userBalance.value.coinBalance)
            .apply()

        client.upsertUserBalance(userId, bonus, _userBalance.value.coinBalance, 0, max, max)
    }

    fun observeDailyCheckin(userId: String): Flow<DailyCheckin> = _dailyCheckin.asStateFlow()

    suspend fun claimDailyReward(userId: String): Long {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val checkin = _dailyCheckin.value
        if (checkin.lastClaimDate == todayStr) {
            return 0L
        }

        val newStreak = checkin.streakCount + 1
        val rewardAmount = (newStreak * 250L).coerceAtMost(5000L)

        _dailyCheckin.value = checkin.copy(
            streakCount = newStreak,
            lastClaimDate = todayStr,
            totalClaimedDays = checkin.totalClaimedDays + 1
        )

        val cur = _userBalance.value
        _userBalance.value = cur.copy(
            coinBalance = cur.coinBalance + rewardAmount,
            lifetimeCoins = cur.lifetimeCoins + rewardAmount
        )

        prefs.edit().putLong("user_coins_$userId", _userBalance.value.coinBalance).apply()
        return rewardAmount
    }

    // -------------------------------------------------------------
    // TASKS & SUBMISSIONS
    // -------------------------------------------------------------
    fun observeTasks(): Flow<List<TaskItem>> = _tasks.asStateFlow()

    fun observeUserSubmissions(userId: String): Flow<List<TaskSubmission>> = _userSubmissions.asStateFlow()

    suspend fun submitTaskProof(submission: TaskSubmission) {
        _userSubmissions.value = listOf(submission) + _userSubmissions.value
        client.submitTaskProof(submission)
    }

    suspend fun createTask(task: TaskItem) {
        _tasks.value = listOf(task) + _tasks.value
        client.saveTask(task)
    }

    suspend fun deleteTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
        client.deleteTask(taskId)
    }

    // -------------------------------------------------------------
    // BOOSTERS
    // -------------------------------------------------------------
    fun observeBoosters(): Flow<List<Booster>> = _boosters.asStateFlow()

    fun observeUserActivePurchases(userId: String): Flow<List<BoosterPurchase>> = _userBoosters.asStateFlow()

    suspend fun purchaseBooster(userId: String, booster: Booster) {
        val curBalance = _userBalance.value.coinBalance
        if (curBalance < booster.price) {
            throw IllegalStateException("Insufficient coins to purchase ${booster.name}")
        }

        val newBalance = curBalance - booster.price
        _userBalance.value = _userBalance.value.copy(coinBalance = newBalance)
        prefs.edit().putLong("user_coins_$userId", newBalance).apply()

        val purchase = BoosterPurchase(
            id = UUID.randomUUID().toString(),
            userId = userId,
            boosterId = booster.id,
            boosterName = booster.name,
            purchasedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (booster.durationSeconds * 1000L)
        )
        _userBoosters.value = listOf(purchase) + _userBoosters.value
    }

    // -------------------------------------------------------------
    // WALLET & WITHDRAWALS
    // -------------------------------------------------------------
    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    suspend fun requestWithdrawal(
        userId: String,
        username: String,
        coinAmount: Long,
        paymentMethod: String,
        paymentDetails: String
    ) {
        val min = _appConfig.value.minWithdrawalCoins
        if (coinAmount < min) {
            throw IllegalStateException("Minimum withdrawal is $min coins")
        }
        val curBalance = _userBalance.value.coinBalance
        if (curBalance < coinAmount) {
            throw IllegalStateException("Insufficient coin balance")
        }

        val usdVal = coinAmount * _appConfig.value.coinToUsdRate
        val req = WithdrawalRequest(
            id = UUID.randomUUID().toString(),
            userId = userId,
            username = username,
            coinAmount = coinAmount,
            withdrawalAmount = usdVal,
            paymentMethod = paymentMethod,
            paymentDetails = paymentDetails,
            status = "Pending"
        )

        // Deduct coins
        val newBal = curBalance - coinAmount
        _userBalance.value = _userBalance.value.copy(coinBalance = newBal, withdrawableBalance = newBal)
        prefs.edit().putLong("user_coins_$userId", newBal).apply()

        _withdrawals.value = listOf(req) + _withdrawals.value
        client.submitWithdrawal(req)
    }

    fun observeTransactions(userId: String): Flow<List<TransactionItem>> = _transactions.asStateFlow()

    fun observeLeaderboard(): Flow<List<UserProfile>> = _leaderboard.asStateFlow()

    fun observeAnnouncements(): Flow<List<AnnouncementItem>> = _announcements.asStateFlow()

    suspend fun createAnnouncement(announcement: AnnouncementItem) {
        _announcements.value = listOf(announcement) + _announcements.value
        client.saveAnnouncement(announcement)
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        _announcements.value = _announcements.value.filter { it.id != announcementId }
        client.deleteAnnouncement(announcementId)
    }

    // -------------------------------------------------------------
    // ADMIN OBSERVABLES & CONTROLS
    // -------------------------------------------------------------
    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = _allUsers.asStateFlow()

    fun observeAllSubmissionsAdmin(): Flow<List<TaskSubmission>> = _allSubmissions.asStateFlow()

    fun observeAllWithdrawalsAdmin(): Flow<List<WithdrawalRequest>> = _allWithdrawals.asStateFlow()

    suspend fun refreshAdminData() {
        client.fetchAllUsersAdmin().onSuccess { _allUsers.value = it }
        client.fetchAllSubmissionsAdmin().onSuccess { _allSubmissions.value = it }
        client.fetchAllWithdrawalsAdmin().onSuccess { _allWithdrawals.value = it }
    }

    suspend fun reviewTaskSubmission(submissionId: String, approve: Boolean, rewardCoins: Long, adminUid: String) {
        val newStatus = if (approve) "approved" else "rejected"
        _allSubmissions.value = _allSubmissions.value.map {
            if (it.id == submissionId) it.copy(status = newStatus, reviewedAt = System.currentTimeMillis()) else it
        }
        client.updateTaskSubmissionStatus(submissionId, newStatus)
    }

    suspend fun updateWithdrawalStatus(withdrawalId: String, newStatus: String, adminNotes: String) {
        _allWithdrawals.value = _allWithdrawals.value.map {
            if (it.id == withdrawalId) it.copy(status = newStatus, adminNotes = adminNotes, processedDate = System.currentTimeMillis()) else it
        }
        client.updateWithdrawalStatus(withdrawalId, newStatus, adminNotes)
    }

    suspend fun updateUserStatusAdmin(userId: String, newStatus: String) {
        _allUsers.value = _allUsers.value.map {
            if (it.userId == userId) it.copy(status = newStatus) else it
        }
    }

    private fun defaultTasks(): List<TaskItem> = listOf(
        TaskItem(
            id = "t1",
            title = "Join Official Community",
            description = "Join our Telegram community channel for daily bonus codes",
            category = "social",
            rewardCoins = 1000L,
            requirements = "Submit your handle"
        ),
        TaskItem(
            id = "t2",
            title = "Follow on X (Twitter)",
            description = "Follow @SmartTapApp and repost the announcement",
            category = "social",
            rewardCoins = 750L,
            requirements = "Submit your X username"
        ),
        TaskItem(
            id = "t3",
            title = "Complete 500 Taps Today",
            description = "Reach 500 taps in a single day session",
            category = "daily",
            rewardCoins = 500L,
            requirements = "Completed automatically"
        ),
        TaskItem(
            id = "t4",
            title = "Invite 3 Friends",
            description = "Share your referral code and invite 3 friends to join",
            category = "referral",
            rewardCoins = 5000L,
            requirements = "3 referrals required"
        )
    )

    private fun defaultBoosters(): List<Booster> = listOf(
        Booster(
            id = "b1",
            name = "Multitap x2",
            description = "Doubles coin reward on every tap for 5 minutes",
            multiplier = 2,
            price = 300L,
            durationSeconds = 300L,
            boosterType = "multitap"
        ),
        Booster(
            id = "b2",
            name = "Energy Surge +500",
            description = "Increases maximum energy capacity by +500",
            multiplier = 1,
            price = 600L,
            durationSeconds = 600L,
            boosterType = "energy_boost"
        ),
        Booster(
            id = "b3",
            name = "Turbo Recharge x3",
            description = "Triples energy regeneration speed per second",
            multiplier = 3,
            price = 500L,
            durationSeconds = 300L,
            boosterType = "energy_recovery"
        )
    )

    private fun defaultAnnouncements(): List<AnnouncementItem> = listOf(
        AnnouncementItem(
            id = "a1",
            heading = "Welcome to SMART TAP!",
            description = "Tap the 3D coin, recharge energy, unlock boosters, and climb the leaderboard.",
            isActive = true
        )
    )
}
