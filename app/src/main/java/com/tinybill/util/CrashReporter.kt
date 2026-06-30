package com.tinybill.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CrashReport(
    val id: String,
    val timestamp: Long,
    val exception: String,
    val message: String,
    val stackTrace: String,
    val deviceInfo: DeviceInfo,
    val appVersion: String,
    val isHandled: Boolean = false
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val sdkVersion: Int,
    val brand: String
)

data class CrashReportSummary(
    val totalCount: Int,
    val unhandledCount: Int,
    val latestCrash: CrashReport?
)

class CrashReporter private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val PREFS_NAME = "tinybill_crashes"
        private const val KEY_CRASH_REPORTS = "crash_reports"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val MAX_CACHED_CRASHES = 50
        private const val TAG = "CrashReporter"

        @Volatile
        private var INSTANCE: CrashReporter? = null

        fun getInstance(context: Context): CrashReporter {
            return INSTANCE ?: synchronized(this) {
                val instance = CrashReporter(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun logException(throwable: Throwable, tag: String = TAG) {
        Log.e(tag, "Captured exception", throwable)

        val crashReport = createCrashReport(throwable)
        saveCrashReport(crashReport)
    }

    fun logException(message: String, throwable: Throwable, tag: String = TAG) {
        Log.e(tag, message, throwable)

        val crashReport = createCrashReport(throwable, message)
        saveCrashReport(crashReport)
    }

    private fun createCrashReport(throwable: Throwable, customMessage: String? = null): CrashReport {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        return CrashReport(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            exception = throwable.javaClass.simpleName,
            message = customMessage ?: throwable.message ?: "No message",
            stackTrace = stackTrace,
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
        )
    }

    private fun saveCrashReport(report: CrashReport) {
        val reports = getCrashReports().toMutableList()
        reports.add(0, report)

        val trimmedReports = if (reports.size > MAX_CACHED_CRASHES) {
            reports.take(MAX_CACHED_CRASHES)
        } else {
            reports
        }

        val json = gson.toJson(trimmedReports)
        prefs.edit()
            .putString(KEY_CRASH_REPORTS, json)
            .putInt(KEY_CRASH_COUNT, prefs.getInt(KEY_CRASH_COUNT, 0) + 1)
            .apply()

        Log.d(TAG, "Crash report saved: ${report.id}")
    }

    fun getCrashReports(): List<CrashReport> {
        val json = prefs.getString(KEY_CRASH_REPORTS, "[]") ?: "[]"
        return try {
            gson.fromJson(json, Array<CrashReport>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUnprocessedCrashes(): List<CrashReport> {
        return getCrashReports().filter { !it.isHandled }
    }

    fun markAsProcessed(crashId: String) {
        val reports = getCrashReports().map { report ->
            if (report.id == crashId) {
                report.copy(isHandled = true)
            } else {
                report
            }
        }
        saveAllReports(reports)
    }

    fun markAllAsProcessed() {
        val reports = getCrashReports().map { it.copy(isHandled = true) }
        saveAllReports(reports)
    }

    private fun saveAllReports(reports: List<CrashReport>) {
        val json = gson.toJson(reports)
        prefs.edit().putString(KEY_CRASH_REPORTS, json).apply()
    }

    fun clearAllCrashes() {
        prefs.edit()
            .remove(KEY_CRASH_REPORTS)
            .remove(KEY_CRASH_COUNT)
            .apply()
    }

    fun deleteCrash(crashId: String) {
        val reports = getCrashReports().filter { it.id != crashId }
        saveAllReports(reports)
    }

    fun getSummary(): CrashReportSummary {
        val reports = getCrashReports()
        return CrashReportSummary(
            totalCount = reports.size,
            unhandledCount = reports.count { !it.isHandled },
            latestCrash = reports.firstOrNull()
        )
    }

    fun exportCrashReports(): String {
        val reports = getCrashReports()
        val sb = StringBuilder()
        sb.appendLine("=== TinyBill Crash Reports ===")
        sb.appendLine("Export Time: ${dateFormat.format(Date())}")
        sb.appendLine("Total Crashes: ${reports.size}")
        sb.appendLine("================================")
        sb.appendLine()

        reports.forEach { report ->
            sb.appendLine("--- Crash ${report.id} ---")
            sb.appendLine("Time: ${dateFormat.format(Date(report.timestamp))}")
            sb.appendLine("Exception: ${report.exception}")
            sb.appendLine("Message: ${report.message}")
            sb.appendLine("Device: ${report.deviceInfo.manufacturer} ${report.deviceInfo.model}")
            sb.appendLine("App Version: ${report.appVersion}")
            sb.appendLine("Stack Trace:")
            sb.appendLine(report.stackTrace)
            sb.appendLine()
        }

        return sb.toString()
    }

    fun exportToFile(): File? {
        return try {
            val content = exportCrashReports()
            val fileName = "tinybill_crashes_${System.currentTimeMillis()}.txt"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(content)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export crash reports", e)
            null
        }
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            sdkVersion = Build.VERSION.SDK_INT,
            brand = Build.BRAND
        )
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getCrashReportAsJson(crashId: String): String? {
        val report = getCrashReports().find { it.id == crashId }
        return report?.let { gson.toJson(it) }
    }

    fun importCrashReport(json: String): Boolean {
        return try {
            val report = gson.fromJson(json, CrashReport::class.java)
            val reports = getCrashReports().toMutableList()
            reports.add(0, report.copy(id = UUID.randomUUID().toString()))
            saveAllReports(reports)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import crash report", e)
            false
        }
    }
}

object GlobalExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashReporter: CrashReporter? = null

    fun install(context: Context) {
        crashReporter = CrashReporter.getInstance(context)
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashReporter?.logException(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun uninstall() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
    }
}
