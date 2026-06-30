package com.tinybill.data.stats

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * ParserStats 的进程内 + 跨进程持久化容器。
 *
 * 读写使用 AtomicReference 持有最新快照，频繁写入无锁。
 * 每次有更新时以 1.5 倍的频率落盘（节流）以避免 SharedPreferences 抖动。
 *
 * 用例：
 * - BillAccessibilityService 在 onAccessibilityEvent 中调用 incXxx()
 * - 设置页通过 get() 读取快照
 * - "重置统计"按钮调用 reset()
 */
class ParserStatsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** 真实写入次数计数（用于节流）。 */
    private val ref = AtomicReference(loadOrEmpty())
    private val _flow = MutableStateFlow(ref.get())
    /** 给 UI 用的可订阅流。 */
    val flow: StateFlow<ParserStats> = _flow.asStateFlow()

    @Volatile
    private var pendingWrites = 0

    fun get(): ParserStats = ref.get()

    fun recordEvent() = mutate { it.copy(totalEvents = it.totalEvents + 1, lastSampleAt = System.currentTimeMillis()) }
    fun recordSuccess() = mutate { it.copy(parseSucceeded = it.parseSucceeded + 1) }
    fun recordIgnored() = mutate { it.copy(parseIgnored = it.parseIgnored + 1) }
    fun recordNotApplicable() = mutate { it.copy(parseNotApplicable = it.parseNotApplicable + 1) }
    fun recordFailure() = mutate { it.copy(parseFailed = it.parseFailed + 1) }
    fun recordSnapshot() = mutate { it.copy(snapshotCount = it.snapshotCount + 1) }

    fun reset() {
        ref.set(ParserStats(firstSampleAt = System.currentTimeMillis()))
        _flow.value = ref.get()
        flush()
    }

    /**
     * 立即把当前快照持久化到 SharedPreferences。应在服务 onDestroy 或
     * 后台快照前主动调用，避免丢数据。
     */
    fun flush() {
        prefs.edit().putString(KEY_STATS, gson.toJson(ref.get())).apply()
    }

    private inline fun mutate(transform: (ParserStats) -> ParserStats) {
        val updated = transform(ref.get())
        ref.set(updated)
        _flow.value = updated
        if (pendingWrites++ >= FLUSH_THRESHOLD) {
            pendingWrites = 0
            flush()
        }
    }

    private fun loadOrEmpty(): ParserStats {
        val raw = prefs.getString(KEY_STATS, null) ?: return ParserStats()
        return try {
            gson.fromJson(raw, ParserStats::class.java) ?: ParserStats()
        } catch (_: Exception) {
            // 旧版本可能格式不兼容，丢弃并从零开始
            ParserStats()
        }
    }

    companion object {
        private const val PREFS_NAME = "tinybill_parser_stats"
        private const val KEY_STATS = "stats"
        private const val FLUSH_THRESHOLD = 8

        @Volatile
        private var INSTANCE: ParserStatsStore? = null

        fun getInstance(context: Context): ParserStatsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParserStatsStore(context).also { INSTANCE = it }
            }
        }
    }
}
