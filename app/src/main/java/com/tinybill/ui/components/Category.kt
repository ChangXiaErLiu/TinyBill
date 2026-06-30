package com.tinybill.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*


// CATEGORY_ICON_MAP 已抽到 [com.tinybill.ui.components.CategoryIcons] 统一管理
@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val context = LocalContext.current
    val customCategoryRepository = remember { CustomCategoryPrefsRepository(context.applicationContext) }
    val customCategories by customCategoryRepository.categories.collectAsState(initial = emptyList())

    val (icon, color) = getCategoryIconAndColor(category, customCategories)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

fun getCategoryIconAndColor(category: String, customCategories: List<CustomCategoryPrefsRepository.Category> = emptyList()): Pair<ImageVector, Color> {
    // 首先检查是否是自定义分类
    val customCategory = customCategories.find { it.name == category }
    if (customCategory != null) {
        val iconVector = CATEGORY_ICON_MAP[customCategory.icon] ?: Icons.Default.MoreHoriz
        val color = Color(customCategory.colorValue.toULong())
        return iconVector to color
    }
    
    // 预设分类
    return when (category) {
        Transaction.CATEGORY_FOOD -> Icons.Default.Restaurant to FoodColor
        Transaction.CATEGORY_SHOPPING -> Icons.Default.ShoppingBag to ShoppingColor
        Transaction.CATEGORY_RENT -> Icons.Default.Home to LivingColor
        Transaction.CATEGORY_UTILITY -> Icons.Default.Bolt to LivingColor
        Transaction.CATEGORY_TRANSPORT -> Icons.Default.DirectionsBus to TransportColor
        Transaction.CATEGORY_ENTERTAINMENT -> Icons.Default.Movie to EntertainmentColor
        Transaction.CATEGORY_BEAUTY -> Icons.Default.Face to MedicalColor
        Transaction.CATEGORY_OTHER -> Icons.Default.MoreHoriz to OtherColor

        Transaction.CATEGORY_SALARY -> Icons.Default.AttachMoney to SuccessColor
        Transaction.CATEGORY_RED_PACKET -> Icons.Default.Favorite to ErrorColor
        Transaction.CATEGORY_TRANSFER -> Icons.Default.SwapHoriz to PrimaryGreen
        Transaction.CATEGORY_OTHER_INCOME -> Icons.Default.AddCircle to WarningColor

        else -> Icons.Default.MoreHoriz to OtherColor
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == Transaction.TYPE_EXPENSE
    val amountColor = if (isExpense) MaterialTheme.colorScheme.error else SuccessColor
    val amountPrefix = if (isExpense) "-" else "+"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category = transaction.category)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    AnimatedVisibility(
                        visible = transaction.source == Transaction.SOURCE_AUTO,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryGreen.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "自动",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isExpense) ErrorColor.copy(alpha = 0.12f) else SuccessColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isExpense) "支出" else "收入",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExpense) ErrorColor else SuccessColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatFullDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (transaction.note.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${amountPrefix}¥${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
