package com.tinybill.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.ui.components.getCategoryIconAndColor
import com.tinybill.ui.components.designsystem.TinyBillColors
import kotlinx.coroutines.launch

/**
 * 编辑账单的 BottomSheet。
 *
 * 与 [AddTransactionDialog] 的区别：
 * - 标题固定"编辑账单"（不再"添加账单"误导）
 * - 商户 / 时间是只读展示（编辑时不变）
 * - 字段更紧凑：金额 / 分类 / 备注 3 个核心字段
 * - 不打断上下文：BottomSheet 覆盖在详情页之上，用户随时能看到原账单
 *
 * 设计取舍：表单逻辑与 AddTransactionDialog 大部分重叠（分类选择 + 颜色）。
 * 这里不复用 AddTransactionDialog 是因为后者是 Dialog 容器 + 全量字段，差异大于复用价值。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: Transaction,
    repository: TransactionRepository,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customCategoryRepository = remember { CustomCategoryPrefsRepository(context.applicationContext) }
    val customCategories by customCategoryRepository.categories.collectAsState(initial = emptyList())

    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var note by remember { mutableStateOf(transaction.note) }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // 标题 + 顶部删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "编辑账单",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            repository.softDelete(transaction.id)
                            isSaving = false
                            onDismiss()
                        }
                    },
                    enabled = !isSaving,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = TinyBillColors.Expense,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // 只读上下文：商户 + 时间
            Text(
                text = "${transaction.merchant} · ${formatTime(transaction.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            // 金额
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = input
                    }
                },
                label = { Text("金额") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("¥ ") },
            )
            Spacer(Modifier.height(20.dp))

            // 分类
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val categories = Transaction.EXPENSE_CATEGORIES + Transaction.INCOME_CATEGORIES
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(categories) { cat ->
                    val (icon, color) = getCategoryIconAndColor(cat, customCategories)
                    CategoryChipCompact(
                        name = cat,
                        icon = icon,
                        color = color,
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
            Spacer(Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    val newAmount = amount.toDoubleOrNull() ?: return@Button
                    if (newAmount <= 0) return@Button
                    scope.launch {
                        isSaving = true
                        val updated = transaction.copy(
                            amount = newAmount,
                            category = selectedCategory,
                            note = note,
                            // type / timestamp / merchant 保持原值
                        )
                        repository.update(updated)
                        isSaving = false
                        onDismiss()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun CategoryChipCompact(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) color.copy(alpha = 0.18f) else Color.Transparent
    val border = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isSelected) color else color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(calendar.time)
}
