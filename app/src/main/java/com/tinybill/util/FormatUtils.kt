package com.tinybill.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * TinyBill 统一格式化工具。
 *
 * 集中管理金额/日期的格式化逻辑，避免各处重复定义。
 */
object FormatUtils {

    // ---------- 金额 ----------

    /**
     * 格式化金额显示。
     * @param amount 金额
     * @param showSign 是否显示 +/- 符号
     * @param isExpense 支出为负数前缀，收入为正数前缀（仅 [showSign]=true 时有效）
     * @param decimals 小数位数，默认 2
     */
    fun formatAmount(
        amount: Double,
        showSign: Boolean = false,
        isExpense: Boolean = true,
        decimals: Int = 2
    ): String {
        val prefix = when {
            !showSign -> ""
            isExpense -> "-"
            else -> "+"
        }
        val formatStr = "%." + decimals + "f"
        return "${prefix}¥${String.format(formatStr, amount)}"
    }

    // ---------- 日期 ----------

    private val ymdHmFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    private val mdHmFormat by lazy { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    private val ymdFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private val monthDayFormat by lazy { SimpleDateFormat("M月d日", Locale.getDefault()) }
    private val ymdHmCnFormat by lazy { SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()) }

    /** yyyy-MM-dd HH:mm */
    fun formatYmdHm(timestamp: Long): String = ymdHmFormat.format(Date(timestamp))

    /** MM-dd HH:mm */
    fun formatMdHm(timestamp: Long): String = mdHmFormat.format(Date(timestamp))

    /** yyyy-MM-dd */
    fun formatYmd(timestamp: Long): String = ymdFormat.format(Date(timestamp))

    /** M月d日 */
    fun formatMonthDay(timestamp: Long): String = monthDayFormat.format(Date(timestamp))

    /** yyyy年MM月dd日 HH:mm */
    fun formatYmdHmCn(timestamp: Long): String = ymdHmCnFormat.format(Date(timestamp))
}
