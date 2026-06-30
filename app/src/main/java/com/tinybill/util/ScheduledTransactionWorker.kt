package com.tinybill.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScheduledTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val manager = ScheduledTransactionManager.getInstance(applicationContext)
            manager.executeScheduledTransactions()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "scheduled_transaction_work"
    }
}