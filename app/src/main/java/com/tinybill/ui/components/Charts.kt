package com.tinybill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.SuccessColor
import com.tinybill.ui.components.ChartDrawers.drawLineChart
import com.tinybill.ui.components.ChartDrawers.drawLineChartLabels
import com.tinybill.ui.components.ChartDrawers.drawDonutPie
import com.tinybill.ui.components.ChartDrawers.drawBar
import java.util.Calendar
import kotlin.math.max

/**
 * 图表 UI 壳。
 *
 * 拆分说明：
 *  - 纯数据模型在 [ChartModels]
 *  - Canvas 绘制逻辑（drawScope 函数）在 [ChartDrawers]
 *  - 这里只负责响应式布局、BoxWithConstraints 尺寸探测、空态提示与图例
 *
 * 这三个文件加起来代码量与原来相当，但每层关注点单一。
 */

/**
 * 折线图：支出走势，可选叠加收入。
 */
@Composable
fun LineChart(
    data: List<TrendData>,
    modifier: Modifier = Modifier,
    lineColor: Color = ErrorColor,
    incomeColor: Color = SuccessColor,
    showIncome: Boolean = false,
    showDataLabel: Boolean = true
) {
    if (data.isEmpty()) return EmptyChart(modifier.height(220.dp))

    val maxValue = max(
        data.maxOf { it.expense },
        if (showIncome) data.maxOf { it.income } else 0.0
    ).let { if (it == 0.0) 1.0 else it }

    BoxWithConstraints(modifier = modifier) {
        val isLargeScreen = maxWidth > 600.dp
        val chartHeight = if (isLargeScreen) 280.dp else 180.dp
        val padding = if (isLargeScreen) 56.dp else 40.dp
        val fontSize = if (isLargeScreen) 12.sp else 10.sp
        val density = LocalDensity.current

        val outlineColor = MaterialTheme.colorScheme.outline

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(horizontal = padding)
                ) {
                    drawLineChart(
                        data = data,
                        maxValue = maxValue,
                        paddingPx = padding.toPx(),
                        lineColor = lineColor,
                        incomeColor = incomeColor,
                        showIncome = showIncome,
                        outlineColor = outlineColor
                    )
                }

                if (showDataLabel) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                            .padding(horizontal = padding)
                    ) {
                        drawLineChartLabels(
                            data = data,
                            maxValue = maxValue,
                            paddingPx = padding.toPx(),
                            textSizePx = with(density) { fontSize.toPx() },
                            showIncome = showIncome
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X 轴日期
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { item ->
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showIncome) {
                Spacer(modifier = Modifier.height(8.dp))
                ChartLegend(
                    items = listOf(
                        LegendItem("支出", lineColor),
                        LegendItem("收入", incomeColor)
                    )
                )
            }
        }
    }
}

/**
 * 饼图：分类占比。
 */
@Composable
fun PieChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    total: Double = data.sumOf { it.value }
) {
    if (data.isEmpty() || total == 0.0) return EmptyChart(modifier.size(200.dp))

    BoxWithConstraints(modifier = modifier) {
        val isLargeScreen = maxWidth > 500.dp
        val pieSize = if (isLargeScreen) 220.dp else 180.dp
        val centerHoleColor = MaterialTheme.colorScheme.background
        val visibleCount = if (isLargeScreen) 8 else 5

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLargeScreen) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(pieSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(pieSize)) {
                    drawDonutPie(
                        data = data,
                        total = total,
                        centerHoleColor = centerHoleColor
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总计",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${String.format("%.0f", total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 图例
            Column(
                verticalArrangement = Arrangement.spacedBy(if (isLargeScreen) 10.dp else 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                data.take(visibleCount).forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(slice.color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${String.format("%.1f", slice.value / total * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (data.size > visibleCount) {
                    Text(
                        text = "等${data.size - visibleCount}项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 简单柱状图（单系列）。
 */
@Composable
fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return EmptyChart(modifier.height(200.dp))

    BoxWithConstraints(modifier = modifier) {
        val isLargeScreen = maxWidth > 500.dp
        val chartHeight = if (isLargeScreen) 220.dp else 180.dp
        val barWidth = if (isLargeScreen) 32.dp else 24.dp
        val maxValue = data.maxOf { it.value }.let { if (it == 0.0) 1.0 else it }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    val barHeight = (item.value / maxValue * (chartHeight.value - 40)).toFloat()

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.0f", item.value),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barHeight.dp)
                        ) {
                            drawBar(color = item.color, cornerRadius = 4.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { item ->
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (isLargeScreen) 40.dp else 32.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 双柱柱状图（支出 + 收入），可横向滚动，自动定位到当前日期附近。
 */
@Composable
fun BarChartWithIncome(
    expenseData: List<ChartData>,
    incomeData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (expenseData.isEmpty() && incomeData.isEmpty()) {
        return EmptyChart(modifier.height(240.dp))
    }

    val maxExpense = expenseData.maxOfOrNull { it.value } ?: 0.0
    val maxIncome = incomeData.maxOfOrNull { it.value } ?: 0.0
    val maxValue = maxOf(maxExpense, maxIncome).let { if (it == 0.0) 1.0 else it }

    val scrollState = rememberScrollState()
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val itemWidth = 48
    val targetPosition = (today - 3).coerceAtLeast(0) * itemWidth

    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.animateScrollTo(targetPosition.coerceIn(0, scrollState.maxValue))
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isLargeScreen = maxWidth > 600.dp
        val chartHeight = if (isLargeScreen) 200.dp else 150.dp
        val barWidth = if (isLargeScreen) 16.dp else 12.dp
        val itemSpacing = if (isLargeScreen) 12.dp else 8.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 30.dp)
                    .horizontalScroll(scrollState)
            ) {
                Column {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                        ) {
                            expenseData.forEachIndexed { index, item ->
                                val expenseHeight = (item.value / maxValue * (chartHeight.value - 20)).toFloat()
                                val incomeHeight = incomeData.getOrNull(index)
                                    ?.let { (it.value / maxValue * (chartHeight.value - 20)).toFloat() }
                                    ?: 0f

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(if (isLargeScreen) 48.dp else 40.dp)
                                ) {
                                    if (expenseHeight > 0 || incomeHeight > 0) {
                                        Text(
                                            text = "¥${String.format("%.0f", item.value + (incomeData.getOrNull(index)?.value ?: 0.0))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = if (isLargeScreen) 10.sp else 8.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        if (incomeHeight > 0) {
                                            Canvas(
                                                modifier = Modifier
                                                    .width(barWidth)
                                                    .height(incomeHeight.dp)
                                            ) {
                                                drawBar(color = SuccessColor, cornerRadius = 4.dp)
                                            }
                                        }
                                        if (expenseHeight > 0) {
                                            Canvas(
                                                modifier = Modifier
                                                    .width(barWidth)
                                                    .height(expenseHeight.dp)
                                            ) {
                                                drawBar(color = ErrorColor, cornerRadius = 4.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // X 轴
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .horizontalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    expenseData.forEach { item ->
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(if (isLargeScreen) 48.dp else 40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ChartLegend(
                items = listOf(
                    LegendItem("支出", ErrorColor),
                    LegendItem("收入", SuccessColor)
                )
            )
        }
    }
}

// -------------------- 私有辅助 --------------------

/** 空数据占位 */
@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class LegendItem(val label: String, val color: Color)

/** 通用图例：色块 + 名称 */
@Composable
private fun ChartLegend(items: List<LegendItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(item.color, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
