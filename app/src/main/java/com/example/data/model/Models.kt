package com.example.data.model

data class UserProfile(
    val id: String = "",
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class UserBalance(
    val id: String = "",
    val userId: String = "",
    val coinBalance: Long = 0L,
    val withdrawableBalance: Long = 0L,
    val lifetimeCoins: Long = 0L,
    val pendingCoins: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

data class EnergyRecord(
    val id: String = "",
    val userId: String = "",
    val currentEnergy: Long = 1000L,
    val maxEnergy: Long = 1000L,
    val lastRechargeTime: Long = System.currentTimeMillis(),
    val rechargeRatePerSec: Long = 3L,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DailyCheckin(
    val id: String = "",
    val userId: String = "",
    val streakCount: Int = 0,
    val lastClaimDate: String = "",
    val totalClaimedDays: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "daily",
    val rewardCoins: Long = 100L,
    val actionUrl: String = "",
    val iconName: String = "star",
    val isActive: Boolean = true,
    val requirements: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskSubmission(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val username: String = "",
    val taskTitle: String = "",
    val proofText: String = "",
    val proofUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    val rewardCoins: Long = 0L,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null
)

data class Booster(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val multiplier: Int = 2,
    val price: Long = 500L,
    val durationSeconds: Long = 300L,
    val boosterType: String = "multitap",
    val upgradeLevel: Int = 1,
    val isActive: Boolean = true
)

data class BoosterPurchase(
    val id: String = "",
    val userId: String = "",
    val boosterId: String = "",
    val boosterName: String = "",
    val purchasedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true
)

data class WithdrawalRequest(
    val id: String = "",
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
    val requestDate: Long = System.currentTimeMillis(),
    val processedDate: Long? = null
)

data class TransactionItem(
    val id: String = "",
    val userId: String = "",
    val type: String = "tap_reward",
    val amount: Long = 0L,
    val description: String = "",
    val status: String = "completed",
    val balanceAfter: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReferralItem(
    val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val referredUsername: String = "",
    val rewardAmount: Long = 500L,
    val status: String = "claimed",
    val createdAt: Long = System.currentTimeMillis()
)

data class AnnouncementItem(
    val id: String = "",
    val heading: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val actionText: String = "Learn More",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppConfiguration(
    val id: String = "default",
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
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

data class AdminActivityLog(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
