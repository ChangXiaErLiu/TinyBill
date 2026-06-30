package com.tinybill.util

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {
    
    private const val BACKUP_DIR = "backups"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    suspend fun createBackup(
        context: Context,
        transactions: List<Transaction>,
        budgets: List<com.tinybill.data.entity.Budget>,
        templates: List<com.tinybill.data.entity.Template>,
        customCategories: List<com.tinybill.data.entity.CustomCategory>
    ): BackupResult = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val fileName = "tinybill_backup_${fileNameFormat.format(Date(timestamp))}.json"
        val backupFile = File(backupDir, fileName)
        
        val backupData = BackupData(
            version = 1,
            timestamp = timestamp,
            transactions = transactions,
            budgets = budgets,
            templates = templates,
            customCategories = customCategories
        )
        
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()
        
        // 序列化数据
        val json = gson.toJson(backupData)
        
        // 写入数据
        val outputStream = backupFile.outputStream()
        outputStream.use {
            it.write(json.toByteArray())
        }
        
        BackupResult(
            success = true,
            filePath = backupFile.absolutePath,
            fileSize = backupFile.length(),
            timestamp = timestamp
        )
    }
    
    suspend fun restoreBackup(
        context: Context,
        filePath: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext RestoreResult(success = false, errorMessage = "备份文件不存在")
            }
            
            // 读取数据
            val inputStream = file.inputStream()
            var backupData: BackupData? = null
            
            inputStream.use {
                // 读取数据
                val data = it.readBytes()
                
                // 解析数据
                val json = String(data)
                val gson = GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .create()
                
                backupData = gson.fromJson(json, BackupData::class.java)
            }
            
            if (backupData == null) {
                return@withContext RestoreResult(success = false, errorMessage = "备份文件解析失败")
            }
            
            val result = RestoreResult(
                success = true,
                transactionCount = backupData?.transactions?.size ?: 0,
                budgetCount = backupData?.budgets?.size ?: 0,
                templateCount = backupData?.templates?.size ?: 0,
                categoryCount = backupData?.customCategories?.size ?: 0,
                timestamp = backupData?.timestamp ?: 0
            )
            result
        } catch (e: Exception) {
            RestoreResult(success = false, errorMessage = e.message ?: "恢复失败")
        }
    }
    
    suspend fun getBackupList(context: Context): List<BackupInfo> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            return@withContext emptyList()
        }
        
        backupDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                BackupInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSize = file.length(),
                    lastModified = file.lastModified()
                )
            } ?: emptyList()
    }
    
    suspend fun deleteBackup(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun autoBackup(
        context: Context,
        transactions: List<Transaction>,
        budgets: List<com.tinybill.data.entity.Budget>,
        templates: List<com.tinybill.data.entity.Template>,
        customCategories: List<com.tinybill.data.entity.CustomCategory>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val lastBackupTime = prefs.getLong("last_auto_backup", 0)
            val currentTime = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            
            if (currentTime - lastBackupTime < oneDayMs) {
                return@withContext true
            }
            
            val result = createBackup(context, transactions, budgets, templates, customCategories)
            
            if (result.success) {
                prefs.edit().putLong("last_auto_backup", currentTime).apply()
                
                val backupDir = File(context.filesDir, BACKUP_DIR)
                val backupFiles = backupDir.listFiles()
                    ?.filter { it.extension == "json" }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
                
                if (backupFiles.size > 7) {
                    backupFiles.drop(7).forEach { it.delete() }
                }
            }
            
            result.success
        } catch (e: Exception) {
            false
        }
    }
}

data class BackupData(
    val version: Int,
    val timestamp: Long,
    val transactions: List<Transaction>,
    val budgets: List<com.tinybill.data.entity.Budget>,
    val templates: List<com.tinybill.data.entity.Template>,
    val customCategories: List<com.tinybill.data.entity.CustomCategory>
)

data class BackupResult(
    val success: Boolean,
    val filePath: String = "",
    val fileSize: Long = 0,
    val timestamp: Long = 0
)

data class RestoreResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val templateCount: Int = 0,
    val categoryCount: Int = 0,
    val timestamp: Long = 0
)

data class BackupInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long
) {
    fun formatFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }
    }
}