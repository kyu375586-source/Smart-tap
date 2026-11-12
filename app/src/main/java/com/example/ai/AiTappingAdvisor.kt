package com.example.ai

import kotlinx.coroutines.delay

class AiTappingAdvisor {

    suspend fun getTappingStrategy(
        username: String,
        coins: Long,
        energy: Long,
        maxEnergy: Long,
        totalTaps: Long,
        activeBoosters: List<String>
    ): Result<String> {
        delay(400L) // Smooth simulation delay for responsive UX

        val energyPercent = if (maxEnergy > 0) ((energy * 100) / maxEnergy).toInt() else 0
        val boosterStatus = if (activeBoosters.isEmpty()) "No active multipliers" else activeBoosters.joinToString(", ")

        val tip1 = when {
            energyPercent < 20 -> "⚡ Energy Critical ($energyPercent%): Watch a Rewarded Ad now in Home to immediately restore 100% capacity and snag +250 free coins!"
            energyPercent < 50 -> "🔋 Energy Reserve at $energyPercent%: Tap in bursts of 50 to let passive regeneration cycle between taps without stalling."
            else -> "⚡ Prime Energy ($energyPercent%): Dump your energy pool with rapid taps! Perfect moment to activate a Multitap booster."
        }

        val tip2 = when {
            coins < 500 -> "💰 Growth Phase: Focus on the Tasks tab — complete community and telegram quests to jumpstart your bankroll to 2,500+ coins."
            coins < 5000 -> "🚀 Scaling Phase: Reinvest 300 coins into the Multitap x2 booster to double your coin yield per tap for 5 full minutes."
            else -> "💎 Whale Tier: You have $coins coins! Accumulate toward the minimum payout threshold or unlock the Turbo Recharge upgrade."
        }

        val tip3 = "🎯 Milestone Target: Tap 500 times today to claim the Daily Sprint achievement. Current lifetime taps: $totalTaps."

        val strategyText = """
            🔥 SMART TAP TACTICAL ADVISORY FOR ${username.uppercase()}
            
            • $tip1
            • $tip2
            • $tip3
            
            ⚡ Active Multipliers: $boosterStatus
            💡 Pro Tip: Energy regenerates 24/7. Check back every 10 minutes to drain full tank!
        """.trimIndent()

        return Result.success(strategyText)
    }

    suspend fun generateTaskIdea(category: String): Result<Pair<String, String>> {
        delay(300L)
        val suggestions = when (category.uppercase()) {
            "SOCIAL" -> Pair("Retweet Daily Milestone", "Share your latest coin balance screenshot on X and tag #SmartTap.")
            "DAILY" -> Pair("Golden Hour Sprint", "Perform 300 taps within a single session before energy reaches zero.")
            "REFERRAL" -> Pair("Squad Builder", "Invite 2 new players with your referral code to earn 5,000 bonus coins.")
            else -> Pair("Community Advocate", "Leave a 5-star review and submit your profile screenshot for 1,500 coins.")
        }
        return Result.success(suggestions)
    }
}
