package com.tinybill.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        // 主页列表：isDeleted 过滤 + 按时间倒序，最常用查询
        Index(value = ["isDeleted", "timestamp"]),
        // 去重检测：精确匹配 (merchant, amount) + 时间窗口
        Index(value = ["merchant", "amount", "timestamp"]),
        // 分类聚合：分类支出 / 分类汇总 GROUP BY
        Index(value = ["category", "timestamp", "type"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val type: Int = 0,
    val source: Int = 0,
    val note: String = "",
    val isDeleted: Int = 0
) {
    companion object {
        const val SOURCE_AUTO = 0
        const val SOURCE_MANUAL = 1
        const val SOURCE_SCHEDULED = 2

        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1

        const val CATEGORY_FOOD = "餐饮"
        const val CATEGORY_SHOPPING = "购物"
        const val CATEGORY_RENT = "房租"
        const val CATEGORY_UTILITY = "水电"
        const val CATEGORY_TRANSPORT = "交通"
        const val CATEGORY_ENTERTAINMENT = "娱乐"
        const val CATEGORY_BEAUTY = "美妆"
        const val CATEGORY_OTHER = "其他"

        const val CATEGORY_SALARY = "工资"
        const val CATEGORY_RED_PACKET = "红包"
        const val CATEGORY_TRANSFER = "转账"
        const val CATEGORY_OTHER_INCOME = "其他收入"

        val EXPENSE_CATEGORIES = listOf(
            CATEGORY_FOOD,
            CATEGORY_SHOPPING,
            CATEGORY_RENT,
            CATEGORY_UTILITY,
            CATEGORY_TRANSPORT,
            CATEGORY_ENTERTAINMENT,
            CATEGORY_BEAUTY,
            CATEGORY_OTHER
        )

        val INCOME_CATEGORIES = listOf(
            CATEGORY_SALARY,
            CATEGORY_RED_PACKET,
            CATEGORY_TRANSFER,
            CATEGORY_OTHER_INCOME
        )

        val ALL_CATEGORIES = EXPENSE_CATEGORIES + INCOME_CATEGORIES
    }
}

/**
 * 预算数据类（非 Room Entity）。
 *
 * 预算数据实际由 BudgetRepository（DataStore）持久化。
 * 本类仅作 UI 层展示和备份导出的 DTO。
 */
data class Budget(
    val category: String,
    val monthlyLimit: Double,
    val enabled: Boolean = true,
    val alertThreshold: Float = 0.8f
)

@Entity(tableName = "scheduled_transaction")
data class ScheduledTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val type: Int = 0,
    val dayOfMonth: Int,
    val enabled: Int = 1,
    val lastExecuted: Long = 0
)
