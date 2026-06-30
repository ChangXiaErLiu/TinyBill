package com.tinybill.util

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.tinybill.data.entity.ExportFormat
import com.tinybill.data.entity.ExportOptions
import com.tinybill.data.entity.ExportResult
import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    suspend fun exportTransactions(
        context: Context,
        transactions: List<Transaction>,
        options: ExportOptions
    ): ExportResult = withContext(Dispatchers.IO) {
        val filtered = transactions.filter { transaction ->
            val inTimeRange = if (options.startTime != null && options.endTime != null) {
                transaction.timestamp in options.startTime..options.endTime
            } else true
            
            val inCategories = options.categories?.contains(transaction.category) ?: true
            val inTypes = options.types?.contains(transaction.type) ?: true
            
            inTimeRange && inCategories && inTypes
        }
        
        val timestamp = System.currentTimeMillis()
        val fileName = "tinybill_export_${fileNameDateFormat.format(Date(timestamp))}"
        
        val result = when (options.format) {
            ExportFormat.CSV -> exportToCsv(context, filtered, fileName, options, timestamp)
            ExportFormat.JSON -> exportToJson(context, filtered, fileName, options, timestamp)
        }
        
        result
    }
    
    private suspend fun exportToCsv(
        context: Context,
        transactions: List<Transaction>,
        fileName: String,
        options: ExportOptions,
        timestamp: Long
    ): ExportResult = withContext(Dispatchers.IO) {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(documentsDir, "$fileName.csv")
        
        FileWriter(file).use { writer ->
            writer.append("日期,时间,类型,商户,分类,金额,备注,来源\n")
            
            transactions.forEach { transaction ->
                val date = Date(transaction.timestamp)
                val typeStr = if (transaction.type == Transaction.TYPE_EXPENSE) "支出" else "收入"
                val sourceStr = when (transaction.source) {
                    Transaction.SOURCE_AUTO -> "自动记账"
                    Transaction.SOURCE_MANUAL -> "手动添加"
                    else -> "未知"
                }
                
                val line = buildString {
                    append(dateFormat.format(date).replace(",", ";"))
                    append(",")
                    append(typeStr)
                    append(",")
                    append("\"${transaction.merchant.replace("\"", "\"\"")}\"")
                    append(",")
                    append("\"${transaction.category.replace("\"", "\"\"")}\"")
                    append(",")
                    append(String.format("%.2f", transaction.amount))
                    append(",")
                    append("\"${(transaction.note ?: "").replace("\"", "\"\"")}\"")
                    append(",")
                    append(sourceStr)
                    append("\n")
                }
                writer.append(line)
            }
        }
        
        ExportResult(
            filePath = file.absolutePath,
            recordCount = transactions.size,
            startTime = options.startTime ?: transactions.minOfOrNull { it.timestamp } ?: 0,
            endTime = options.endTime ?: timestamp
        )
    }
    
    private suspend fun exportToJson(
        context: Context,
        transactions: List<Transaction>,
        fileName: String,
        options: ExportOptions,
        timestamp: Long
    ): ExportResult = withContext(Dispatchers.IO) {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(documentsDir, "$fileName.json")
        
        val exportData = ExportData(
            exportTime = timestamp,
            recordCount = transactions.size,
            transactions = transactions.map { it.toExportRecord() }
        )
        
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()
        
        File(file.absolutePath).writeText(gson.toJson(exportData))
        
        ExportResult(
            filePath = file.absolutePath,
            recordCount = transactions.size,
            startTime = options.startTime ?: transactions.minOfOrNull { it.timestamp } ?: 0,
            endTime = options.endTime ?: timestamp
        )
    }
    
    private fun Transaction.toExportRecord() = ExportRecord(
        id = this.id,
        timestamp = this.timestamp,
        dateStr = dateFormat.format(Date(this.timestamp)),
        type = if (this.type == Transaction.TYPE_EXPENSE) "expense" else "income",
        merchant = this.merchant,
        category = this.category,
        amount = this.amount,
        note = this.note,
        source = when (this.source) {
            Transaction.SOURCE_AUTO -> "auto"
            Transaction.SOURCE_MANUAL -> "manual"
            else -> "unknown"
        }
    )
}

private data class ExportData(
    val exportTime: Long,
    val recordCount: Int,
    val transactions: List<ExportRecord>
)

private data class ExportRecord(
    val id: Long,
    val timestamp: Long,
    val dateStr: String,
    val type: String,
    val merchant: String,
    val category: String,
    val amount: Double,
    val note: String?,
    val source: String
)