package com.tinybill.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double,
    val currentBalance: Double,
    val icon: String = "account_balance_wallet",
    val color: Int = 0xFF10B981.toInt(),
    val isDefault: Boolean = false,
    val note: String = ""
) {
    companion object {
        const val ICON_CASH = "payments"
        const val ICON_BANK_CARD = "credit_card"
        const val ICON_CREDIT_CARD = "credit_score"
        const val ICON_ALIPAY = "account_balance"
        const val ICON_WECHAT = "chat"
        const val ICON_INVESTMENT = "trending_up"
        
        fun getDefaultAccounts(): List<Account> = listOf(
            Account(
                name = "现金",
                type = AccountType.CASH,
                initialBalance = 0.0,
                currentBalance = 0.0,
                icon = ICON_CASH,
                color = 0xFF10B981.toInt(),
                isDefault = true
            ),
            Account(
                name = "支付宝",
                type = AccountType.E_WALLET,
                initialBalance = 0.0,
                currentBalance = 0.0,
                icon = ICON_ALIPAY,
                color = 0xFF1677FF.toInt(),
                isDefault = true
            ),
            Account(
                name = "微信支付",
                type = AccountType.E_WALLET,
                initialBalance = 0.0,
                currentBalance = 0.0,
                icon = ICON_WECHAT,
                color = 0xFF07C160.toInt(),
                isDefault = true
            )
        )
    }
}

enum class AccountType {
    CASH,           // 现金
    BANK_CARD,      // 借记卡
    CREDIT_CARD,    // 信用卡
    E_WALLET,       // 电子钱包
    INVESTMENT,     // 投资账户
    PREPAID_CARD,   // 储值卡
    OTHER           // 其他
}

@Entity(tableName = "account_transactions")
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val transactionId: Long,
    val type: AccountTransactionType,
    val amount: Double,
    val balanceAfter: Double,
    val timestamp: Long
)

enum class AccountTransactionType {
    INCOME,     // 收入
    EXPENSE,    // 支出
    TRANSFER_IN,    // 转入
    TRANSFER_OUT,   // 转出
    ADJUSTMENT      // 余额调整
}
