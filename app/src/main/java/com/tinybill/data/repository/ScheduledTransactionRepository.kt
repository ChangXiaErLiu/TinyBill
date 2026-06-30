package com.tinybill.data.repository

import com.tinybill.data.dao.ScheduledTransactionDao
import com.tinybill.data.entity.ScheduledTransaction
import kotlinx.coroutines.flow.Flow

class ScheduledTransactionRepository(private val scheduledTransactionDao: ScheduledTransactionDao) {
    
    val allScheduledTransactions: Flow<List<ScheduledTransaction>> = scheduledTransactionDao.getAllScheduledTransactions()
    
    val enabledScheduledTransactions: Flow<List<ScheduledTransaction>> = scheduledTransactionDao.getAllEnabledScheduledTransactions()
    
    suspend fun insert(scheduledTransaction: ScheduledTransaction): Long {
        return scheduledTransactionDao.insert(scheduledTransaction)
    }
    
    suspend fun update(scheduledTransaction: ScheduledTransaction) {
        scheduledTransactionDao.update(scheduledTransaction)
    }
    
    suspend fun delete(scheduledTransaction: ScheduledTransaction) {
        scheduledTransactionDao.delete(scheduledTransaction)
    }
    
    suspend fun deleteById(id: Long) {
        scheduledTransactionDao.deleteById(id)
    }
    
    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction? {
        return scheduledTransactionDao.getScheduledTransactionById(id)
    }
    
    suspend fun getScheduledTransactionsForDay(dayOfMonth: Int): List<ScheduledTransaction> {
        return scheduledTransactionDao.getScheduledTransactionsForDay(dayOfMonth)
    }
    
    suspend fun updateLastExecuted(id: Long, timestamp: Long) {
        scheduledTransactionDao.updateLastExecuted(id, timestamp)
    }
    
    suspend fun setEnabled(id: Long, enabled: Boolean) {
        scheduledTransactionDao.setEnabled(id, enabled)
    }
    
    suspend fun getAllEnabledOnce(): List<ScheduledTransaction> {
        return scheduledTransactionDao.getAllScheduledTransactionsOnce().filter { it.enabled == 1 }
    }
}