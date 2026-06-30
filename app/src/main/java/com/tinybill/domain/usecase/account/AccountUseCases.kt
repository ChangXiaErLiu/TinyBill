package com.tinybill.domain.usecase.account

import com.tinybill.data.entity.AccountTransaction
import com.tinybill.data.entity.AccountTransactionType
import com.tinybill.data.entity.Transaction
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result
import com.tinybill.domain.repository.IAccountRepository

/**
 * 添加账户 UseCase。
 * 有业务规则：账户名非空校验。
 */
class AddAccountUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(account: com.tinybill.data.entity.Account): Result<Long> {
        if (account.name.isBlank()) {
            return Result.Error(AppException.ValidationException("name", "账户名称不能为空"))
        }
        
        return try {
            val id = repository.insert(account)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseException(e.message ?: "添加失败"))
        }
    }
}

/**
 * 记录交易到账户 UseCase。
 * 有业务规则：自动计算余额变动 + 创建 AccountTransaction 审计记录。
 */
class RecordTransactionToAccountUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(
        accountId: Long,
        transaction: Transaction
    ): Result<Unit> {
        val account = repository.getAccountById(accountId) 
            ?: return Result.Error(AppException.ValidationException("account", "账户不存在"))
        
        val newBalance = when (transaction.type) {
            Transaction.TYPE_EXPENSE -> account.currentBalance - transaction.amount
            Transaction.TYPE_INCOME -> account.currentBalance + transaction.amount
            else -> account.currentBalance
        }
        
        val accountTransaction = AccountTransaction(
            accountId = accountId,
            transactionId = transaction.id,
            type = if (transaction.type == Transaction.TYPE_EXPENSE) 
                AccountTransactionType.EXPENSE 
            else 
                AccountTransactionType.INCOME,
            amount = transaction.amount,
            balanceAfter = newBalance,
            timestamp = transaction.timestamp
        )
        
        return try {
            repository.updateBalance(accountId, newBalance)
            repository.insertAccountTransaction(accountTransaction)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseException(e.message ?: "记录失败"))
        }
    }
}

/**
 * 获取账户汇总 UseCase。
 * 有业务规则：聚合计算总资产 / 总负债 / 净资产。
 */
class GetAccountSummaryUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(): AccountSummary {
        val totalAssets = repository.getTotalAssets() ?: 0.0
        val totalLiabilities = repository.getTotalLiabilities() ?: 0.0
        
        return AccountSummary(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities
        )
    }
    
    data class AccountSummary(
        val totalAssets: Double,
        val totalLiabilities: Double,
        val netWorth: Double
    )
}
