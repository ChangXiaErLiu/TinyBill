package com.tinybill.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tinybill.MainActivity
import com.tinybill.data.ServiceLocator
import java.util.Calendar

class TinyBillWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ServiceLocator.getTransactionRepository(context)

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val todayExpense = repository.getTodayExpense()
        val monthExpense = repository.getMonthExpense(currentYear, currentMonth)
        val todayCount = repository.getTodayTransactionCount()

        provideContent {
            WidgetContent(
                todayExpense = todayExpense,
                monthExpense = monthExpense,
                todayCount = todayCount,
                context = context
            )
        }
    }
}

/** Widget 快捷记账 Action 常量 */
object WidgetActions {
    const val ACTION_QUICK_EXPENSE = "com.tinybill.QUICK_EXPENSE"
    const val ACTION_QUICK_INCOME = "com.tinybill.QUICK_INCOME"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_AMOUNT = "extra_amount"
}

@Composable
private fun WidgetContent(
    todayExpense: Double,
    monthExpense: Double,
    todayCount: Int,
    context: Context
) {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val quickExpenseIntent = Intent(context, MainActivity::class.java).apply {
        action = WidgetActions.ACTION_QUICK_EXPENSE
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val quickIncomeIntent = Intent(context, MainActivity::class.java).apply {
        action = WidgetActions.ACTION_QUICK_INCOME
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val white = ColorProvider(Color.White)
    val green = ColorProvider(Color(0xFF10B981))
    val darkGreen = ColorProvider(Color(0xFF059669))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(green)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App 名称 + 数据概览
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openAppIntent)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "微账",
                style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openAppIntent)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "今日", style = TextStyle(color = white, fontSize = 10.sp))
                Text(
                    text = "¥${String.format("%.0f", todayExpense)}",
                    style = TextStyle(color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Text(text = "${todayCount}笔", style = TextStyle(color = white, fontSize = 9.sp))
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "本月", style = TextStyle(color = white, fontSize = 10.sp))
                Text(
                    text = "¥${String.format("%.0f", monthExpense)}",
                    style = TextStyle(color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // 快捷记账按钮行
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 快速记一笔支出（20 元，餐饮）
            QuickAddButton(
                text = "🍜 记一笔",
                onClick = actionStartActivity(quickExpenseIntent),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            // 快速记一笔收入
            QuickAddButton(
                text = "💰 记收入",
                onClick = actionStartActivity(quickIncomeIntent),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun QuickAddButton(
    text: String,
    onClick: androidx.glance.action.Action,
    modifier: GlanceModifier = GlanceModifier
) {
    val white = ColorProvider(Color.White)
    val semiWhite = ColorProvider(Color.White.copy(alpha = 0.85f))

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0x33FFFFFF)))
            .clickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(color = white, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.padding(8.dp)
        )
    }
}

class TinyBillWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TinyBillWidget()
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TinyBillWidget().update(context, glanceId)
    }
}
