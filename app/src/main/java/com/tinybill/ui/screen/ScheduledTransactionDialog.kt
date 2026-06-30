package com.tinybill.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.ScheduledTransaction
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTransactionDialog(
    scheduledTransactions: List<ScheduledTransaction>,
    onDismiss: () -> Unit,
    onAdd: (ScheduledTransaction) -> Unit,
    onUpdate: (ScheduledTransaction) -> Unit,
    onDelete: (ScheduledTransaction) -> Unit,
    onToggleEnabled: (ScheduledTransaction, Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<ScheduledTransaction?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("定期记账", fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
            }
        },
        text = {
            if (scheduledTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无定期记账",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "点击上方添加按钮创建",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scheduledTransactions, key = { it.id }) { scheduled ->
                        ScheduledTransactionItem(
                            scheduled = scheduled,
                            onEdit = { editingTransaction = scheduled },
                            onDelete = { onDelete(scheduled) },
                            onToggleEnabled = { enabled -> onToggleEnabled(scheduled, enabled) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showAddDialog) {
        AddScheduledTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { scheduled ->
                onAdd(scheduled)
                showAddDialog = false
            }
        )
    }

    editingTransaction?.let { scheduled ->
        AddScheduledTransactionDialog(
            onDismiss = { editingTransaction = null },
            onConfirm = { updated ->
                onUpdate(updated)
                editingTransaction = null
            },
            editScheduledTransaction = scheduled
        )
    }
}

@Composable
private fun ScheduledTransactionItem(
    scheduled: ScheduledTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (scheduled.enabled == 1)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheduled.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¥${String.format("%.2f", scheduled.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "每月${scheduled.dayOfMonth}日",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = scheduled.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = scheduled.enabled == 1,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = PrimaryGreen
                )
            )

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条定期记账吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (ScheduledTransaction) -> Unit,
    editScheduledTransaction: ScheduledTransaction? = null
) {
    var amount by remember { mutableStateOf(editScheduledTransaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(editScheduledTransaction?.merchant ?: "") }
    var selectedCategory by remember { mutableStateOf(editScheduledTransaction?.category ?: Transaction.CATEGORY_OTHER) }
    var selectedType by remember { mutableIntStateOf(editScheduledTransaction?.type ?: Transaction.TYPE_EXPENSE) }
    var dayOfMonth by remember { mutableStateOf(editScheduledTransaction?.dayOfMonth?.toString() ?: "1") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = if (selectedType == Transaction.TYPE_EXPENSE) Transaction.EXPENSE_CATEGORIES else Transaction.INCOME_CATEGORIES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editScheduledTransaction == null) "添加定期记账" else "编辑定期记账",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == Transaction.TYPE_EXPENSE,
                        onClick = {
                            selectedType = Transaction.TYPE_EXPENSE
                            selectedCategory = Transaction.EXPENSE_CATEGORIES.first()
                        },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        selected = selectedType == Transaction.TYPE_INCOME,
                        onClick = {
                            selectedType = Transaction.TYPE_INCOME
                            selectedCategory = Transaction.INCOME_CATEGORIES.first()
                        },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商户名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { dayOfMonth = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("每月几号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("日") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    val day = dayOfMonth.toIntOrNull() ?: return@Button
                    if (amountValue <= 0 || day < 1 || day > 31 || merchant.isBlank()) return@Button

                    val scheduled = ScheduledTransaction(
                        id = editScheduledTransaction?.id ?: 0,
                        amount = amountValue,
                        merchant = merchant,
                        category = selectedCategory,
                        type = selectedType,
                        dayOfMonth = day,
                        enabled = editScheduledTransaction?.enabled ?: 1,
                        lastExecuted = editScheduledTransaction?.lastExecuted ?: 0
                    )
                    onConfirm(scheduled)
                },
                enabled = amount.toDoubleOrNull() != null &&
                        dayOfMonth.toIntOrNull() != null &&
                        merchant.isNotBlank() &&
                        (dayOfMonth.toIntOrNull() ?: 0) in 1..31,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}