package com.tinybill.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.data.stats.ParserStatsStore
import com.tinybill.service.parser.BillParser
import com.tinybill.service.parser.BillParser.ParseResult
import com.tinybill.service.parser.SnapshotWriter
import com.tinybill.util.CategoryKeywordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: TransactionRepository
    private lateinit var billParser: BillParser
    private lateinit var statsStore: ParserStatsStore
    private lateinit var snapshotWriter: SnapshotWriter

    private var lastProcessedTime = 0L
    private val processingInterval = 1000L
    private val recentlyProcessed = mutableMapOf<String, Long>()

    private var lastIncomeTransaction: Transaction? = null
    private var lastIncomeShowTime = 0L
    private val incomeDialogCooldown = 5000L

    companion object {
        private const val TAG = "BillAccessibilitySvc"

        private const val MAX_RECENT_ITEMS = 50
        private const val RECENT_ITEM_TTL = 60_000L

        private val _pendingIncome = MutableStateFlow<Transaction?>(null)
        val pendingIncomeTransaction: StateFlow<Transaction?> = _pendingIncome.asStateFlow()

        fun getPendingIncomeTransaction(): Transaction? = _pendingIncome.value
        fun clearPendingIncomeTransaction() {
            _pendingIncome.value = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        val keywordManager = CategoryKeywordManager.getInstance(this)
        billParser = BillParser(
            amountExtractor = com.tinybill.service.extractor.AmountExtractor(),
            merchantExtractor = com.tinybill.service.extractor.MerchantExtractor(),
            categoryClassifier = com.tinybill.service.extractor.CategoryClassifier(keywordManager)
        )
        val database = com.tinybill.data.database.AppDatabase.getDatabase(this)
        // 直接持有 Repository 而不通过 ServiceLocator：Service 实例与 Application
        // 同生命周期（onCreate/onDestroy 对称），不需要 ServiceLocator 的双检锁开销。
        repository = TransactionRepository(database.transactionDao())
        statsStore = ParserStatsStore.getInstance(this)
        snapshotWriter = SnapshotWriter(this)
        cleanupRecentItems()
    }

    override fun onDestroy() {
        // 退出时落盘最后一批计数，避免重启后少一截
        statsStore.flush()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < processingInterval) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName != BillParser.WECHAT &&
            packageName != BillParser.ALIPAY &&
            !packageName.startsWith(BillParser.UNIONPAY)
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        // 进入解析流程：先采集文本节点，失败时用于快照
        val (texts, _) = com.tinybill.service.extractor.MerchantExtractor().collectAllTexts(rootNode)
        statsStore.recordEvent()

        try {
            when (val result = billParser.parse(rootNode, packageName)) {
                is ParseResult.Expense -> {
                    statsStore.recordSuccess()
                    if (result.amount > 0 && result.merchant.isNotBlank() &&
                        !isRecentlyProcessed(result.merchant, result.amount)
                    ) {
                        saveTransaction(
                            result.amount, result.merchant, result.category,
                            Transaction.TYPE_EXPENSE
                        )
                    }
                }
                is ParseResult.Income -> {
                    statsStore.recordSuccess()
                    if (result.amount > 0 && result.merchant.isNotBlank() &&
                        !isRecentlyProcessed(result.merchant, result.amount)
                    ) {
                        saveTransaction(
                            result.amount, result.merchant, result.category,
                            result.type
                        )
                    }
                }
                is ParseResult.Ignored -> statsStore.recordIgnored()
                is ParseResult.NotApplicable -> statsStore.recordNotApplicable()
            }
        } catch (e: Exception) {
            statsStore.recordFailure()
            // 把这次失败的页面 dump 到本地，便于升级关键字库时离线分析。
            // 同步执行（AccessibilityNodeInfo 不能跨线程使用，且失败罕见，单次 < 50ms 可接受）。
            val snapshotPath = snapshotWriter.writeFailureSnapshot(
                packageName = packageName,
                rootNode = rootNode,
                reason = e.javaClass.simpleName + ": " + (e.message ?: ""),
                extractedTexts = texts
            )
            if (snapshotPath != null) {
                Log.w(TAG, "Parse failed; snapshot saved to $snapshotPath")
            } else {
                Log.e(TAG, "Parse failed and snapshot write also failed")
            }
        } finally {
            lastProcessedTime = currentTime
            rootNode.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    private fun cleanupRecentItems() {
        val now = System.currentTimeMillis()
        recentlyProcessed.entries.removeIf { (_, time) -> now - time > RECENT_ITEM_TTL }
        if (recentlyProcessed.size > MAX_RECENT_ITEMS) {
            val toRemove = recentlyProcessed.entries.sortedBy { it.value }
                .take(recentlyProcessed.size - MAX_RECENT_ITEMS)
            recentlyProcessed.entries.removeAll(toRemove.toSet())
        }
    }

    private fun isRecentlyProcessed(merchant: String, amount: Double): Boolean {
        val key = "$merchant:$amount"
        val lastTime = recentlyProcessed[key]
        return if (lastTime != null && System.currentTimeMillis() - lastTime < RECENT_ITEM_TTL) {
            true
        } else {
            recentlyProcessed[key] = System.currentTimeMillis()
            false
        }
    }

    private fun saveTransaction(
        amount: Double,
        merchant: String,
        category: String,
        type: Int
    ) {
        if (amount <= 0 || merchant.isBlank()) {
            Log.w(TAG, "Invalid transaction data")
            return
        }

        serviceScope.launch {
            try {
                val timestamp = System.currentTimeMillis()

                if (repository.isDuplicate(merchant, amount, timestamp)) {
                    Log.d(TAG, "Duplicate transaction detected")
                    return@launch
                }

                val transaction = Transaction(
                    amount = amount,
                    merchant = merchant.trim(),
                    category = category,
                    timestamp = timestamp,
                    type = type,
                    source = Transaction.SOURCE_AUTO
                )

                repository.insert(transaction)
                Log.d(TAG, "Transaction saved: category=$category type=$type")

                if (type == Transaction.TYPE_INCOME) {
                    showIncomeConfirmIfNeeded(transaction)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction", e)
            }
        }
    }

    private fun showIncomeConfirmIfNeeded(transaction: Transaction) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastIncomeShowTime < incomeDialogCooldown) {
            return
        }
        if (lastIncomeTransaction?.merchant == transaction.merchant &&
            lastIncomeTransaction?.amount == transaction.amount &&
            currentTime - lastIncomeShowTime < 30_000
        ) {
            return
        }
        lastIncomeTransaction = transaction
        lastIncomeShowTime = currentTime
        _pendingIncome.value = transaction
    }
}
