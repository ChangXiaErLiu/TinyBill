package com.tinybill.data.repository

import com.tinybill.data.dao.AccountDao
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountTransaction
import com.tinybill.domain.repository.IAccountRepository
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : IAccountRepository {
    
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts()
    }
    
    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)
    }
    
    override suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()
    }
    
    override suspend fun insert(account: Account): Long {
        return accountDao.insert(account)
    }
    
    override suspend fun update(account: Account) {
        accountDao.update(account)
    }
    
    override suspend fun delete(account: Account) {
        accountDao.delete(account)
    }
    
    override suspend fun updateBalance(accountId: Long, newBalance: Double) {
        accountDao.updateBalance(accountId, newBalance)
    }
    
    override suspend fun getTotalAssets(): Double? {
        return accountDao.getTotalAssets()
    }
    
    override suspend fun getTotalLiabilities(): Double? {
        return accountDao.getTotalLiabilities()
    }
    
    override suspend fun insertAccountTransaction(accountTransaction: AccountTransaction) {
        accountDao.insertAccountTransaction(accountTransaction)
    }
    
    override fun getAccountTransactions(accountId: Long): Flow<List<AccountTransaction>> {
        return accountDao.getAccountTransactions(accountId)
    }
    
    override suspend fun getAccountTransactionByTransactionId(transactionId: Long): AccountTransaction? {
        return accountDao.getAccountTransactionByTransactionId(transactionId)
    }
    
    override suspend fun deleteAccountTransactionByTransactionId(transactionId: Long) {
        accountDao.deleteAccountTransactionByTransactionId(transactionId)
    }
}
