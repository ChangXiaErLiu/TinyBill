package com.tinybill.presentation.state

import com.tinybill.data.entity.Transaction
import java.util.Calendar

sealed interface DialogState {
    data object Hidden : DialogState

    data class AddTransaction(
        val editTransaction: Transaction? = null,
        val presetYear: Int? = null,
        val presetMonth: Int? = null,
        val presetDay: Int? = null
    ) : DialogState {
        val hasPresetDate: Boolean get() = presetYear != null && presetMonth != null && presetDay != null

        fun getPresetTimestamp(): Long? {
            if (!hasPresetDate) return null
            val calendar = Calendar.getInstance().apply {
                set(presetYear!!, presetMonth!! - 1, presetDay!!, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
    }

    data object BudgetSettings : DialogState

    data class Search(val keyword: String = "") : DialogState

    data class Export(val transactions: List<Transaction> = emptyList()) : DialogState

    data class Backup(val transactions: List<Transaction> = emptyList()) : DialogState

    data class AppLock(val isSetup: Boolean = true) : DialogState

    data object AACalculator : DialogState

    data class ExpenseList(val transactions: List<Transaction>) : DialogState

    data class IncomeList(val transactions: List<Transaction>) : DialogState

    data class IncomeConfirm(val transaction: Transaction) : DialogState

    data object BudgetChallenge : DialogState

    data object Settings : DialogState
}
