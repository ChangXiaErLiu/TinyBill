package com.tinybill.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.dao.CategoryTotal
import com.tinybill.ui.components.getCategoryIconAndColor
import com.tinybill.ui.theme.PrimaryGreen

/**
 * 统计页专用 UI 组件。
 *
 * 拆分原因：原 StatisticsScreen.kt 接近 700 行，把"主屏"和"卡片"混在一起。
 * 这里只放可复用的卡片组件，主屏只负责布局 + 状态编排。
 */

/** 支出/收入汇总卡片（点击进入明细） */
@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "¥${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 分类进度条目：分类图标 + 名称 + 金额 + 占比 + 预算进度条
 *
 * - 单击：进入分类明细
 * - 长按：弹出预算设置
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryProgressItem(
    categoryTotal: CategoryTotal,
    total: Double,
    categoryBudget: Double? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val (icon, color) = getCategoryIconAndColor(categoryTotal.category)
    val percentage = if (total > 0) (categoryTotal.total / total * 100) else 0.0
    val budgetPercentage = if (categoryBudget != null && categoryBudget > 0) {
        (categoryTotal.total / categoryBudget * 100).coerceIn(0.0, 100.0)
    } else null
    val isOverBudget = categoryBudget != null && categoryTotal.total > categoryBudget

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = categoryTotal.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (categoryBudget != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "有预算",
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryGreen
                            )
                        }
                    }
                    Text(
                        text = "¥${String.format("%.2f", categoryTotal.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (budgetPercentage != null) {
                CategoryBudgetProgress(
                    spent = categoryTotal.total,
                    budget = categoryBudget ?: 0.0,
                    isOverBudget = isOverBudget
                )
            } else {
                LinearProgressIndicator(
                    progress = (percentage / 100).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
        }
    }
}

/** 分类预算进度条（数字 + 条 + 超额提示） */
@Composable
private fun CategoryBudgetProgress(
    spent: Double,
    budget: Double,
    isOverBudget: Boolean
) {
    val budgetPercentage = if (budget > 0) {
        (spent / budget * 100).coerceIn(0.0, 100.0)
    } else 0.0

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "预算",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "¥${String.format("%.2f", spent)} / ¥${String.format("%.2f", budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (budgetPercentage / 100).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isOverBudget) MaterialTheme.colorScheme.error else PrimaryGreen,
            trackColor = PrimaryGreen.copy(alpha = 0.15f)
        )
        if (isOverBudget) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "已超出 ¥${String.format("%.2f", spent - budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** 分类预算网格 3 列布局（用于统计页"分类预算"区块） */
@Composable
fun CategoryBudgetGrid(
    budgetProgressList: List<com.tinybill.data.repository.BudgetProgress>,
    onCategoryClick: (String) -> Unit,
    onCategoryLongClick: (String) -> Unit = {},
    columns: Int = 3,
    maxRows: Int = 2
) {
    val items = budgetProgressList.take(columns * maxRows)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { progress ->
                    com.tinybill.ui.components.BudgetProgressCard(
                        progress = progress,
                        onClick = { onCategoryClick(progress.budget.category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 填充空位
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 空分类预算占位 */
@Composable
fun EmptyCategoryBudget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无分类预算",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 空消费记录占位 */
@Composable
fun EmptyTransactions(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无消费记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
