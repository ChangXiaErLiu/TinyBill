package com.tinybill.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * 预算配置的 DataStore 仓库。
 *
 * 替换原来的 BudgetManager（基于 SharedPreferences，UI 主动拉取）。
 * 这里把"月度预算 / 预算模式 / 通知开关 / 分类预算"全部以 Flow 形式暴露，
 * UI 用 collectAsState() 即可响应式刷新。
 *
 * 关于分类预算的存储：保持 JSON 数组序列化是最简方案，
 * 且对 < 100 条的分类场景读写性能与查询 SQLite 没差别。
 */
private val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tinybill_budget_v2"
)

class BudgetRepository(context: Context) {

    private val store = context.applicationContext.budgetDataStore

    // ---------- 单值偏好 ----------

    val monthlyBudget: Flow<Double> = store.data
        .map { it[KEY_MONTHLY_BUDGET] ?: 0.0 }
        .distinctUntilChanged()

    val budgetMode: Flow<Int> = store.data
        .map { it[KEY_BUDGET_MODE] ?: MODE_FIXED }
        .distinctUntilChanged()

    val budgetEnabled: Flow<Boolean> = store.data
        .map { it[KEY_BUDGET_ENABLED] ?: true }
        .distinctUntilChanged()

    val lastNotifyMonth: Flow<Int> = store.data
        .map { it[KEY_LAST_NOTIFY_MONTH] ?: -1 }
        .distinctUntilChanged()

    // ---------- 分类预算（JSON 数组） ----------

    val categoryBudgets: Flow<List<CategoryBudget>> = store.data
        .map { prefs -> parseCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS]) }
        .distinctUntilChanged()

    /**
     * 把"是否开启 + 月度预算 + 模式"打包成单个 Flow，
     * 让 UI（如 BudgetSettingsDialog）只需 collect 一次。
     */
    val budgetSettings: Flow<BudgetSettings> = combine(
        budgetEnabled, monthlyBudget, budgetMode
    ) { enabled, monthly, mode ->
        BudgetSettings(enabled = enabled, monthlyBudget = monthly, mode = mode)
    }

    suspend fun setMonthlyBudget(amount: Double) {
        store.edit { it[KEY_MONTHLY_BUDGET] = amount }
    }

    suspend fun setBudgetMode(mode: Int) {
        store.edit { it[KEY_BUDGET_MODE] = mode }
    }

    suspend fun setBudgetEnabled(enabled: Boolean) {
        store.edit { it[KEY_BUDGET_ENABLED] = enabled }
    }

    suspend fun setNotifiedOverBudget() {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth =
            calendar.get(java.util.Calendar.YEAR) * 12 + calendar.get(java.util.Calendar.MONTH)
        store.edit { it[KEY_LAST_NOTIFY_MONTH] = currentMonth }
    }

    suspend fun setCategoryBudget(category: String, budget: Double) {
        store.edit { prefs ->
            val current = parseCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS]).toMutableList()
            current.removeAll { it.category == category }
            if (budget > 0) {
                current.add(CategoryBudget(category, budget))
            }
            prefs[KEY_CATEGORY_BUDGETS] = serializeCategoryBudgets(current)
        }
    }

    suspend fun removeCategoryBudget(category: String) {
        setCategoryBudget(category, budget = 0.0)
    }

    suspend fun clearAllCategoryBudgets() {
        store.edit { it[KEY_CATEGORY_BUDGETS] = "[]" }
    }

    // ---------- 工具方法 ----------

    private fun parseCategoryBudgets(json: String?): List<CategoryBudget> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CategoryBudget(
                    category = obj.getString("category"),
                    budget = obj.getDouble("budget")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeCategoryBudgets(list: List<CategoryBudget>): String {
        val arr = JSONArray()
        list.forEach { cb ->
            arr.put(
                JSONObject().apply {
                    put("category", cb.category)
                    put("budget", cb.budget)
                }
            )
        }
        return arr.toString()
    }

    data class CategoryBudget(
        val category: String,
        val budget: Double
    )

    data class BudgetSettings(
        val enabled: Boolean,
        val monthlyBudget: Double,
        val mode: Int,
    )
    companion object {
        const val MODE_FIXED = 0
        const val MODE_CATEGORY_SUM = 1

        private val KEY_MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        private val KEY_BUDGET_MODE = intPreferencesKey("budget_mode")
        private val KEY_BUDGET_ENABLED = booleanPreferencesKey("budget_enabled")
        private val KEY_LAST_NOTIFY_MONTH = intPreferencesKey("last_notify_month")
        private val KEY_CATEGORY_BUDGETS = stringPreferencesKey("category_budgets")
    }
}

/**
 * 分类预算执行进度：UI 层（StatisticsScreen / BudgetComponents）使用的视图模型。
 * 原文件位置是 TransactionRepository，2026-06 整理时挪到 BudgetRepository 旁以便维护。
 */
data class BudgetProgress(
    val budget: com.tinybill.data.entity.Budget,
    val spent: Double,
    val percentage: Float,
    val remaining: Double,
)
