package com.tinybill.domain.repository

import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountTransaction
import kotlinx.coroutines.flow.Flow

interface IAccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun getDefaultAccount(): Account?
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun delete(account: Account)
    suspend fun updateBalance(accountId: Long, newBalance: Double)
    suspend fun getTotalAssets(): Double?
    suspend fun getTotalLiabilities(): Double?
    suspend fun insertAccountTransaction(accountTransaction: AccountTransaction)
    fun getAccountTransactions(accountId: Long): Flow<List<AccountTransaction>>
    suspend fun getAccountTransactionByTransactionId(transactionId: Long): AccountTransaction?
    suspend fun deleteAccountTransactionByTransactionId(transactionId: Long)
}
