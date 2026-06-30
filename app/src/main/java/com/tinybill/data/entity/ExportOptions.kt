package com.tinybill.data.entity

data class ExportOptions(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val categories: List<String>? = null,
    val types: List<Int>? = null,
    val format: ExportFormat = ExportFormat.CSV
)

enum class ExportFormat {
    CSV,
    JSON
}

data class ExportResult(
    val filePath: String,
    val recordCount: Int,
    val startTime: Long,
    val endTime: Long
)