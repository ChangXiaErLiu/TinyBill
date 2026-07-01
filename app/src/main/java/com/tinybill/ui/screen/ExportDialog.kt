package com.tinybill.ui.screen

import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tinybill.data.entity.ExportFormat
import com.tinybill.data.entity.ExportOptions
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.util.FormatUtils
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit,
    transactionCount: Int = 0
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var customDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf<String?>(null) }

    // 类型筛选
    var exportExpense by remember { mutableStateOf(true) }
    var exportIncome by remember { mutableStateOf(true) }
    
    // 分类筛选
    var selectedCategories by remember { mutableStateOf(emptySet<String>()) }
    var useCategoryFilter by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("导出账单", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 格式选择
                Text("导出格式", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == ExportFormat.CSV,
                        onClick = { selectedFormat = ExportFormat.CSV },
                        label = { Text("CSV（推荐）") },
                        leadingIcon = if (selectedFormat == ExportFormat.CSV) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = selectedFormat == ExportFormat.JSON,
                        onClick = { selectedFormat = ExportFormat.JSON },
                        label = { Text("JSON") },
                        leadingIcon = if (selectedFormat == ExportFormat.JSON) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }

                // 类型筛选
                Text("交易类型", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportExpense,
                        onClick = { exportExpense = !exportExpense },
                        label = { Text("支出") }
                    )
                    FilterChip(
                        selected = exportIncome,
                        onClick = { exportIncome = !exportIncome },
                        label = { Text("收入") }
                    )
                }

                // 日期范围
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自定义日期范围", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = customDateRange,
                        onCheckedChange = { customDateRange = it }
                    )
                }

                if (customDateRange) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate?.let { FormatUtils.formatYmd(it) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("开始日期") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showStartDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                                }
                            }
                        )
                        OutlinedTextField(
                            value = endDate?.let { FormatUtils.formatYmd(it) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("结束日期") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showEndDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                                }
                            }
                        )
                    }
                }

                // 汇总信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("导出说明", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("• 共 ${transactionCount} 条记录")
                                if (customDateRange && startDate != null && endDate != null) {
                                    append("（${FormatUtils.formatYmd(startDate!!)} ~ ${FormatUtils.formatYmd(endDate!!)}）")
                                }
                        append("\n• CSV 格式可用 Excel、WPS 直接打开")
                        append("\n• 导出后点击「分享」发送到电脑或云盘")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isExporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在导出...")
                    }
                }

                exportSuccess?.let { filePath ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "导出成功",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryGreen
                                )
                            }
                            TextButton(onClick = {
                                val file = File(filePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (filePath.endsWith(".csv")) "text/csv" else "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享导出文件"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("分享")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportSuccess == null) {
                Button(
                    onClick = {
                        isExporting = true
                        val types = buildList {
                            if (exportExpense) add(Transaction.TYPE_EXPENSE)
                            if (exportIncome) add(Transaction.TYPE_INCOME)
                        }
                        onExport(ExportOptions(
                            startTime = if (customDateRange) startDate else null,
                            endTime = if (customDateRange) endDate else null,
                            types = types,
                            format = selectedFormat
                        ))
                        isExporting = false
                    },
                    enabled = !isExporting && (!customDateRange || (startDate != null && endDate != null))
                            && (exportExpense || exportIncome),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
            } else {
                TextButton(onClick = {
                    exportSuccess = null
                    onDismiss()
                }) {
                    Text("关闭")
                }
            }
        },
        dismissButton = {
            if (exportSuccess == null) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showStartDatePicker) {
        TinyBillDatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                startDate = date
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        TinyBillDatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onConfirm = { date ->
                endDate = date
                showEndDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinyBillDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onConfirm(it) }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}