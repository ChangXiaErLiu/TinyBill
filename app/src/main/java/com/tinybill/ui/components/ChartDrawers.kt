package com.tinybill.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp

/**
 * 纯 Canvas 绘制层。
 *
 * 这里只接收 DrawScope + 几何/样式参数，**不依赖任何 @Composable、状态或资源 ID**。
 * 好处：
 *  1. 与 UI 壳解耦后，能在单元测试中传 mock DrawScope 验证绘制参数；
 *  2. 多个图表 UI 壳可以复用同一份绘制逻辑（LineChart / 缩略图 / 导出等）。
 *  3. 拆分后单文件不再超过 300 行。
 */
object ChartDrawers {

    // -------------------- 折线图 --------------------

    /**
     * 折线图主绘制（不含数据标签，标签在 [drawLineChartLabels]）。
     *
     * @param data 数据点
     * @param maxValue Y 轴最大值（外部预算好传入，避免每次重算）
     * @param paddingPx 内边距（像素）
     * @param lineColor 支出线颜色
     * @param incomeColor 收入线颜色
     * @param showIncome 是否绘制收入
     * @param outlineColor 网格线颜色
     */
    fun DrawScope.drawLineChart(
        data: List<TrendData>,
        maxValue: Double,
        paddingPx: Float,
        lineColor: Color,
        incomeColor: Color,
        showIncome: Boolean,
        outlineColor: Color
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val chartWidth = canvasWidth - paddingPx * 2
        val chartHeightInner = canvasHeight - paddingPx * 2
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        // 底部基线
        drawPath(
            path = Path().apply {
                moveTo(paddingPx, canvasHeight - paddingPx)
                lineTo(canvasWidth - paddingPx, canvasHeight - paddingPx)
            },
            color = outlineColor,
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        )

        // 支出折线
        val expensePath = Path()
        data.forEachIndexed { index, item ->
            val x = paddingPx + index * stepX
            val y = canvasHeight - paddingPx -
                (item.expense / maxValue * chartHeightInner).toFloat()
            if (index == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
        }
        drawPath(path = expensePath, color = lineColor, style = Stroke(width = 3f))

        // 支出圆点
        data.forEachIndexed { index, item ->
            val x = paddingPx + index * stepX
            val y = canvasHeight - paddingPx -
                (item.expense / maxValue * chartHeightInner).toFloat()
            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
        }

        // 收入折线
        if (showIncome && data.any { it.income > 0 }) {
            val incomePath = Path()
            data.forEachIndexed { index, item ->
                val x = paddingPx + index * stepX
                val y = canvasHeight - paddingPx -
                    (item.income / maxValue * chartHeightInner).toFloat()
                if (index == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
            }
            drawPath(path = incomePath, color = incomeColor, style = Stroke(width = 3f))

            data.forEachIndexed { index, item ->
                if (item.income > 0) {
                    val x = paddingPx + index * stepX
                    val y = canvasHeight - paddingPx -
                        (item.income / maxValue * chartHeightInner).toFloat()
                    drawCircle(color = incomeColor, radius = 5f, center = Offset(x, y))
                }
            }
        }
    }

    /**
     * 折线图数据标签（用 nativeCanvas 文字绘制）。
     */
    fun DrawScope.drawLineChartLabels(
        data: List<TrendData>,
        maxValue: Double,
        paddingPx: Float,
        textSizePx: Float,
        showIncome: Boolean,
        incomeColorHex: String = "#10B981",
        labelColorHex: String = "#666666"
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val chartWidth = canvasWidth - paddingPx * 2
        val chartHeightInner = canvasHeight - paddingPx * 2
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(labelColorHex)
            textSize = textSizePx
            textAlign = android.graphics.Paint.Align.CENTER
        }

        data.forEachIndexed { index, item ->
            val x = paddingPx + index * stepX
            val y = canvasHeight - paddingPx -
                (item.expense / maxValue * chartHeightInner).toFloat()

            if (item.expense > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "¥${String.format("%.0f", item.expense)}",
                    x, y - 12f, labelPaint
                )
            }
        }

        if (showIncome && data.any { it.income > 0 }) {
            val incomePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(incomeColorHex)
                textSize = textSizePx
                textAlign = android.graphics.Paint.Align.CENTER
            }
            data.forEachIndexed { index, item ->
                if (item.income > 0) {
                    val x = paddingPx + index * stepX
                    val y = canvasHeight - paddingPx -
                        (item.income / maxValue * chartHeightInner).toFloat()
                    drawContext.canvas.nativeCanvas.drawText(
                        "¥${String.format("%.0f", item.income)}",
                        x, y + 20f, incomePaint
                    )
                }
            }
        }
    }

    // -------------------- 饼图 --------------------

    /**
     * 环形饼图（中心挖洞），图例由 UI 壳绘制。
     */
    fun DrawScope.drawDonutPie(
        data: List<ChartData>,
        total: Double,
        centerHoleColor: Color
    ) {
        var startAngle = -90f
        val sweepAngle = 360f

        data.forEach { slice ->
            val angle = (slice.value / total * sweepAngle).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = angle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += angle
        }

        // 中心挖洞：洞的半径固定为 1/3 直径
        drawCircle(
            color = centerHoleColor,
            radius = size.width / 3f,
            center = Offset(size.width / 2, size.height / 2)
        )
    }

    // -------------------- 柱状图 --------------------

    /**
     * 单根柱子（圆角矩形）。
     *
     * UI 壳在 [Modifier] 里设置宽高，绘制层只负责颜色与圆角半径。
     * 这种"几何参数在 UI 层、视觉参数在绘制层"的分层有助于：
     *  - 同一绘制逻辑可被不同尺寸的 UI 复用（柱状图、迷你图、导出图）
     *  - 单元测试可断言颜色/圆角，而不是被尺寸绑死
     */
    fun DrawScope.drawBar(
        color: Color,
        cornerRadius: Dp
    ) {
        drawRoundRect(
            color = color,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
}
