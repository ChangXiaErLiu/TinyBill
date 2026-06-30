package com.tinybill.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinybill.ui.theme.PrimaryGreen

enum class EmptyStateType {
    NO_TRANSACTIONS,
    NO_SEARCH_RESULTS,
    NO_CATEGORY_DATA,
    NO_BUDGET_DATA,
    NO_STATISTICS
}

data class EmptyStateConfig(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconColor: Color
)

@Composable
fun EmptyState(
    type: EmptyStateType,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
    actionText: String? = null
) {
    val config = when (type) {
        EmptyStateType.NO_TRANSACTIONS -> EmptyStateConfig(
            Icons.Default.Receipt,
            "暂无记账记录",
            "点击右下角按钮添加第一笔账目，\n或者开启自动记账功能",
            PrimaryGreen
        )
        EmptyStateType.NO_SEARCH_RESULTS -> EmptyStateConfig(
            Icons.Default.SearchOff,
            "没有找到相关记录",
            "换个关键词试试，\n或者调整筛选条件",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        EmptyStateType.NO_CATEGORY_DATA -> EmptyStateConfig(
            Icons.Default.PieChart,
            "暂无分类数据",
            "本月还没有该分类的支出记录",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        EmptyStateType.NO_BUDGET_DATA -> EmptyStateConfig(
            Icons.Default.AccountBalanceWallet,
            "暂无预算数据",
            "设置分类预算来控制支出",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        EmptyStateType.NO_STATISTICS -> EmptyStateConfig(
            Icons.Default.BarChart,
            "暂无统计数据",
            "记录几笔账目后\n再来查看统计报表吧",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = config.iconColor.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = config.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = config.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onActionClick != null && actionText != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    type: EmptyStateType,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
    actionText: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        EmptyState(
            type = type,
            onActionClick = onActionClick,
            actionText = actionText
        )
    }
}

@Composable
fun EmptyListPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(type = EmptyStateType.NO_TRANSACTIONS)
    }
}

@Composable
fun EmptySearchPlaceholder(
    modifier: Modifier = Modifier,
    searchKeyword: String = ""
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "没有找到「$searchKeyword」相关记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}