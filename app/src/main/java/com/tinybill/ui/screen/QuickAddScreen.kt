package com.tinybill.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.components.designsystem.TinyBillButton
import com.tinybill.ui.components.designsystem.TinyBillCard
import com.tinybill.ui.components.designsystem.TinyBillColors
import com.tinybill.util.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    onDismiss: () -> Unit,
    onQuickAdd: (amount: Double, category: String, isExpense: Boolean) -> Unit
) {
    var selectedAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Transaction.CATEGORY_FOOD) }
    var isExpense by remember { mutableStateOf(true) }
    var showValidationError by remember { mutableStateOf(false) }

    val amountValid = selectedAmount.isNotBlank() && selectedAmount.toDoubleOrNull() != null
    val amountValue = selectedAmount.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = amountValid && amountValue > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "快捷记账",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Type Toggle
            TypeToggle(
                isExpense = isExpense,
                onToggle = { isExpense = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Amount Display
            AmountDisplay(amount = selectedAmount)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Categories
            QuickCategorySelector(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                isExpense = isExpense
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Number Pad
            NumberPad(
                onNumberClick = { digit ->
                    HapticManager.performClick()
                    if (selectedAmount.length < 8) {
                        selectedAmount += digit
                    }
                },
                onDecimalClick = {
                    HapticManager.performClick()
                    if (!selectedAmount.contains(".")) {
                        selectedAmount += "."
                    }
                },
                onDeleteClick = {
                    HapticManager.performClick()
                    selectedAmount = selectedAmount.dropLast(1)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 验证错误提示
            if (showValidationError && !isSaveEnabled) {
                Text(
                    text = "请输入有效金额",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            // Save Button
            TinyBillButton(
                text = "保存",
                onClick = {
                    showValidationError = true
                    if (amountValue > 0) {
                        HapticManager.performSuccess()
                        onQuickAdd(amountValue, selectedCategory, isExpense)
                        onDismiss()
                    } else {
                        HapticManager.performError()
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TypeToggle(
    isExpense: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isExpense) TinyBillColors.Expense else Color.Transparent)
                .clickable { HapticManager.performClick(); onToggle(true) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "支出",
                color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isExpense) TinyBillColors.Income else Color.Transparent)
                .clickable { HapticManager.performClick(); onToggle(false) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "收入",
                color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AmountDisplay(amount: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (amount.isEmpty()) "¥0.00" else "¥$amount",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuickCategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isExpense: Boolean
) {
    val categories = if (isExpense) {
        Transaction.EXPENSE_CATEGORIES
    } else {
        Transaction.INCOME_CATEGORIES
    }
    
    val categoryIcons = mapOf(
        Transaction.CATEGORY_FOOD to "🍔",
        Transaction.CATEGORY_SHOPPING to "🛍️",
        Transaction.CATEGORY_TRANSPORT to "🚗",
        Transaction.CATEGORY_ENTERTAINMENT to "🎮",
        Transaction.CATEGORY_RENT to "🏠",
        Transaction.CATEGORY_UTILITY to "💡",
        Transaction.CATEGORY_BEAUTY to "💄",
        Transaction.CATEGORY_SALARY to "💰",
        Transaction.CATEGORY_RED_PACKET to "🧧",
        Transaction.CATEGORY_TRANSFER to "💸",
        Transaction.CATEGORY_OTHER to "�",
        Transaction.CATEGORY_OTHER_INCOME to "💵"
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            if (isExpense) TinyBillColors.Expense.copy(alpha = 0.15f)
                            else TinyBillColors.Income.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = categoryIcons[category] ?: "📦",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        if (isExpense) TinyBillColors.Expense else TinyBillColors.Income
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )
        
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    NumberKey(
                        key = key,
                        onClick = {
                            when (key) {
                                "." -> onDecimalClick()
                                "⌫" -> onDeleteClick()
                                else -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = key,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
