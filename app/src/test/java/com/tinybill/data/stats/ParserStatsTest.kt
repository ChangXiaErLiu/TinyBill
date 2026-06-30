package com.tinybill.data.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParserStatsTest {

    @Test
    fun `empty stats have zero rates`() {
        val stats = ParserStats()
        assertThat(stats.successRate).isEqualTo(0f)
        assertThat(stats.failureRate).isEqualTo(0f)
        assertThat(stats.ignoredRate).isEqualTo(0f)
    }

    @Test
    fun `success rate is parseSucceeded over total`() {
        val stats = ParserStats(
            totalEvents = 100,
            parseSucceeded = 80,
            parseIgnored = 10,
            parseNotApplicable = 5,
            parseFailed = 5
        )
        assertThat(stats.successRate).isEqualTo(0.8f)
        assertThat(stats.failureRate).isEqualTo(0.05f)
        assertThat(stats.ignoredRate).isEqualTo(0.1f)
    }

    @Test
    fun `describe includes all counters when present`() {
        val stats = ParserStats(
            totalEvents = 10,
            parseSucceeded = 5,
            parseIgnored = 2,
            parseNotApplicable = 1,
            parseFailed = 2,
            snapshotCount = 3
        )
        val desc = stats.describe()
        assertThat(desc).contains("总事件 10")
        assertThat(desc).contains("成功 5")
        assertThat(desc).contains("跳过 2")
        assertThat(desc).contains("忽略 1")
        assertThat(desc).contains("失败 2")
        assertThat(desc).contains("快照 3")
    }

    @Test
    fun `describe omits snapshot count when zero`() {
        val stats = ParserStats(totalEvents = 10, parseSucceeded = 10)
        assertThat(stats.describe()).doesNotContain("快照")
    }
}
