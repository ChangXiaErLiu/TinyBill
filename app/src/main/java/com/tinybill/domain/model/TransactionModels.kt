package com.tinybill.domain.model

import com.tinybill.data.entity.Transaction

sealed class TransactionType {
    data object Expense : TransactionType()
    data object Income : TransactionType()
}

sealed class TransactionSource {
    data object Auto : TransactionSource()
    data object Manual : TransactionSource()
    data object Scheduled : TransactionSource()
}

data class TransactionValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class BudgetAlert(
    val type: AlertType,
    val message: String,
    val remainingAmount: Double? = null
) {
    enum class AlertType {
        OVER_BUDGET,
        NEAR_BUDGET,
        NORMAL
    }
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
}

sealed class AppException : Exception() {
    data class InvalidAmountException(override val message: String = "金额无效") : AppException()
    data class DuplicateTransactionException(override val message: String = "重复交易") : AppException()
    data class DatabaseException(override val message: String = "数据库错误") : AppException()
    data class ValidationException(val field: String, override val message: String) : AppException()
    data class NetworkException(override val message: String = "网络错误") : AppException()
}
