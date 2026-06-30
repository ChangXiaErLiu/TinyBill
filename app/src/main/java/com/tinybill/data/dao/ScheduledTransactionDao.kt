package com.tinybill.data.dao

import androidx.room.*
import com.tinybill.data.entity.ScheduledTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTransactionDao {
    @Query("SELECT * FROM scheduled_transaction WHERE enabled = 1 ORDER BY dayOfMonth ASC")
    fun getAllEnabledScheduledTransactions(): Flow<List<ScheduledTransaction>>
    
    @Query("SELECT * FROM scheduled_transaction ORDER BY dayOfMonth ASC")
    fun getAllScheduledTransactions(): Flow<List<ScheduledTransaction>>
    
    @Query("SELECT * FROM scheduled_transaction ORDER BY dayOfMonth ASC")
    suspend fun getAllScheduledTransactionsOnce(): List<ScheduledTransaction>
    
    @Query("SELECT * FROM scheduled_transaction WHERE id = :id")
    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction?
    
    @Query("SELECT * FROM scheduled_transaction WHERE dayOfMonth = :dayOfMonth AND enabled = 1")
    suspend fun getScheduledTransactionsForDay(dayOfMonth: Int): List<ScheduledTransaction>
    
    @Insert
    suspend fun insert(scheduledTransaction: ScheduledTransaction): Long
    
    @Update
    suspend fun update(scheduledTransaction: ScheduledTransaction)
    
    @Delete
    suspend fun delete(scheduledTransaction: ScheduledTransaction)
    
    @Query("DELETE FROM scheduled_transaction WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE scheduled_transaction SET lastExecuted = :timestamp WHERE id = :id")
    suspend fun updateLastExecuted(id: Long, timestamp: Long)
    
    @Query("UPDATE scheduled_transaction SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}