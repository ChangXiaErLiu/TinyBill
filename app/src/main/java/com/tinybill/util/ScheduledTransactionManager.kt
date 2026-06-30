package com.tinybill.util

import android.content.Context
import com.tinybill.TinyBillApp
import com.tinybill.data.database.AppDatabase
import com.tinybill.data.entity.ScheduledTransaction
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.ScheduledTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduledTransactionManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ScheduledTransactionManager? = null
        
        fun getInstance(context: Context): ScheduledTransactionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduledTransactionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val database = AppDatabase.getDatabase(context)
    private val repository = ScheduledTransactionRepository(database.scheduledTransactionDao())
    private val transactionRepository = com.tinybill.data.repository.TransactionRepository(database.transactionDao())
    
    suspend fun executeScheduledTransactions() = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1
        val startOfDay = today.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val scheduledList = repository.getScheduledTransactionsForDay(dayOfMonth)
        
        for (scheduled in scheduledList) {
            if (scheduled.enabled != 1) continue
            
            val lastExecutedMonth = getMonthFromTimestamp(scheduled.lastExecuted)
            val currentMonthTimestamp = currentYear * 100 + currentMonth
            
            if (lastExecutedMonth == currentMonthTimestamp) continue
            
            val isDuplicate = transactionRepository.isDuplicate(
                scheduled.merchant,
                scheduled.amount,
                System.currentTimeMillis()
            )
            
            if (isDuplicate) {
                repository.updateLastExecuted(scheduled.id, System.currentTimeMillis())
                continue
            }
            
            val transaction = Transaction(
                amount = scheduled.amount,
                merchant = scheduled.merchant,
                category = scheduled.category,
                timestamp = System.currentTimeMillis(),
                type = scheduled.type,
                source = Transaction.SOURCE_SCHEDULED,
                note = "定期记账"
            )
            
            transactionRepository.insert(transaction)
            repository.updateLastExecuted(scheduled.id, System.currentTimeMillis())
        }
    }
    
    suspend fun checkAndExecuteForDay(dayOfMonth: Int) = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1
        
        val scheduledList = repository.getScheduledTransactionsForDay(dayOfMonth)
        
        for (scheduled in scheduledList) {
            if (scheduled.enabled != 1) continue
            
            val lastExecutedMonth = getMonthFromTimestamp(scheduled.lastExecuted)
            val currentMonthTimestamp = currentYear * 100 + currentMonth
            
            if (lastExecutedMonth == currentMonthTimestamp) continue
            
            val transaction = Transaction(
                amount = scheduled.amount,
                merchant = scheduled.merchant,
                category = scheduled.category,
                timestamp = System.currentTimeMillis(),
                type = scheduled.type,
                source = Transaction.SOURCE_SCHEDULED,
                note = "定期记账"
            )
            
            transactionRepository.insert(transaction)
            repository.updateLastExecuted(scheduled.id, System.currentTimeMillis())
        }
    }
    
    private fun getMonthFromTimestamp(timestamp: Long): Int {
        if (timestamp == 0L) return 0
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
    }
}