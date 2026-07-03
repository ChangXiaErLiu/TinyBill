package com.tinybill.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.tinybill.data.entity.Transaction
import com.tinybill.data.entity.Template
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.data.repository.TemplateRepository
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.ui.components.TemplateQuickBar
import com.tinybill.ui.components.getCategoryIconAndColor
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.SuccessColor
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.tinybill.util.HapticManager

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    repository: TransactionRepository,
    editTransaction: Transaction? = null,
    presetTimestamp: Long? = null
) {
    val context = LocalContext.current
    val customCategoryRepository = remember { CustomCategoryPrefsRepository(context.applicationContext) }
    val customCategories by customCategoryRepository.categories.collectAsState(initial = emptyList())
    val templateRepository: TemplateRepository = koinInject()

    val initialTimestamp = editTransaction?.timestamp ?: presetTimestamp ?: System.currentTimeMillis()
    val initialIsExpense = editTransaction?.type ?: Transaction.TYPE_EXPENSE
    var isExpense by remember { mutableStateOf(initialIsExpense == Transaction.TYPE_EXPENSE) }
    var amount by remember { mutableStateOf(editTransaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(editTransaction?.merchant ?: "") }
    var selectedCategory by remember { mutableStateOf(editTransaction?.category ?: Transaction.CATEGORY_FOOD) }
    var note by remember { mutableStateOf(editTransaction?.note ?: "") }
    var timestamp by remember { mutableStateOf(initialTimestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var saveAsTemplate by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }
    var merchantTouched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 加载模板推荐（根据当前收支类型过滤）
    val topTemplates by produceState<List<com.tinybill.data.entity.Template>>(
        initialValue = emptyList(),
        key1 = isExpense
    ) {
        value = templateRepository.getTopTemplates(5)
            .filter { it.type == if (isExpense) Transaction.TYPE_EXPENSE else Transaction.TYPE_INCOME }
    }

    val isEditing = editTransaction != null
    val isPresetDate = presetTimestamp != null && editTransaction == null

    // 引用统一的图标映射（避免在 Composable 内重复创建 600+ 项 map）
    val CATEGORY_ICON_MAP = com.tinybill.ui.components.CATEGORY_ICON_MAP

    fun resolveCategoryIconAndColor(
        categoryName: String,
        customCategories: List<CustomCategoryPrefsRepository.Category>
    ): Pair<ImageVector, Color> {
        val customCategory = customCategories.find { it.name == categoryName }
        if (customCategory != null) {
            val iconVector = CATEGORY_ICON_MAP[customCategory.icon] ?: Icons.Default.MoreHoriz
            // 确保颜色正确转换 - colorValue 存储为 Long，需要转换为 ULong 再创建 Color
            val color = Color(customCategory.colorValue.toULong())
            return iconVector to color
        }
        return getCategoryIconAndColor(categoryName)
    }

    val customExpenseCategories = customCategories
        .filter { it.type == Transaction.TYPE_EXPENSE }
        .map { it.name }
    val customIncomeCategories = customCategories
        .filter { it.type == Transaction.TYPE_INCOME }
        .map { it.name }
    
    val categories = if (isExpense) {
        Transaction.EXPENSE_CATEGORIES + customExpenseCategories
    } else {
        Transaction.INCOME_CATEGORIES + customIncomeCategories
    }

    val categoryItems = categories.map { categoryName ->
        val (icon, color) = resolveCategoryIconAndColor(categoryName, customCategories)
        CategoryItem(categoryName, icon, color)
    }

    LaunchedEffect(isExpense) {
        if (selectedCategory !in categories) {
            selectedCategory = categories.first()
        }
    }
    
    // 智能分类建议
    LaunchedEffect(merchant) {
        if (merchant.isNotBlank()) {
            val suggestedCategory = repository.getMostUsedCategoryForMerchant(merchant)
            if (suggestedCategory != null && suggestedCategory in categories) {
                selectedCategory = suggestedCategory
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dialog 弹入动画：缩放 + 淡入
        val animatedScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "dialog_scale"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    alpha = animatedScale
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = when {
                        isEditing -> "编辑账单"
                        isPresetDate -> "📅 快速记账"
                        else -> "添加账单"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (isPresetDate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDateForPreset(timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TypeChip(
                        text = "支出",
                        isSelected = isExpense,
                        color = ErrorColor,
                        onClick = { isExpense = true },
                        modifier = Modifier.weight(1f)
                    )
                    TypeChip(
                        text = "收入",
                        isSelected = !isExpense,
                        color = SuccessColor,
                        onClick = { isExpense = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amountTouched = true
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                    },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("¥ ", style = MaterialTheme.typography.titleLarge) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = amountTouched && (amount.isBlank() || (amount.toDoubleOrNull() ?: 0.0) <= 0),
                    supportingText = if (amountTouched && amount.isBlank()) {
                        { Text("请输入金额", color = MaterialTheme.colorScheme.error) }
                    } else if (amountTouched && (amount.toDoubleOrNull() ?: 0.0) <= 0) {
                        { Text("金额必须大于 0", color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = merchant,
                    onValueChange = {
                        merchantTouched = true
                        merchant = it
                    },
                    label = { Text("商户名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = merchantTouched && merchant.isBlank(),
                    supportingText = if (merchantTouched && merchant.isBlank()) {
                        { Text("请输入商户名称", color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    }
                )

                // 模板快捷选择（仅非编辑模式且商户名为空时展示 top 模板）
                if (!isEditing && topTemplates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TemplateQuickBar(
                        templates = topTemplates,
                        onTemplateClick = { template ->
                            amount = if (template.amount > 0) template.amount.toString() else ""
                            merchant = template.merchant
                            selectedCategory = template.category
                            scope.launch {
                                templateRepository.useTemplate(template.id)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = formatDateTime(timestamp),
                    onValueChange = {},
                    label = { Text("消费时间") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "选择日期",
                            tint = PrimaryGreen
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoryItems) { item ->
                        CategoryChip(
                            category = item.name,
                            icon = item.icon,
                            color = item.color,
                            isSelected = item.name == selectedCategory,
                            onClick = { selectedCategory = item.name }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                // 保存为模板（仅新增模式）
                if (!isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { saveAsTemplate = !saveAsTemplate }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = saveAsTemplate,
                            onCheckedChange = { saveAsTemplate = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "保存为模板，下次快速记账",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull() ?: 0.0
                            if (amountValue > 0 && merchant.isNotBlank()) {
                                HapticManager.performSuccess()
                                val transaction = Transaction(
                                    id = editTransaction?.id ?: 0,
                                    amount = amountValue,
                                    merchant = merchant,
                                    category = selectedCategory,
                                    timestamp = timestamp,
                                    type = if (isExpense) Transaction.TYPE_EXPENSE else Transaction.TYPE_INCOME,
                                    source = editTransaction?.source ?: Transaction.SOURCE_MANUAL,
                                    note = note
                                )
                                // 保存为模板
                                if (saveAsTemplate && !isEditing) {
                                    scope.launch {
                                        templateRepository.createFromTransaction(
                                            merchant = merchant,
                                            amount = amountValue,
                                            category = selectedCategory,
                                            type = transaction.type
                                        )
                                    }
                                }
                                onConfirm(transaction)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = amount.isNotBlank() && merchant.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) ErrorColor else SuccessColor
                        )
                    ) {
                        Text(if (isEditing) "保存" else "添加", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateTimePickerDialog(
            initialTimestamp = timestamp,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedTimestamp ->
                timestamp = selectedTimestamp
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun TypeChip(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier.background(color.copy(alpha = 0.15f))
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .then(
                if (isSelected) Modifier.border(2.dp, color, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryChip(
    category: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) color.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, color, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialTimestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTimestamp }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialTimestamp
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            DatePicker(state = datePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        cal.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                        onConfirm(cal.timeInMillis)
                    }
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
    )
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDateForPreset(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()

    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val isYesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.let { yesterday ->
        calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }

    return when {
        isToday -> "今天"
        isYesterday -> "昨天"
        else -> {
            val sdf = SimpleDateFormat("M月d日", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
