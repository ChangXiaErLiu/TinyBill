package com.tinybill.data.dao

import androidx.room.*
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?
    
    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?
    
    @Insert
    suspend fun insert(account: Account): Long
    
    @Update
    suspend fun update(account: Account)
    
    @Delete
    suspend fun delete(account: Account)
    
    @Query("UPDATE accounts SET currentBalance = :newBalance WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, newBalance: Double)
    
    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type != 'CREDIT_CARD'")
    suspend fun getTotalAssets(): Double?
    
    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type = 'CREDIT_CARD'")
    suspend fun getTotalLiabilities(): Double?
    
    // Account transactions
    @Insert
    suspend fun insertAccountTransaction(accountTransaction: AccountTransaction)
    
    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getAccountTransactions(accountId: Long): Flow<List<AccountTransaction>>
    
    @Query("SELECT * FROM account_transactions WHERE transactionId = :transactionId")
    suspend fun getAccountTransactionByTransactionId(transactionId: Long): AccountTransaction?
    
    @Query("DELETE FROM account_transactions WHERE transactionId = :transactionId")
    suspend fun deleteAccountTransactionByTransactionId(transactionId: Long)
}
