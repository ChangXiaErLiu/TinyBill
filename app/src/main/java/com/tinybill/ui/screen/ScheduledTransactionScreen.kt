package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.ScheduledTransaction
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.ScheduledTransactionRepository
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.SuccessColor
import com.tinybill.util.FormatUtils
import com.tinybill.util.HapticManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTransactionScreen(
    onBack: () -> Unit,
    repository: ScheduledTransactionRepository = koinInject()
) {
    val scheduled by repository.allScheduledTransactions.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScheduledTransaction?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定期账单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加定期账单")
            }
        }
    ) { paddingValues ->
        if (scheduled.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无定期账单",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角按钮添加，每月自动记账",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(scheduled, key = { it.id }) { item ->
                    ScheduledTransactionCard(
                        scheduled = item,
                        onToggleEnabled = {
                            scope.launch {
                                repository.setEnabled(item.id, item.enabled == 0)
                            }
                        },
                        onEdit = {
                            editingItem = item
                            showAddDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteById(item.id)
                                HapticManager.performDelete()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduledTransactionEditDialog(
            existing = editingItem,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            onConfirm = { scheduledTransaction ->
                scope.launch {
                    HapticManager.performSuccess()
                    if (editingItem != null) {
                        repository.update(scheduledTransaction)
                    } else {
                        repository.insert(scheduledTransaction)
                    }
                    showAddDialog = false
                    editingItem = null
                }
            }
        )
    }
}

@Composable
private fun ScheduledTransactionCard(
    scheduled: ScheduledTransaction,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scheduled.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scheduled.enabled == 1) "" else "（已暂停）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "每月${scheduled.dayOfMonth}日 · ${Transaction.getCategoryName(scheduled.category)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (scheduled.lastExecuted > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "上次执行：${FormatUtils.formatYmd(scheduled.lastExecuted)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = FormatUtils.formatAmount(scheduled.amount, showSign = true, isExpense = scheduled.type == Transaction.TYPE_EXPENSE),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (scheduled.type == Transaction.TYPE_EXPENSE) ErrorColor else SuccessColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onToggleEnabled) {
                Icon(
                    if (scheduled.enabled == 1) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (scheduled.enabled == 1) "暂停" else "启用",
                    tint = if (scheduled.enabled == 1) MaterialTheme.colorScheme.onSurfaceVariant else PrimaryGreen
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = ErrorColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledTransactionEditDialog(
    existing: ScheduledTransaction?,
    onDismiss: () -> Unit,
    onConfirm: (ScheduledTransaction) -> Unit
) {
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(existing?.merchant ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: Transaction.CATEGORY_FOOD) }
    var isExpense by remember { mutableStateOf(existing?.type != Transaction.TYPE_INCOME) }
    var dayOfMonth by remember { mutableStateOf(existing?.dayOfMonth?.toString() ?: "1") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val day = dayOfMonth.toIntOrNull() ?: 1
    val isValid = amount > 0 && merchant.isNotBlank() && day in 1..28

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "编辑定期账单" else "添加定期账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 收支切换
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ErrorColor.copy(alpha = 0.15f),
                            selectedLabelColor = ErrorColor
                        )
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessColor.copy(alpha = 0.15f),
                            selectedLabelColor = SuccessColor
                        )
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥ ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商户名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 分类选择
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        val categories = if (isExpense) Transaction.EXPENSE_CATEGORIES else Transaction.INCOME_CATEGORIES
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,2}$"))) dayOfMonth = it },
                    label = { Text("每月几号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = dayOfMonth.isNotBlank() && (day < 1 || day > 28),
                    supportingText = if (dayOfMonth.isNotBlank() && (day < 1 || day > 28)) {
                        { Text("请输入 1-28 之间的日期", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ScheduledTransaction(
                            id = existing?.id ?: 0,
                            amount = amount,
                            merchant = merchant,
                            category = category,
                            type = if (isExpense) Transaction.TYPE_EXPENSE else Transaction.TYPE_INCOME,
                            dayOfMonth = day,
                            enabled = existing?.enabled ?: 1
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 从 Transaction 伴生对象中获取分类的中文名，否则原样返回 */
private fun Transaction.Companion.getCategoryName(category: String): String = category
