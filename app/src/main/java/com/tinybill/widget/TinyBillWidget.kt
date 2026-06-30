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
        // 通过 ServiceLocator 复用 Repository 单例，避免每次 Widget 刷新
        // 都新建 Repository（连带打开 Room 的 dao 引用）
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

@Composable
private fun WidgetContent(
    todayExpense: Double,
    monthExpense: Double,
    todayCount: Int,
    context: Context
) {
    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    val white = ColorProvider(Color.White)
    val green = ColorProvider(Color(0xFF10B981))
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(green)
            .padding(16.dp)
            .clickable(actionStartActivity(activityIntent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "微账",
            style = TextStyle(
                color = white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今日支出",
                    style = TextStyle(
                        color = white,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = "¥${String.format("%.2f", todayExpense)}",
                    style = TextStyle(
                        color = white,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${todayCount}笔",
                    style = TextStyle(
                        color = white,
                        fontSize = 10.sp
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.width(16.dp))
            
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "本月支出",
                    style = TextStyle(
                        color = white,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = "¥${String.format("%.2f", monthExpense)}",
                    style = TextStyle(
                        color = white,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
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
