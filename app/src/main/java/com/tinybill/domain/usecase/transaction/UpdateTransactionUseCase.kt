package com.tinybill.domain.usecase.transaction

import com.tinybill.data.entity.Transaction
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result

/**
 * 更新账单的 UseCase。
 *
 * 保留理由：内含验证逻辑（金额>0、商户非空），与 AddTransactionUseCase 共享校验规则。
 * 如果删除，调用方各自实现验证 → 容易遗漏，违反 DRY。
 */
class UpdateTransactionUseCase(
    private val repository: com.tinybill.data.repository.TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amount <= 0) {
            return Result.Error(AppException.InvalidAmountException())
        }
        if (transaction.merchant.isBlank()) {
            return Result.Error(
                AppException.ValidationException("merchant", "商户名称不能为空")
            )
        }

        return try {
            repository.update(transaction)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseException(e.message ?: "更新失败"))
        }
    }
}
