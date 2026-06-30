package com.tinybill.domain.usecase.transaction

import com.tinybill.data.entity.Transaction
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result

/**
 * 添加账单的 UseCase。
 *
 * 保留理由：内含 3 段业务规则（金额>0 + 商户非空 + 分类非空 → 验证 → 去重检查 → 保存），
 * 是当前唯一有"业务价值"的 UseCase。
 */
class AddTransactionUseCase(
    private val repository: com.tinybill.data.repository.TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        val validation = validateTransaction(transaction)
        if (!validation.isValid) {
            return Result.Error(
                AppException.ValidationException("amount", validation.errorMessage ?: "验证失败")
            )
        }

        if (repository.isDuplicate(transaction.merchant, transaction.amount, transaction.timestamp)) {
            return Result.Error(AppException.DuplicateTransactionException())
        }

        return try {
            val id = repository.insert(transaction)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseException(e.message ?: "保存失败"))
        }
    }

    private fun validateTransaction(transaction: Transaction): ValidationResult {
        return when {
            transaction.amount <= 0 -> ValidationResult(false, "金额必须大于0")
            transaction.merchant.isBlank() -> ValidationResult(false, "商户名称不能为空")
            transaction.category.isBlank() -> ValidationResult(false, "分类不能为空")
            else -> ValidationResult(true)
        }
    }

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)
}
