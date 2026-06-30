package com.tinybill.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch

/**
 * 预算设置弹窗。
 *
 * 拆分记录（2026-06）：
 *  - 旧版用 `BudgetManager` facade，函数体里 5 处 `getXxx()/setXxx()` 同步调用。
 *  - 改用 `BudgetRepository` 后，状态由 Flow 推送，UI 只需 `collectAsState` 即可。
 *  - 不再持有 `onBudgetChanged` 回调：Flow 一变，订阅方自动重组。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsDialog(
    budgetRepository: BudgetRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // -------- 订阅 Repository Flow，避免回读到 stale state --------
    val monthlyBudget by budgetRepository.monthlyBudget.collectAsState(initial = 0.0)
    val budgetMode by budgetRepository.budgetMode.collectAsState(initial = BudgetRepository.MODE_FIXED)
    val categoryBudgets by budgetRepository.categoryBudgets.collectAsState(initial = emptyList())

    // 仅作为"保存按钮触发后短暂回弹到输入框"用
    var monthlyBudgetInput by remember(monthlyBudget) { mutableStateOf(monthlyBudget.toString()) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingBudget by remember { mutableStateOf<Double?>(null) }

    val effectiveBudget = remember(budgetMode, categoryBudgets, monthlyBudget) {
        if (budgetMode == BudgetRepository.MODE_CATEGORY_SUM) {
            categoryBudgets.sumOf { it.budget }
        } else {
            monthlyBudget
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("预算设置", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "本月可用预算",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "¥${String.format("%.2f", effectiveBudget)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            if (budgetMode == BudgetRepository.MODE_CATEGORY_SUM && categoryBudgets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "（由分类预算合计：${categoryBudgets.size}个分类）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "预算模式",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetModeOption(
                            title = "固定总预算",
                            description = "设置一个月度总预算额度",
                            selected = budgetMode == BudgetRepository.MODE_FIXED,
                            onClick = {
                                scope.launch { budgetRepository.setBudgetMode(BudgetRepository.MODE_FIXED) }
                            }
                        )
                        BudgetModeOption(
                            title = "分类预算合计",
                            description = "各分类预算自动合计为总预算",
                            selected = budgetMode == BudgetRepository.MODE_CATEGORY_SUM,
                            onClick = {
                                scope.launch { budgetRepository.setBudgetMode(BudgetRepository.MODE_CATEGORY_SUM) }
                            }
                        )
                    }
                }

                if (budgetMode == BudgetRepository.MODE_FIXED) {
                    item {
                        OutlinedTextField(
                            value = monthlyBudgetInput,
                            onValueChange = { monthlyBudgetInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("月度总预算") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            prefix = { Text("¥") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val value = monthlyBudgetInput.toDoubleOrNull() ?: 0.0
                                    if (value > 0) {
                                        scope.launch { budgetRepository.setMonthlyBudget(value) }
                                    }
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "保存")
                                }
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "分类预算",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加分类")
                        }
                    }
                }

                if (categoryBudgets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "暂无分类预算",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(categoryBudgets, key = { it.category }) { categoryBudget ->
                        CategoryBudgetItem(
                            category = categoryBudget.category,
                            budget = categoryBudget.budget,
                            onEdit = {
                                editingCategory = categoryBudget.category
                                editingBudget = categoryBudget.budget
                            },
                            onDelete = {
                                scope.launch { budgetRepository.removeCategoryBudget(categoryBudget.category) }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showAddCategoryDialog) {
        CategoryBudgetEditDialog(
            title = "添加分类预算",
            category = null,
            currentBudget = null,
            existingCategories = categoryBudgets.map { it.category },
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { cat, budget ->
                scope.launch { budgetRepository.setCategoryBudget(cat, budget) }
                showAddCategoryDialog = false
            }
        )
    }

    editingCategory?.let { cat ->
        CategoryBudgetEditDialog(
            title = "编辑分类预算",
            category = cat,
            currentBudget = editingBudget,
            existingCategories = emptyList(),
            onDismiss = {
                editingCategory = null
                editingBudget = null
            },
            onConfirm = { category, budget ->
                scope.launch { budgetRepository.setCategoryBudget(category, budget) }
                editingCategory = null
                editingBudget = null
            }
        )
    }
}

@Composable
private fun BudgetModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetItem(
    category: String,
    budget: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "¥${String.format("%.2f", budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryGreen
                )
            }

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
            text = { Text("确定要删除「${category}」的预算吗？") },
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
private fun CategoryBudgetEditDialog(
    title: String,
    category: String?,
    currentBudget: Double?,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(category ?: "") }
    var amount by remember { mutableStateOf(currentBudget?.toString() ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val availableCategories = Transaction.EXPENSE_CATEGORIES.filter {
        it !in existingCategories || it == category
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (category == null) {
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
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("月度限额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    val cat = if (category.isNullOrEmpty()) selectedCategory else category
                    if (cat.isNotBlank() && amountValue > 0) {
                        onConfirm(cat, amountValue)
                    }
                },
                enabled = (category != null || selectedCategory.isNotBlank()) && (amount.toDoubleOrNull() ?: 0.0) > 0,
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
