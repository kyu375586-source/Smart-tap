package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val referralCode: String = "",
    val referredBy: String? = null,
    val currentLevel: Int = 1,
    val totalTaps: Long = 0L,
    val totalEarnings: Long = 0L,
    val role: String = "user",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBalance(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coinBalance: Long = 0L,
    val withdrawableBalance: Long = 0L,
    val lifetimeCoins: Long = 0L,
    val pendingCoins: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EnergyRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currentEnergy: Long = 1000L,
    val maxEnergy: Long = 1000L,
    val lastRechargeTime: Long = System.currentTimeMillis(),
    val rechargeRatePerSec: Long = 3L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyCheckin(
    @DocumentId val id: String = "",
    val userId: String = "",
    val streakCount: Int = 0,
    val lastClaimDate: String = "",
    val totalClaimedDays: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "daily",
    val rewardCoins: Long = 100L,
    val actionUrl: String = "",
    val iconName: String = "star",
    val isActive: Boolean = true,
    val requirements: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskSubmission(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val username: String = "",
    val taskTitle: String = "",
    val proofText: String = "",
    val proofUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    val rewardCoins: Long = 0L,
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null,
    val reviewedBy: String? = null
)

@IgnoreExtraProperties
data class Booster(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val multiplier: Int = 2,
    val price: Long = 500L,
    val durationSeconds: Long = 300L,
    val boosterType: String = "multitap",
    val upgradeLevel: Int = 1,
    val isActive: Boolean = true
)

@IgnoreExtraProperties
data class BoosterPurchase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val boosterId: String = "",
    val boosterName: String = "",
    @ServerTimestamp val purchasedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val isActive: Boolean = true
)

@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val coinAmount: Long = 0L,
    val conversionRate: Double = 0.001,
    val withdrawalAmount: Double = 0.0,
    val paymentMethod: String = "PayPal",
    val paymentDetails: String = "",
    val status: String = "Pending", // Pending, Under Review, Approved, Processing, Completed, Rejected
    val adminNotes: String = "",
    val transactionRef: String = "",
    @ServerTimestamp val requestDate: Timestamp? = null,
    @ServerTimestamp val processedDate: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "tap_reward",
    val amount: Long = 0L,
    val description: String = "",
    val status: String = "completed",
    val balanceAfter: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReferralItem(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val referredUsername: String = "",
    val rewardAmount: Long = 500L,
    val status: String = "claimed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AnnouncementItem(
    @DocumentId val id: String = "",
    val heading: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val actionText: String = "Learn More",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfiguration(
    @DocumentId val id: String = "default",
    val appName: String = "Smart Tap",
    val coinsPerTap: Long = 1L,
    val baseEnergy: Long = 1000L,
    val energyPerTap: Long = 1L,
    val energyRegenPerSec: Long = 3L,
    val dailyRewardSchedule: String = "100,200,400,800,1500,3000,7500",
    val referralReward: Long = 500L,
    val minWithdrawalCoins: Long = 5000L,
    val coinToUsdRate: Double = 0.001,
    val maintenanceMode: Boolean = false,
    val bannerNotice: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val version: Long = 1L
)

@IgnoreExtraProperties
data class AdminActivityLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
