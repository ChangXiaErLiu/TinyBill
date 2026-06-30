package com.tinybill.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class BudgetChallenge(
    val id: String = "",
    val title: String = "",
    val category: String? = null,
    val targetAmount: Double? = null,
    val challengeType: ChallengeType = ChallengeType.NO_SPENDING,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = 0,
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalDays: Int = 0,
    val completedDays: Int = 0
) {
    enum class ChallengeType {
        NO_SPENDING,
        UNDER_BUDGET,
        DAILY_LIMIT
    }

    enum class ChallengeStatus {
        ACTIVE,
        COMPLETED,
        FAILED,
        EXPIRED
    }
}

class BudgetChallengeManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currentChallenge = MutableStateFlow<BudgetChallenge?>(getCurrentChallenge())
    private val _challengeHistory = MutableStateFlow(getChallengeHistory())
    private val _streakData = MutableStateFlow(getStreakData())

    companion object {
        private const val PREFS_NAME = "tinybill_budget_challenge"
        private const val KEY_CURRENT_CHALLENGE = "current_challenge"
        private const val KEY_CHALLENGE_HISTORY = "challenge_history"
        private const val KEY_STREAK_DATA = "streak_data"
        private const val KEY_LAST_CHECK_DATE = "last_check_date"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_MAX_STREAK = "max_streak"

        @Volatile
        private var INSTANCE: BudgetChallengeManager? = null

        fun getInstance(context: Context): BudgetChallengeManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BudgetChallengeManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun getCurrentChallengeFlow(): StateFlow<BudgetChallenge?> = _currentChallenge.asStateFlow()

    fun getChallengeHistoryFlow(): StateFlow<List<BudgetChallenge>> = _challengeHistory.asStateFlow()

    fun getStreakDataFlow(): StateFlow<StreakData> = _streakData.asStateFlow()

    fun createChallenge(
        title: String,
        category: String? = null,
        targetAmount: Double? = null,
        challengeType: BudgetChallenge.ChallengeType,
        days: Int
    ): BudgetChallenge {
        val calendar = Calendar.getInstance()
        val startDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, days)
        val endDate = calendar.timeInMillis

        val challenge = BudgetChallenge(
            id = System.currentTimeMillis().toString(),
            title = title,
            category = category,
            targetAmount = targetAmount,
            challengeType = challengeType,
            startDate = startDate,
            endDate = endDate,
            status = BudgetChallenge.ChallengeStatus.ACTIVE,
            currentStreak = 0,
            maxStreak = 0,
            totalDays = days,
            completedDays = 0
        )

        saveCurrentChallenge(challenge)
        _currentChallenge.value = challenge
        return challenge
    }

    fun updateChallengeProgress(
        hasUnderBudget: Boolean,
        spentAmount: Double? = null
    ): BudgetChallenge? {
        val challenge = _currentChallenge.value ?: return null
        if (challenge.status != BudgetChallenge.ChallengeStatus.ACTIVE) return challenge

        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        if (today > challenge.endDate) {
            val failedChallenge = challenge.copy(status = BudgetChallenge.ChallengeStatus.EXPIRED)
            addToHistory(failedChallenge)
            _currentChallenge.value = null
            clearCurrentChallenge()
            return failedChallenge
        }

        var newStreak = challenge.currentStreak
        var newMaxStreak = challenge.maxStreak
        var newCompletedDays = challenge.completedDays

        when (challenge.challengeType) {
            BudgetChallenge.ChallengeType.NO_SPENDING -> {
                if (hasUnderBudget) {
                    newStreak++
                    newCompletedDays++
                    if (newStreak > newMaxStreak) {
                        newMaxStreak = newStreak
                    }
                } else {
                    newStreak = 0
                }
            }
            BudgetChallenge.ChallengeType.UNDER_BUDGET -> {
                val target = challenge.targetAmount ?: return challenge
                if ((spentAmount ?: 0.0) <= target) {
                    newStreak++
                    newCompletedDays++
                    if (newStreak > newMaxStreak) {
                        newMaxStreak = newStreak
                    }
                } else {
                    newStreak = 0
                }
            }
            BudgetChallenge.ChallengeType.DAILY_LIMIT -> {
                val target = challenge.targetAmount ?: return challenge
                if ((spentAmount ?: 0.0) <= target) {
                    newStreak++
                    newCompletedDays++
                    if (newStreak > newMaxStreak) {
                        newMaxStreak = newStreak
                    }
                } else {
                    newStreak = 0
                }
            }
        }

        val updatedChallenge = challenge.copy(
            currentStreak = newStreak,
            maxStreak = newMaxStreak,
            completedDays = newCompletedDays
        )

        saveCurrentChallenge(updatedChallenge)
        _currentChallenge.value = updatedChallenge
        updateStreakData(newStreak, newMaxStreak)

        return updatedChallenge
    }

    fun completeChallenge() {
        val challenge = _currentChallenge.value ?: return
        val completedChallenge = challenge.copy(status = BudgetChallenge.ChallengeStatus.COMPLETED)
        addToHistory(completedChallenge)
        updateStreakData(completedChallenge.currentStreak, completedChallenge.maxStreak)
        _currentChallenge.value = null
        clearCurrentChallenge()
    }

    fun failChallenge() {
        val challenge = _currentChallenge.value ?: return
        val failedChallenge = challenge.copy(status = BudgetChallenge.ChallengeStatus.FAILED)
        addToHistory(failedChallenge)
        _currentChallenge.value = null
        clearCurrentChallenge()
    }

    fun cancelChallenge() {
        _currentChallenge.value = null
        clearCurrentChallenge()
    }

    private fun getCurrentChallenge(): BudgetChallenge? {
        val json = prefs.getString(KEY_CURRENT_CHALLENGE, null) ?: return null
        return try {
            parseChallengeFromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCurrentChallenge(challenge: BudgetChallenge) {
        prefs.edit().putString(KEY_CURRENT_CHALLENGE, challengeToJson(challenge)).apply()
    }

    private fun clearCurrentChallenge() {
        prefs.edit().remove(KEY_CURRENT_CHALLENGE).apply()
    }

    private fun getChallengeHistory(): List<BudgetChallenge> {
        val json = prefs.getString(KEY_CHALLENGE_HISTORY, null) ?: return emptyList()
        return try {
            parseChallengeListFromJson(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addToHistory(challenge: BudgetChallenge) {
        val history = _challengeHistory.value.toMutableList()
        history.add(0, challenge)
        if (history.size > 50) {
            history.removeAt(history.lastIndex)
        }
        _challengeHistory.value = history
        prefs.edit().putString(KEY_CHALLENGE_HISTORY, challengeListToJson(history)).apply()
    }

    private fun getStreakData(): StreakData {
        val lastCheckDate = prefs.getLong(KEY_LAST_CHECK_DATE, 0)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val maxStreak = prefs.getInt(KEY_MAX_STREAK, 0)
        return StreakData(lastCheckDate, currentStreak, maxStreak)
    }

    private fun updateStreakData(currentStreak: Int, maxStreak: Int) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        prefs.edit()
            .putLong(KEY_LAST_CHECK_DATE, today)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_MAX_STREAK, maxStreak)
            .apply()

        _streakData.value = StreakData(today, currentStreak, maxStreak)
    }

    private fun challengeToJson(challenge: BudgetChallenge): String {
        return "${challenge.id}|${challenge.title}|${challenge.category ?: ""}|${challenge.targetAmount ?: 0.0}|${challenge.challengeType.name}|${challenge.startDate}|${challenge.endDate}|${challenge.status.name}|${challenge.currentStreak}|${challenge.maxStreak}|${challenge.totalDays}|${challenge.completedDays}"
    }

    private fun parseChallengeFromJson(json: String): BudgetChallenge? {
        val parts = json.split("|")
        if (parts.size < 12) return null
        return BudgetChallenge(
            id = parts[0],
            title = parts[1],
            category = parts[2].ifEmpty { null },
            targetAmount = parts[3].toDoubleOrNull()?.takeIf { it > 0 },
            challengeType = BudgetChallenge.ChallengeType.valueOf(parts[4]),
            startDate = parts[5].toLongOrNull() ?: System.currentTimeMillis(),
            endDate = parts[6].toLongOrNull() ?: 0,
            status = BudgetChallenge.ChallengeStatus.valueOf(parts[7]),
            currentStreak = parts[8].toIntOrNull() ?: 0,
            maxStreak = parts[9].toIntOrNull() ?: 0,
            totalDays = parts[10].toIntOrNull() ?: 0,
            completedDays = parts[11].toIntOrNull() ?: 0
        )
    }

    private fun challengeListToJson(list: List<BudgetChallenge>): String {
        return list.joinToString(";;") { challengeToJson(it) }
    }

    private fun parseChallengeListFromJson(json: String): List<BudgetChallenge> {
        if (json.isEmpty()) return emptyList()
        return json.split(";;").mapNotNull { parseChallengeFromJson(it) }
    }

    data class StreakData(
        val lastCheckDate: Long,
        val currentStreak: Int,
        val maxStreak: Int
    )
}