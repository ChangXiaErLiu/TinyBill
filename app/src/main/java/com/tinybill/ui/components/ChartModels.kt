package com.tinybill.ui.components

import androidx.compose.ui.graphics.Color
import com.tinybill.ui.theme.PrimaryGreen

/**
 * 图表数据模型。
 *
 * 拆分原因：原 Charts.kt 把数据类、UI shell、Canvas 绘制逻辑都堆在一起，
 * 693 行文件混杂 3 种关注点。重构后这里只放纯数据，UI 壳在 Charts.kt，绘制在 ChartDrawers.kt。
 */
data class ChartData(
    val label: String,
    val value: Double,
    val color: Color = PrimaryGreen
)

data class TrendData(
    val date: String,
    val expense: Double,
    val income: Double = 0.0
)
