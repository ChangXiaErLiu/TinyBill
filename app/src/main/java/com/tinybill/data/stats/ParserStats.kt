package com.tinybill.data.stats

/**
 * 自动记账解析的命中率指标。
 *
 * 用于在设置页向用户展示"今日 / 本周自动记录成功率"，便于：
 * 1. 微信 / 支付宝页面改版时第一时间感知
 * 2. 决定是否需要更新关键字或重装无障碍
 */
data class ParserStats(
    val totalEvents: Int = 0,
    val parseSucceeded: Int = 0,
    val parseIgnored: Int = 0,
    val parseNotApplicable: Int = 0,
    val parseFailed: Int = 0,
    val snapshotCount: Int = 0,
    /** 自本次开机以来最早的样本时间戳，0 表示无样本。 */
    val firstSampleAt: Long = 0L,
    val lastSampleAt: Long = 0L,
) {
    /** 成功率：成功 / 全部事件。小于 0.5 时用户应被提醒。 */
    val successRate: Float
        get() = if (totalEvents == 0) 0f else parseSucceeded.toFloat() / totalEvents

    /** 失败占比：超过 0.1 通常意味着页面改版或无障碍被关。 */
    val failureRate: Float
        get() = if (totalEvents == 0) 0f else parseFailed.toFloat() / totalEvents

    /** 被 Ignored 的占比：包含"打开了微信但没付款"等正常情况。 */
    val ignoredRate: Float
        get() = if (totalEvents == 0) 0f else parseIgnored.toFloat() / totalEvents

    fun describe(): String = buildString {
        append("总事件 ").append(totalEvents)
        append(" | 成功 ").append(parseSucceeded)
        append(" | 跳过 ").append(parseIgnored)
        append(" | 忽略 ").append(parseNotApplicable)
        append(" | 失败 ").append(parseFailed)
        if (snapshotCount > 0) append(" | 快照 ").append(snapshotCount)
    }
}
