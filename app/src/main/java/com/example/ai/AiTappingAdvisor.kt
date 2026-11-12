package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class AiTappingAdvisor {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    suspend fun getTappingStrategy(
        username: String,
        coins: Long,
        energy: Long,
        maxEnergy: Long,
        totalTaps: Long,
        activeBoosters: List<String>
    ): Result<String> {
        return try {
            val prompt = """
                You are SMART TAP's elite Fintech & Gaming Strategy Advisor.
                User Stats:
                - Player: $username
                - Coin Balance: $coins
                - Current Energy: $energy / $maxEnergy
                - Total Lifetime Taps: $totalTaps
                - Active Boosters: ${if (activeBoosters.isEmpty()) "None" else activeBoosters.joinToString(", ")}

                Give a concise, punchy 3-bullet action plan for maximum coin earning rate, energy management, and booster strategy. Include estimated time to next milestone. Keep it energetic, motivating, and gamified!
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Advisor returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AiAdvisor", "Strategy generation timed out", e)
            Result.failure(Exception("AI strategy request timed out. Please check network connection."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiAdvisor", "Failed to generate strategy", e)
            Result.failure(e)
        }
    }

    suspend fun generateTaskIdea(category: String): Result<Pair<String, String>> {
        return try {
            val prompt = """
                Suggest 1 engaging, realistic viral challenge or task for an online rewards tapping app.
                Category: $category
                Respond in format:
                Title: <Short Title>
                Description: <Short Instructions under 20 words>
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text.orEmpty()
            val title = text.lines().firstOrNull { it.startsWith("Title:", ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: "Special Community Quest"
            val desc = text.lines().firstOrNull { it.startsWith("Description:", ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: "Complete the quest and submit proof for bonus coins."

            Result.success(Pair(title, desc))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
