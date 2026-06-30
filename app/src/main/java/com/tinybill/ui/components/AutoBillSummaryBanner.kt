package com.tinybill.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.stats.ParserStats
import com.tinybill.data.stats.ParserStatsStore
import com.tinybill.ui.components.designsystem.TinyBillColors
import kotlinx.coroutines.flow.StateFlow

/**
 * 首页"自动记账摘要"反馈条。
 *
 * - 完全没有事件（用户还没用过）→ 完全隐藏，不打扰
 * - 成功率 > 80% → 绿色，"已自动记录 N 笔"
 * - 成功率 30-80% → 黄色，"成功率偏低，请检查"
 * - 成功率 < 30% → 红色，"解析失败较多"
 *
 * 状态变化时通过 Flow 自动更新（BillAccessibilityService 每解析一次都会触发）。
 */
@Composable
fun AutoBillSummaryBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // ParserStatsStore 是单例 + AtomicReference，频繁 collectAsState 无成本
    val store = remember { ParserStatsStore.getInstance(context) }
    val stats by store.flow.collectAsStateCompat()

    // 完全没有事件就不显示，避免"今日 0 笔"这种空信息
    val hasData = remember(stats) { stats.totalEvents > 0 }
    if (!hasData) return

    val visual = remember(stats) { resolveVisual(stats) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(visual.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = visual.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun <T> StateFlow<T>.collectAsStateCompat(): State<T> =
    collectAsState(initial = this.value)

private fun resolveVisual(stats: ParserStats): BannerVisual {
    val rate = stats.successRate
    val total = stats.parseSucceeded
    return when {
        rate >= 0.8f -> BannerVisual(
            text = "已自动记录 $total 笔 · 成功率 ${(rate * 100).toInt()}%",
            icon = Icons.Default.AutoAwesome,
            tint = TinyBillColors.Income,
            background = TinyBillColors.Income.copy(alpha = 0.12f),
        )
        rate >= 0.3f -> BannerVisual(
            text = "自动记账成功率偏低（${(rate * 100).toInt()}%），建议检查无障碍服务",
            icon = Icons.Default.Warning,
            tint = TinyBillColors.Warning,
            background = TinyBillColors.Warning.copy(alpha = 0.15f),
        )
        else -> BannerVisual(
            text = "解析失败较多（${stats.parseFailed} 次），可能是微信/支付宝页面改版",
            icon = Icons.Default.Error,
            tint = TinyBillColors.Expense,
            background = TinyBillColors.Expense.copy(alpha = 0.12f),
        )
    }
}

private data class BannerVisual(
    val text: String,
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
)
