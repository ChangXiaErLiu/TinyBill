package com.tinybill

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tinybill.di.appModules
import com.tinybill.util.GlobalExceptionHandler
import com.tinybill.util.ScheduledTransactionWorker
import com.tinybill.util.SettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TinyBillApp : Application() {

    companion object {
        lateinit var instance: TinyBillApp
            private set

        lateinit var settingsManager: SettingsManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        GlobalExceptionHandler.install(this)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@TinyBillApp)
            modules(appModules)
        }

        settingsManager = SettingsManager.getInstance(this)
        scheduleScheduledTransactionWork()
    }

    private fun scheduleScheduledTransactionWork() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val millisUntilMidnight = getMillisUntilMidnight()

        val initialDelay = if (millisUntilMidnight > 0) millisUntilMidnight else TimeUnit.HOURS.toMillis(24)

        val dailyWorkRequest = PeriodicWorkRequestBuilder<ScheduledTransactionWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ScheduledTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    private fun getMillisUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return midnight.timeInMillis - now.timeInMillis
    }
}