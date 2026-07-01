package com.tinybill.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tinybill.R

/**
 * 通知工具类。
 *
 * 负责创建通知渠道和发送通知。
 * 当前用途：预算超支提醒。
 */
object NotificationHelper {

    const val CHANNEL_BUDGET = "budget_alert"
    const val CHANNEL_SCHEDULED = "scheduled_bill"

    private var initialized = false

    /**
     * 创建通知渠道（在 Application.onCreate 中调用一次即可）。
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_BUDGET,
                    "预算提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "当分类支出超过预算时发送提醒"
                },
                NotificationChannel(
                    CHANNEL_SCHEDULED,
                    "定期账单",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "每月定期账单自动记账通知"
                }
            )

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    /**
     * 发送预算超支通知。
     */
    fun sendBudgetAlert(context: Context, category: String, spent: Double, limit: Double, percentage: Float) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (percentage >= 100) "预算已超支！" else "预算即将用尽"
        val body = if (percentage >= 100) {
            "「$category」本月已花费 ¥${String.format("%.0f", spent)}，超过预算 ¥${String.format("%.0f", limit)}"
        } else {
            "「$category」本月已花费 ¥${String.format("%.0f", spent)}，已达到预算的 ${String.format("%.0f", percentage)}%"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 用 category 的 hashCode 作为固定通知 ID，这样同一分类的新通知会替换旧通知
        nm.notify("budget_${category}".hashCode(), notification)
    }
}
