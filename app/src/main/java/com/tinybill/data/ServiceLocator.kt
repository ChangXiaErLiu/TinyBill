package com.tinybill.data

import android.content.Context
import com.tinybill.data.database.AppDatabase
import com.tinybill.data.repository.TransactionRepository

/**
 * 数据层服务定位器
 *
 * 解决 Glance Widget、AccessibilityService、Worker 等无法使用 Koin 注入的
 * 组件的依赖获取问题。所有 Repository 单例化，避免在每次 Widget 刷新/Service
 * 事件处理中重新创建。
 *
 * 正常使用 Koin 注入的 ViewModel/Composable 仍应优先使用 Koin，
 * 仅在系统服务/广播接收器/Widget 等非标准场景使用本类。
 */
object ServiceLocator {
    @Volatile
    private var transactionRepository: TransactionRepository? = null

    fun getTransactionRepository(context: Context): TransactionRepository {
        return transactionRepository ?: synchronized(this) {
            transactionRepository ?: TransactionRepository(
                AppDatabase.getDatabase(context).transactionDao()
            ).also { transactionRepository = it }
        }
    }
}
