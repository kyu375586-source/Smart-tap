package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiTappingAdvisor
import com.example.data.model.*
import com.example.data.repository.SmartTapRepository
import com.example.util.SoundAndHaptics
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TapParticle(
    val id: Long,
    val value: Long,
    val xOffset: Float,
    val yOffset: Float
)

data class UiMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isError: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SmartTapRepository(application)
    private val aiAdvisor = AiTappingAdvisor()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfiguration())
    val appConfig: StateFlow<AppConfiguration> = _appConfig.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userBalance = MutableStateFlow(UserBalance())
    val userBalance: StateFlow<UserBalance> = _userBalance.asStateFlow()

    private val _energyRecord = MutableStateFlow(EnergyRecord())
    val energyRecord: StateFlow<EnergyRecord> = _energyRecord.asStateFlow()

    private val _dailyCheckin = MutableStateFlow(DailyCheckin())
    val dailyCheckin: StateFlow<DailyCheckin> = _dailyCheckin.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _userSubmissions = MutableStateFlow<List<TaskSubmission>>(emptyList())
    val userSubmissions: StateFlow<List<TaskSubmission>> = _userSubmissions.asStateFlow()

    private val _boosters = MutableStateFlow<List<Booster>>(emptyList())
    val boosters: StateFlow<List<Booster>> = _boosters.asStateFlow()

    private val _userBoosters = MutableStateFlow<List<BoosterPurchase>>(emptyList())
    val userBoosters: StateFlow<List<BoosterPurchase>> = _userBoosters.asStateFlow()

    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<UserProfile>>(emptyList())
    val leaderboard: StateFlow<List<UserProfile>> = _leaderboard.asStateFlow()

    private val _announcements = MutableStateFlow<List<AnnouncementItem>>(emptyList())
    val announcements: StateFlow<List<AnnouncementItem>> = _announcements.asStateFlow()

    // Admin streams
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _allSubmissions = MutableStateFlow<List<TaskSubmission>>(emptyList())
    val allSubmissions: StateFlow<List<TaskSubmission>> = _allSubmissions.asStateFlow()

    private val _allWithdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val allWithdrawals: StateFlow<List<WithdrawalRequest>> = _allWithdrawals.asStateFlow()

    // Floating particles & transient UI
    private val _tapParticles = MutableStateFlow<List<TapParticle>>(emptyList())
    val tapParticles: StateFlow<List<TapParticle>> = _tapParticles.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    private val _aiStrategyText = MutableStateFlow<String?>(null)
    val aiStrategyText: StateFlow<String?> = _aiStrategyText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Tap sync accumulator
    private var pendingTaps = 0
    private var pendingCoins = 0L
    private var syncJob: Job? = null

    init {
        monitorNetwork(application)
        observeGlobalData()
        startEnergyRegenerationLoop()
    }

    private fun monitorNetwork(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }
            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun observeGlobalData() {
        viewModelScope.launch {
            repository.observeAppConfig().collect { _appConfig.value = it }
        }
        viewModelScope.launch {
            repository.observeAnnouncements().collect { _announcements.value = it }
        }
        viewModelScope.launch {
            repository.observeTasks().collect { _tasks.value = it }
        }
        viewModelScope.launch {
            repository.observeBoosters().collect { _boosters.value = it }
        }
        viewModelScope.launch {
            repository.observeLeaderboard().collect { _leaderboard.value = it }
        }
    }

    fun bindUser(userId: String, displayName: String?, email: String?, photoUrl: String?) {
        viewModelScope.launch {
            repository.initializeUserIfMissing(userId, displayName, email, photoUrl)

            launch {
                repository.observeUserProfile(userId).collect { profile ->
                    _userProfile.value = profile
                    if (profile?.role == "admin" || repository.isCurrentUserAdmin()) {
                        observeAdminData()
                    }
                }
            }

            launch {
                repository.observeUserBalance(userId).collect { _userBalance.value = it }
            }

            launch {
                repository.observeEnergy(userId).collect { _energyRecord.value = it }
            }

            launch {
                repository.observeDailyCheckin(userId).collect { _dailyCheckin.value = it }
            }

            launch {
                repository.observeUserSubmissions(userId).collect { _userSubmissions.value = it }
            }

            launch {
                repository.observeUserActivePurchases(userId).collect { _userBoosters.value = it }
            }

            launch {
                repository.observeUserWithdrawals(userId).collect { _withdrawals.value = it }
            }

            launch {
                repository.observeTransactions(userId).collect { _transactions.value = it }
            }
        }
    }

    private fun observeAdminData() {
        viewModelScope.launch {
            repository.observeAllUsersAdmin().collect { _allUsers.value = it }
        }
        viewModelScope.launch {
            repository.observeAllSubmissionsAdmin().collect { _allSubmissions.value = it }
        }
        viewModelScope.launch {
            repository.observeAllWithdrawalsAdmin().collect { _allWithdrawals.value = it }
        }
    }

    private fun startEnergyRegenerationLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _energyRecord.value
                val config = _appConfig.value
                val max = if (config.baseEnergy > 0) config.baseEnergy else current.maxEnergy
                val regen = if (config.energyRegenPerSec > 0) config.energyRegenPerSec else current.rechargeRatePerSec

                if (current.currentEnergy < max) {
                    val updatedEnergy = (current.currentEnergy + regen).coerceAtMost(max)
                    _energyRecord.value = current.copy(currentEnergy = updatedEnergy, maxEnergy = max)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // TAP ENGINE
    // -------------------------------------------------------------
    fun onCoinTapped(context: Context) {
        val user = _userProfile.value ?: return
        if (user.status != "active") {
            showMessage("Account is ${user.status}", isError = true)
            return
        }
        if (!_isOnline.value) {
            showMessage("You are offline. Reconnect to tap and earn.", isError = true)
            return
        }

        val config = _appConfig.value
        if (config.maintenanceMode && user.role != "admin") {
            showMessage("App is in Maintenance Mode. Please check back soon.", isError = true)
            return
        }

        val currentEnergy = _energyRecord.value.currentEnergy
        val energyCost = config.energyPerTap.coerceAtLeast(1L)

        if (currentEnergy < energyCost) {
            showMessage("Energy depleted! Watch an ad or wait for refill.", isError = true)
            return
        }

        // Haptics & Sound
        SoundAndHaptics.vibrate(context, 18)
        SoundAndHaptics.playTapSound(viewModelScope)

        // Calculate booster multiplier
        val activeMult = _userBoosters.value.maxOfOrNull { 2 } ?: 1
        val coinsEarned = (config.coinsPerTap * activeMult).coerceAtLeast(1L)
        val newEnergy = (currentEnergy - energyCost).coerceAtLeast(0L)

        // Optimistic UI updates
        _energyRecord.value = _energyRecord.value.copy(currentEnergy = newEnergy)
        val curBalance = _userBalance.value
        _userBalance.value = curBalance.copy(
            coinBalance = curBalance.coinBalance + coinsEarned,
            lifetimeCoins = curBalance.lifetimeCoins + coinsEarned
        )

        // Spawn particle
        val pId = System.currentTimeMillis() + (0..999).random()
        val xRand = ((-60..60).random()).toFloat()
        val yRand = ((-40..40).random()).toFloat()
        val newParticle = TapParticle(pId, coinsEarned, xRand, yRand)
        _tapParticles.value = _tapParticles.value + newParticle
        viewModelScope.launch {
            delay(650L)
            _tapParticles.value = _tapParticles.value.filter { it.id != pId }
        }

        // Accumulate for batch sync
        pendingTaps++
        pendingCoins += coinsEarned
        scheduleTapSync(user.userId, newEnergy, _energyRecord.value.maxEnergy)
    }

    private fun scheduleTapSync(userId: String, remainingEnergy: Long, maxEnergy: Long) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(1200L) // Debounce batch interval
            val tapsToSync = pendingTaps
            val coinsToSync = pendingCoins
            pendingTaps = 0
            pendingCoins = 0L

            try {
                repository.syncTaps(userId, tapsToSync, coinsToSync, remainingEnergy, maxEnergy)
            } catch (e: Exception) {
                Log.e("TapSync", "Failed to sync taps: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // DAILY REWARD
    // -------------------------------------------------------------
    fun claimDailyCheckin() {
        val user = _userProfile.value ?: return
        val schedule = _appConfig.value.dailyRewardSchedule
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty { listOf(100L, 200L, 400L, 800L, 1500L, 3000L, 7500L) }

        viewModelScope.launch {
            val result = repository.claimDailyCheckin(user.userId, schedule)
            result.onSuccess { reward ->
                showMessage("Claimed Day reward: +$reward coins!")
            }.onFailure { e ->
                showMessage(e.message ?: "Failed to claim reward", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // TASKS
    // -------------------------------------------------------------
    fun submitTask(task: TaskItem, proofText: String, proofUrl: String) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            val submission = TaskSubmission(
                taskId = task.id,
                userId = user.userId,
                username = user.username,
                taskTitle = task.title,
                proofText = proofText,
                proofUrl = proofUrl,
                status = "pending",
                rewardCoins = task.rewardCoins
            )
            try {
                repository.submitTask(submission)
                showMessage("Task submitted for verification!")
            } catch (e: Exception) {
                showMessage(e.message ?: "Submission failed", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // BOOSTERS
    // -------------------------------------------------------------
    fun purchaseBooster(booster: Booster) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            val res = repository.purchaseBooster(user.userId, booster)
            res.onSuccess {
                showMessage("Activated booster: ${booster.name}!")
            }.onFailure { e ->
                showMessage(e.message ?: "Purchase failed", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // WITHDRAWAL
    // -------------------------------------------------------------
    fun requestWithdrawal(coinAmount: Long, method: String, details: String) {
        val user = _userProfile.value ?: return
        val config = _appConfig.value
        if (coinAmount < config.minWithdrawalCoins) {
            showMessage("Minimum withdrawal is ${config.minWithdrawalCoins} coins", isError = true)
            return
        }
        val usdAmount = coinAmount * config.coinToUsdRate

        viewModelScope.launch {
            val res = repository.submitWithdrawalRequest(
                userId = user.userId,
                username = user.username,
                coinAmount = coinAmount,
                usdAmount = usdAmount,
                rate = config.coinToUsdRate,
                method = method,
                details = details
            )
            res.onSuccess {
                showMessage("Withdrawal of $$usdAmount requested successfully!")
            }.onFailure { e ->
                showMessage(e.message ?: "Withdrawal request failed", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // REWARDED ADS
    // -------------------------------------------------------------
    fun onRewardedAdCompleted(refillEnergy: Boolean, bonusCoins: Long) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            try {
                repository.awardAdBonus(user.userId, bonusCoins, refillEnergy, _appConfig.value.baseEnergy)
                showMessage("Reward claimed: full energy & $bonusCoins coins!")
            } catch (e: Exception) {
                showMessage("Failed to award ad reward: ${e.message}", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // AI ADVISOR
    // -------------------------------------------------------------
    fun generateAiStrategy() {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiStrategyText.value = null
            val boosterNames = _userBoosters.value.map { it.boosterName }
            val res = aiAdvisor.getTappingStrategy(
                username = user.username,
                coins = _userBalance.value.coinBalance,
                energy = _energyRecord.value.currentEnergy,
                maxEnergy = _energyRecord.value.maxEnergy,
                totalTaps = user.totalTaps,
                activeBoosters = boosterNames
            )
            _isAiLoading.value = false
            res.onSuccess { text ->
                _aiStrategyText.value = text
            }.onFailure { e ->
                _aiStrategyText.value = "Advisor Error: ${e.message}"
            }
        }
    }

    fun dismissAiStrategy() {
        _aiStrategyText.value = null
    }

    // -------------------------------------------------------------
    // REFERRAL CODE APPLICATION
    // -------------------------------------------------------------
    fun applyReferralCode(code: String) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            val reward = _appConfig.value.referralReward
            val res = repository.processReferralCode(user.userId, user.username, code, reward)
            res.onSuccess {
                showMessage("Referral code applied! You received bonus coins!")
            }.onFailure { e ->
                showMessage(e.message ?: "Invalid code", isError = true)
            }
        }
    }

    // -------------------------------------------------------------
    // ADMIN ACTIONS
    // -------------------------------------------------------------
    fun adminSaveConfig(newConfig: AppConfiguration) {
        viewModelScope.launch {
            try {
                repository.saveAppConfig(newConfig)
                showMessage("Remote Configuration Published Successfully!")
            } catch (e: Exception) {
                showMessage("Failed to save config: ${e.message}", isError = true)
            }
        }
    }

    fun adminReviewSubmission(submission: TaskSubmission, approve: Boolean) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            try {
                repository.reviewTaskSubmission(
                    submissionId = submission.id,
                    targetUserId = submission.userId,
                    approve = approve,
                    rewardCoins = submission.rewardCoins,
                    adminUid = user.userId
                )
                showMessage("Submission marked as ${if (approve) "Approved" else "Rejected"}")
            } catch (e: Exception) {
                showMessage("Review failed: ${e.message}", isError = true)
            }
        }
    }

    fun adminUpdateWithdrawal(withdrawal: WithdrawalRequest, newStatus: String, notes: String) {
        viewModelScope.launch {
            try {
                repository.updateWithdrawalStatus(
                    withdrawalId = withdrawal.id,
                    targetUserId = withdrawal.userId,
                    coinAmount = withdrawal.coinAmount,
                    newStatus = newStatus,
                    notes = notes
                )
                showMessage("Withdrawal status updated to $newStatus")
            } catch (e: Exception) {
                showMessage("Failed to update status: ${e.message}", isError = true)
            }
        }
    }

    fun adminAdjustBalance(targetUserId: String, delta: Long, reason: String) {
        viewModelScope.launch {
            try {
                repository.adminAdjustBalance(targetUserId, delta, reason)
                showMessage("Balance adjusted by $delta coins")
            } catch (e: Exception) {
                showMessage("Failed: ${e.message}", isError = true)
            }
        }
    }

    fun adminSetUserStatus(targetUserId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.adminSetUserStatus(targetUserId, newStatus)
                showMessage("User status set to $newStatus")
            } catch (e: Exception) {
                showMessage("Failed: ${e.message}", isError = true)
            }
        }
    }

    fun adminSaveTask(task: TaskItem) {
        viewModelScope.launch {
            try {
                repository.saveTask(task)
                showMessage("Task saved successfully")
            } catch (e: Exception) {
                showMessage("Failed to save task: ${e.message}", isError = true)
            }
        }
    }

    fun adminDeleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                showMessage("Task deleted")
            } catch (e: Exception) {
                showMessage("Failed: ${e.message}", isError = true)
            }
        }
    }

    fun adminSaveAnnouncement(item: AnnouncementItem) {
        viewModelScope.launch {
            try {
                repository.saveAnnouncement(item)
                showMessage("Announcement published!")
            } catch (e: Exception) {
                showMessage("Failed to publish: ${e.message}", isError = true)
            }
        }
    }

    fun adminDeleteAnnouncement(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(id)
                showMessage("Announcement deleted")
            } catch (e: Exception) {
                showMessage("Failed to delete: ${e.message}", isError = true)
            }
        }
    }

    fun showMessage(msg: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(text = msg, isError = isError)
    }

    fun dismissMessage() {
        _uiMessage.value = null
    }
}
