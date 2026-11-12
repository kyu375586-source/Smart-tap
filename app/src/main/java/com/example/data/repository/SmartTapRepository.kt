package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SmartTapRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCurrentUserAdmin(): Boolean {
        val email = auth.currentUser?.email
        return email != null && email.equals("kyu375586@gmail.com", ignoreCase = true)
    }

    // -------------------------------------------------------------
    // REMOTE CONFIGURATION
    // -------------------------------------------------------------
    fun observeAppConfig(): Flow<AppConfiguration> = callbackFlow {
        val docRef = db.collection("app_configuration").document("default")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val config = snapshot.toObject(AppConfiguration::class.java) ?: AppConfiguration()
                trySend(config)
            } else {
                // Return default configuration
                val defaultConfig = AppConfiguration()
                trySend(defaultConfig)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAppConfig(config: AppConfiguration) {
        val docRef = db.collection("app_configuration").document("default")
        try {
            val payload = mapOf(
                "appName" to config.appName,
                "coinsPerTap" to config.coinsPerTap,
                "baseEnergy" to config.baseEnergy,
                "energyPerTap" to config.energyPerTap,
                "energyRegenPerSec" to config.energyRegenPerSec,
                "dailyRewardSchedule" to config.dailyRewardSchedule,
                "referralReward" to config.referralReward,
                "minWithdrawalCoins" to config.minWithdrawalCoins,
                "coinToUsdRate" to config.coinToUsdRate,
                "maintenanceMode" to config.maintenanceMode,
                "bannerNotice" to config.bannerNotice,
                "updatedBy" to (auth.currentUser?.email ?: "admin"),
                "updatedAt" to FieldValue.serverTimestamp(),
                "version" to (config.version + 1)
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // USER PROFILE & INITIALIZATION
    // -------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            val profile = snapshot?.toObject(UserProfile::class.java)
            trySend(profile)
        }
        awaitClose { listener.remove() }
    }

    suspend fun initializeUserIfMissing(
        userId: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        val userDoc = db.collection("users").document(userId)
        val snapshot = try {
            userDoc.get().await()
        } catch (e: Exception) {
            null
        }

        if (snapshot == null || !snapshot.exists()) {
            val username = displayName?.ifBlank { "Tapper_${userId.take(5)}" } ?: "Tapper_${userId.take(5)}"
            val referralCode = "TAP_${userId.take(6).uppercase()}"
            val isAdmin = email.equals("kyu375586@gmail.com", ignoreCase = true)
            val role = if (isAdmin) "admin" else "user"

            val userMap = mapOf(
                "userId" to userId,
                "username" to username,
                "email" to (email ?: ""),
                "photoUrl" to (photoUrl ?: ""),
                "referralCode" to referralCode,
                "referredBy" to "",
                "currentLevel" to 1,
                "totalTaps" to 0L,
                "totalEarnings" to 0L,
                "role" to role,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc.set(userMap).await()

            // Initialize balance
            val balanceDoc = db.collection("user_balances").document(userId)
            val balanceMap = mapOf(
                "userId" to userId,
                "coinBalance" to 0L,
                "withdrawableBalance" to 0L,
                "lifetimeCoins" to 0L,
                "pendingCoins" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            balanceDoc.set(balanceMap).await()

            // Initialize energy
            val energyDoc = db.collection("energy_records").document(userId)
            val energyMap = mapOf(
                "userId" to userId,
                "currentEnergy" to 1000L,
                "maxEnergy" to 1000L,
                "lastRechargeTime" to System.currentTimeMillis(),
                "rechargeRatePerSec" to 3L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            energyDoc.set(energyMap).await()

            // Initialize daily checkin
            val checkinDoc = db.collection("daily_checkins").document(userId)
            val checkinMap = mapOf(
                "userId" to userId,
                "streakCount" to 0,
                "lastClaimDate" to "",
                "totalClaimedDays" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            checkinDoc.set(checkinMap).await()

            // Welcome transaction
            val txDoc = db.collection("transactions").document()
            val txMap = mapOf(
                "userId" to userId,
                "type" to "tap_reward",
                "amount" to 0L,
                "description" to "Welcome to SMART TAP!",
                "status" to "completed",
                "balanceAfter" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            txDoc.set(txMap).await()
        }
    }

    suspend fun updateUsername(userId: String, newName: String) {
        val userDoc = db.collection("users").document(userId)
        userDoc.update(
            mapOf(
                "username" to newName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // -------------------------------------------------------------
    // BALANCES & TRANSACTIONS
    // -------------------------------------------------------------
    fun observeUserBalance(userId: String): Flow<UserBalance> = callbackFlow {
        val docRef = db.collection("user_balances").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val balance = snapshot.toObject(UserBalance::class.java) ?: UserBalance(userId = userId)
                trySend(balance)
            } else {
                trySend(UserBalance(userId = userId))
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeTransactions(userId: String): Flow<List<TransactionItem>> = callbackFlow {
        val query = db.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "transactions")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TransactionItem::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------
    // ENERGY SYSTEM & TAPPING SYNC
    // -------------------------------------------------------------
    fun observeEnergy(userId: String): Flow<EnergyRecord> = callbackFlow {
        val docRef = db.collection("energy_records").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val record = snapshot.toObject(EnergyRecord::class.java) ?: EnergyRecord(userId = userId)
                trySend(record)
            } else {
                trySend(EnergyRecord(userId = userId))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun syncTaps(
        userId: String,
        tapsCount: Int,
        coinsEarned: Long,
        remainingEnergy: Long,
        maxEnergy: Long
    ) {
        if (tapsCount <= 0) return
        val balanceRef = db.collection("user_balances").document(userId)
        val energyRef = db.collection("energy_records").document(userId)
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val balanceSnap = transaction.get(balanceRef)
            val currentCoins = balanceSnap.getLong("coinBalance") ?: 0L
            val currentWithdrawable = balanceSnap.getLong("withdrawableBalance") ?: 0L
            val currentLifetime = balanceSnap.getLong("lifetimeCoins") ?: 0L

            val newCoins = currentCoins + coinsEarned
            val newWithdrawable = currentWithdrawable + coinsEarned
            val newLifetime = currentLifetime + coinsEarned

            transaction.update(
                balanceRef,
                mapOf(
                    "coinBalance" to newCoins,
                    "withdrawableBalance" to newWithdrawable,
                    "lifetimeCoins" to newLifetime,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            transaction.update(
                energyRef,
                mapOf(
                    "currentEnergy" to remainingEnergy.coerceIn(0L, maxEnergy),
                    "lastRechargeTime" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Update user stats
            transaction.update(
                userRef,
                mapOf(
                    "totalTaps" to FieldValue.increment(tapsCount.toLong()),
                    "totalEarnings" to newLifetime,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun refillEnergy(userId: String, maxEnergy: Long) {
        val energyRef = db.collection("energy_records").document(userId)
        energyRef.update(
            mapOf(
                "currentEnergy" to maxEnergy,
                "lastRechargeTime" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // -------------------------------------------------------------
    // DAILY CHECK-IN REWARDS
    // -------------------------------------------------------------
    fun observeDailyCheckin(userId: String): Flow<DailyCheckin> = callbackFlow {
        val docRef = db.collection("daily_checkins").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            val record = snapshot?.toObject(DailyCheckin::class.java) ?: DailyCheckin(userId = userId)
            trySend(record)
        }
        awaitClose { listener.remove() }
    }

    suspend fun claimDailyCheckin(userId: String, schedule: List<Long>): Result<Long> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val checkinRef = db.collection("daily_checkins").document(userId)
        val balanceRef = db.collection("user_balances").document(userId)
        val txRef = db.collection("transactions").document()

        return try {
            var rewardCoinsAwarded = 0L
            db.runTransaction { tx ->
                val checkinSnap = tx.get(checkinRef)
                val lastDate = checkinSnap.getString("lastClaimDate") ?: ""
                if (lastDate == todayStr) {
                    throw IllegalStateException("Already claimed today!")
                }

                val currentStreak = (checkinSnap.getLong("streakCount") ?: 0L).toInt()
                val totalDays = (checkinSnap.getLong("totalClaimedDays") ?: 0L).toInt()
                val newStreak = (currentStreak % schedule.size) + 1
                val reward = schedule.getOrElse(newStreak - 1) { 100L }
                rewardCoinsAwarded = reward

                tx.update(
                    checkinRef,
                    mapOf(
                        "streakCount" to newStreak,
                        "lastClaimDate" to todayStr,
                        "totalClaimedDays" to (totalDays + 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val balanceSnap = tx.get(balanceRef)
                val coins = balanceSnap.getLong("coinBalance") ?: 0L
                val withDr = balanceSnap.getLong("withdrawableBalance") ?: 0L
                val life = balanceSnap.getLong("lifetimeCoins") ?: 0L
                val newBalance = coins + reward

                tx.update(
                    balanceRef,
                    mapOf(
                        "coinBalance" to newBalance,
                        "withdrawableBalance" to (withDr + reward),
                        "lifetimeCoins" to (life + reward),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to userId,
                        "type" to "daily_bonus",
                        "amount" to reward,
                        "description" to "Day $newStreak Daily Check-in Bonus",
                        "status" to "completed",
                        "balanceAfter" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(rewardCoinsAwarded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // TASKS & SUBMISSIONS
    // -------------------------------------------------------------
    fun observeTasks(): Flow<List<TaskItem>> = callbackFlow {
        val query = db.collection("tasks").whereEqualTo("isActive", true)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "tasks")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TaskItem::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeAllTasksAdmin(): Flow<List<TaskItem>> = callbackFlow {
        val listener = db.collection("tasks").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "tasks")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TaskItem::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTask(task: TaskItem) {
        val docRef = if (task.id.isBlank()) db.collection("tasks").document() else db.collection("tasks").document(task.id)
        val map = mapOf(
            "title" to task.title,
            "description" to task.description,
            "category" to task.category,
            "rewardCoins" to task.rewardCoins,
            "actionUrl" to task.actionUrl,
            "iconName" to task.iconName,
            "isActive" to task.isActive,
            "requirements" to task.requirements,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(map).await()
    }

    suspend fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete().await()
    }

    fun observeUserSubmissions(userId: String): Flow<List<TaskSubmission>> = callbackFlow {
        val query = db.collection("task_submissions").whereEqualTo("userId", userId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "task_submissions")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TaskSubmission::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeAllSubmissionsAdmin(): Flow<List<TaskSubmission>> = callbackFlow {
        val listener = db.collection("task_submissions")
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "task_submissions")
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(TaskSubmission::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitTask(submission: TaskSubmission) {
        val docRef = db.collection("task_submissions").document()
        val map = mapOf(
            "taskId" to submission.taskId,
            "userId" to submission.userId,
            "username" to submission.username,
            "taskTitle" to submission.taskTitle,
            "proofText" to submission.proofText,
            "proofUrl" to submission.proofUrl,
            "status" to "pending",
            "rewardCoins" to submission.rewardCoins,
            "submittedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(map).await()
    }

    suspend fun reviewTaskSubmission(
        submissionId: String,
        targetUserId: String,
        approve: Boolean,
        rewardCoins: Long,
        adminUid: String
    ) {
        val subRef = db.collection("task_submissions").document(submissionId)
        val balanceRef = db.collection("user_balances").document(targetUserId)
        val txRef = db.collection("transactions").document()

        db.runTransaction { tx ->
            val status = if (approve) "approved" else "rejected"
            tx.update(
                subRef,
                mapOf(
                    "status" to status,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to adminUid
                )
            )

            if (approve && rewardCoins > 0) {
                val balanceSnap = tx.get(balanceRef)
                val coins = balanceSnap.getLong("coinBalance") ?: 0L
                val withDr = balanceSnap.getLong("withdrawableBalance") ?: 0L
                val life = balanceSnap.getLong("lifetimeCoins") ?: 0L
                val newBal = coins + rewardCoins

                tx.update(
                    balanceRef,
                    mapOf(
                        "coinBalance" to newBal,
                        "withdrawableBalance" to (withDr + rewardCoins),
                        "lifetimeCoins" to (life + rewardCoins),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to targetUserId,
                        "type" to "task_reward",
                        "amount" to rewardCoins,
                        "description" to "Task Reward Approved",
                        "status" to "completed",
                        "balanceAfter" to newBal,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
    }

    // -------------------------------------------------------------
    // BOOSTERS & UPGRADES
    // -------------------------------------------------------------
    fun observeBoosters(): Flow<List<Booster>> = callbackFlow {
        val listener = db.collection("boosters").whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "boosters")
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Booster::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeAllBoostersAdmin(): Flow<List<Booster>> = callbackFlow {
        val listener = db.collection("boosters").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "boosters")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(Booster::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveBooster(booster: Booster) {
        val docRef = if (booster.id.isBlank()) db.collection("boosters").document() else db.collection("boosters").document(booster.id)
        val map = mapOf(
            "name" to booster.name,
            "description" to booster.description,
            "multiplier" to booster.multiplier,
            "price" to booster.price,
            "durationSeconds" to booster.durationSeconds,
            "boosterType" to booster.boosterType,
            "upgradeLevel" to booster.upgradeLevel,
            "isActive" to booster.isActive
        )
        docRef.set(map).await()
    }

    suspend fun deleteBooster(boosterId: String) {
        db.collection("boosters").document(boosterId).delete().await()
    }

    fun observeUserActivePurchases(userId: String): Flow<List<BoosterPurchase>> = callbackFlow {
        val query = db.collection("booster_purchases")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isActive", true)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "booster_purchases")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(BoosterPurchase::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun purchaseBooster(userId: String, booster: Booster): Result<Unit> {
        val balanceRef = db.collection("user_balances").document(userId)
        val purchaseRef = db.collection("booster_purchases").document()
        val txRef = db.collection("transactions").document()

        return try {
            db.runTransaction { tx ->
                val balanceSnap = tx.get(balanceRef)
                val coins = balanceSnap.getLong("coinBalance") ?: 0L
                if (coins < booster.price) {
                    throw IllegalStateException("Insufficient coin balance (${coins} < ${booster.price})")
                }

                val newBal = coins - booster.price
                tx.update(
                    balanceRef,
                    mapOf(
                        "coinBalance" to newBal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val now = System.currentTimeMillis()
                val expiresAt = Timestamp(Date(now + booster.durationSeconds * 1000))

                tx.set(
                    purchaseRef,
                    mapOf(
                        "userId" to userId,
                        "boosterId" to booster.id,
                        "boosterName" to booster.name,
                        "isActive" to true,
                        "purchasedAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to expiresAt
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to userId,
                        "type" to "booster_purchase",
                        "amount" to -booster.price,
                        "description" to "Purchased ${booster.name}",
                        "status" to "completed",
                        "balanceAfter" to newBal,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // WITHDRAWALS
    // -------------------------------------------------------------
    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        val query = db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .orderBy("requestDate", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "withdrawals")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(WithdrawalRequest::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeAllWithdrawalsAdmin(): Flow<List<WithdrawalRequest>> = callbackFlow {
        val query = db.collection("withdrawals").orderBy("requestDate", Query.Direction.DESCENDING).limit(100)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "withdrawals")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(WithdrawalRequest::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun submitWithdrawalRequest(
        userId: String,
        username: String,
        coinAmount: Long,
        usdAmount: Double,
        rate: Double,
        method: String,
        details: String
    ): Result<Unit> {
        val balanceRef = db.collection("user_balances").document(userId)
        val withRef = db.collection("withdrawals").document()
        val txRef = db.collection("transactions").document()

        return try {
            db.runTransaction { tx ->
                val balanceSnap = tx.get(balanceRef)
                val coins = balanceSnap.getLong("coinBalance") ?: 0L
                val withDr = balanceSnap.getLong("withdrawableBalance") ?: 0L
                if (coins < coinAmount || withDr < coinAmount) {
                    throw IllegalStateException("Insufficient withdrawable coin balance")
                }

                val newCoins = coins - coinAmount
                val newWithDr = withDr - coinAmount

                tx.update(
                    balanceRef,
                    mapOf(
                        "coinBalance" to newCoins,
                        "withdrawableBalance" to newWithDr,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    withRef,
                    mapOf(
                        "userId" to userId,
                        "username" to username,
                        "coinAmount" to coinAmount,
                        "conversionRate" to rate,
                        "withdrawalAmount" to usdAmount,
                        "paymentMethod" to method,
                        "paymentDetails" to details,
                        "status" to "Pending",
                        "adminNotes" to "",
                        "transactionRef" to "WTH-${UUID.randomUUID().toString().take(8).uppercase()}",
                        "requestDate" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to userId,
                        "type" to "withdrawal",
                        "amount" to -coinAmount,
                        "description" to "Withdrawal via $method ($$usdAmount)",
                        "status" to "pending",
                        "balanceAfter" to newCoins,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWithdrawalStatus(
        withdrawalId: String,
        targetUserId: String,
        coinAmount: Long,
        newStatus: String,
        notes: String
    ) {
        val withRef = db.collection("withdrawals").document(withdrawalId)
        val balanceRef = db.collection("user_balances").document(targetUserId)

        db.runTransaction { tx ->
            tx.update(
                withRef,
                mapOf(
                    "status" to newStatus,
                    "adminNotes" to notes,
                    "processedDate" to FieldValue.serverTimestamp()
                )
            )

            // If rejected, refund the coins
            if (newStatus == "Rejected") {
                val balanceSnap = tx.get(balanceRef)
                val coins = balanceSnap.getLong("coinBalance") ?: 0L
                val withDr = balanceSnap.getLong("withdrawableBalance") ?: 0L
                tx.update(
                    balanceRef,
                    mapOf(
                        "coinBalance" to (coins + coinAmount),
                        "withdrawableBalance" to (withDr + coinAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
    }

    // -------------------------------------------------------------
    // LEADERBOARD & USERS
    // -------------------------------------------------------------
    fun observeLeaderboard(): Flow<List<UserProfile>> = callbackFlow {
        val query = db.collection("users")
            .whereEqualTo("status", "active")
            .limit(50)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "users")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(UserProfile::class.java).orEmpty()
                .sortedByDescending { it.totalEarnings }
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = callbackFlow {
        val listener = db.collection("users")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users")
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(UserProfile::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun adminAdjustBalance(targetUserId: String, delta: Long, reason: String) {
        val balanceRef = db.collection("user_balances").document(targetUserId)
        val userRef = db.collection("users").document(targetUserId)
        val txRef = db.collection("transactions").document()

        db.runTransaction { tx ->
            val bSnap = tx.get(balanceRef)
            val coins = bSnap.getLong("coinBalance") ?: 0L
            val withDr = bSnap.getLong("withdrawableBalance") ?: 0L
            val life = bSnap.getLong("lifetimeCoins") ?: 0L

            val newCoins = (coins + delta).coerceAtLeast(0L)
            val newWithDr = (withDr + delta).coerceAtLeast(0L)
            val newLife = if (delta > 0) life + delta else life

            tx.update(
                balanceRef,
                mapOf(
                    "coinBalance" to newCoins,
                    "withdrawableBalance" to newWithDr,
                    "lifetimeCoins" to newLife,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.update(
                userRef,
                mapOf(
                    "totalEarnings" to newLife,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                txRef,
                mapOf(
                    "userId" to targetUserId,
                    "type" to "admin_adjustment",
                    "amount" to delta,
                    "description" to "Admin Adjustment: $reason",
                    "status" to "completed",
                    "balanceAfter" to newCoins,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun adminSetUserStatus(targetUserId: String, newStatus: String) {
        db.collection("users").document(targetUserId).update(
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // -------------------------------------------------------------
    // ANNOUNCEMENTS
    // -------------------------------------------------------------
    fun observeAnnouncements(): Flow<List<AnnouncementItem>> = callbackFlow {
        val query = db.collection("announcements").whereEqualTo("isActive", true)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "announcements")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(AnnouncementItem::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAnnouncement(item: AnnouncementItem) {
        val docRef = if (item.id.isBlank()) db.collection("announcements").document() else db.collection("announcements").document(item.id)
        val map = mapOf(
            "heading" to item.heading,
            "description" to item.description,
            "imageUrl" to item.imageUrl,
            "linkUrl" to item.linkUrl,
            "actionText" to item.actionText,
            "isActive" to item.isActive,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(map).await()
    }

    suspend fun deleteAnnouncement(id: String) {
        db.collection("announcements").document(id).delete().await()
    }

    // -------------------------------------------------------------
    // AD REWARD
    // -------------------------------------------------------------
    suspend fun awardAdBonus(userId: String, coins: Long, refillEnergy: Boolean, maxEnergy: Long) {
        val balanceRef = db.collection("user_balances").document(userId)
        val energyRef = db.collection("energy_records").document(userId)
        val userRef = db.collection("users").document(userId)
        val txRef = db.collection("transactions").document()

        db.runTransaction { tx ->
            val bSnap = tx.get(balanceRef)
            val currentCoins = bSnap.getLong("coinBalance") ?: 0L
            val currentWithDr = bSnap.getLong("withdrawableBalance") ?: 0L
            val currentLife = bSnap.getLong("lifetimeCoins") ?: 0L

            val newCoins = currentCoins + coins
            val newWithDr = currentWithDr + coins
            val newLife = currentLife + coins

            tx.update(
                balanceRef,
                mapOf(
                    "coinBalance" to newCoins,
                    "withdrawableBalance" to newWithDr,
                    "lifetimeCoins" to newLife,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.update(
                userRef,
                mapOf(
                    "totalEarnings" to newLife,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (refillEnergy) {
                tx.update(
                    energyRef,
                    mapOf(
                        "currentEnergy" to maxEnergy,
                        "lastRechargeTime" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            tx.set(
                txRef,
                mapOf(
                    "userId" to userId,
                    "type" to "ad_reward",
                    "amount" to coins,
                    "description" to if (refillEnergy) "Rewarded Ad: Full Energy & $coins Coins" else "Rewarded Ad: $coins Coins",
                    "status" to "completed",
                    "balanceAfter" to newCoins,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    // -------------------------------------------------------------
    // REFERRALS
    // -------------------------------------------------------------
    fun observeReferrals(userId: String): Flow<List<ReferralItem>> = callbackFlow {
        val query = db.collection("referrals").whereEqualTo("referrerUid", userId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "referrals")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(ReferralItem::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun processReferralCode(newUserId: String, newUsername: String, code: String, reward: Long): Result<Boolean> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return Result.success(false)

        return try {
            val query = db.collection("users").whereEqualTo("referralCode", cleanCode).limit(1).get().await()
            if (query.isEmpty) return Result.failure(Exception("Referral code not found"))

            val referrerDoc = query.documents.first()
            val referrerUid = referrerDoc.getString("userId") ?: return Result.failure(Exception("Invalid referrer"))

            if (referrerUid == newUserId) {
                return Result.failure(Exception("Cannot refer yourself"))
            }

            val userRef = db.collection("users").document(newUserId)
            userRef.update(
                mapOf(
                    "referredBy" to referrerUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val refDoc = db.collection("referrals").document()
            refDoc.set(
                mapOf(
                    "referrerUid" to referrerUid,
                    "referredUid" to newUserId,
                    "referredUsername" to newUsername,
                    "rewardAmount" to reward,
                    "status" to "claimed",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Credit referrer
            adminAdjustBalance(referrerUid, reward, "Referral Bonus for inviting $newUsername")
            // Credit new user welcome bonus
            adminAdjustBalance(newUserId, reward / 2, "Welcome Referral Bonus")

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
