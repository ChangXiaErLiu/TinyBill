package com.tinybill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinybill.data.dao.CategoryTotal
import com.tinybill.data.dao.DailyTotal
import com.tinybill.data.entity.Budget
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.BudgetProgress
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 统计页状态。
 *
 * 拆分原因：原 StatisticsScreen 把"加载/汇总/分类预算"全部塞进 Composable 函数体，
 * 配合 N 个 mutableStateOf 字段后函数体接近 200 行，且每次重组都会重新计算。
 * 把状态聚合到 ViewModel 后，UI 只需 collectAsState 即可。
 */
data class StatisticsState(
    val selectedTab: Int = 0,                // 0=本月 1=本年
    val showExpense: Boolean = true,         // 分类明细 tab
    val currentYear: Int = 0,
    val currentMonth: Int = 0,

    // 月度
    val monthExpense: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthCategoryList: List<CategoryTotal> = emptyList(),
    val monthIncomeCategoryList: List<CategoryTotal> = emptyList(),
    val dailyExpenseList: List<DailyTotal> = emptyList(),
    val dailyIncomeList: List<DailyTotal> = emptyList(),

    // 年度
    val yearExpense: Double = 0.0,
    val yearIncome: Double = 0.0,
    val yearCategoryList: List<CategoryTotal> = emptyList(),

    // 支出趋势（近 12 个月，按月升序）
    val monthlyTrend: List<Pair<String, Double>> = emptyList(),

    // 分类预算
    val categoryBudgets: List<BudgetRepository.CategoryBudget> = emptyList(),
    val budgetProgressList: List<BudgetProgress> = emptyList(),

    val isLoading: Boolean = false
) {
    val netAmount: Double
        get() = if (selectedTab == 0) monthIncome - monthExpense else yearIncome - yearExpense
}

sealed interface StatisticsUserEvent {
    data class SelectTab(val tab: Int) : StatisticsUserEvent
    data class SwitchType(val isExpense: Boolean) : StatisticsUserEvent
    data class ChangeMonth(val year: Int, val month: Int) : StatisticsUserEvent
    data object PrevMonth : StatisticsUserEvent
    data object NextMonth : StatisticsUserEvent
    data object Refresh : StatisticsUserEvent
}

/**
 * 统计页数据加载 + 状态管理。
 *
 * 设计要点：
 *  - 单一入口 [loadData]：按 (tab, year, month) 缓存键触发
 *  - 分类预算进度单独异步计算：因为它需要遍历每个分类的 1~12 月汇总
 *  - 不持有 UI 引用，全部通过 StateFlow 暴露
 */
class StatisticsViewModel(
    private val repository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        initializeState()
        // 分类预算由 BudgetRepository 自身暴露 Flow，这里订阅
        viewModelScope.launch {
            budgetRepository.categoryBudgets.collect { list ->
                _state.update { it.copy(categoryBudgets = list) }
                recalcBudgetProgress(list)
            }
        }
    }

    private fun initializeState() {
        val now = Calendar.getInstance()
        _state.update {
            it.copy(
                currentYear = now.get(Calendar.YEAR),
                currentMonth = now.get(Calendar.MONTH) + 1
            )
        }
        loadData()
    }

    fun onEvent(event: StatisticsUserEvent) {
        when (event) {
            is StatisticsUserEvent.SelectTab -> {
                _state.update { it.copy(selectedTab = event.tab) }
                loadData()
            }
            is StatisticsUserEvent.SwitchType -> {
                _state.update { it.copy(showExpense = event.isExpense) }
            }
            is StatisticsUserEvent.ChangeMonth -> {
                _state.update { it.copy(currentYear = event.year, currentMonth = event.month) }
                loadData()
            }
            StatisticsUserEvent.PrevMonth -> {
                val s = _state.value
                if (s.currentMonth == 1) {
                    _state.update { it.copy(currentMonth = 12, currentYear = s.currentYear - 1) }
                } else {
                    _state.update { it.copy(currentMonth = s.currentMonth - 1) }
                }
                loadData()
            }
            StatisticsUserEvent.NextMonth -> {
                val s = _state.value
                if (s.currentMonth == 12) {
                    _state.update { it.copy(currentMonth = 1, currentYear = s.currentYear + 1) }
                } else {
                    _state.update { it.copy(currentMonth = s.currentMonth + 1) }
                }
                loadData()
            }
            StatisticsUserEvent.Refresh -> loadData()
        }
    }

    /**
     * 加载汇总数据（月度或年度）。
     * 用 try/finally 保证 isLoading 一定会被复位。
     */
    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val s = _state.value
                if (s.selectedTab == 0) {
                    val expense = repository.getMonthExpense(s.currentYear, s.currentMonth)
                    val income = repository.getMonthIncome(s.currentYear, s.currentMonth)
                    val monthCats = repository.getMonthCategorySummary(s.currentYear, s.currentMonth)
                    val monthIncomeCats = repository.getMonthIncomeCategorySummary(s.currentYear, s.currentMonth)
                    val dailyExp = repository.getDailyExpenseForMonth(s.currentYear, s.currentMonth)
                    val dailyInc = repository.getDailyIncomeForMonth(s.currentYear, s.currentMonth)
                    val trend = repository.getMonthlyExpenseTrend(s.currentYear, s.currentMonth)
                    _state.update {
                        it.copy(
                            monthExpense = expense,
                            monthIncome = income,
                            monthCategoryList = monthCats,
                            monthIncomeCategoryList = monthIncomeCats,
                            dailyExpenseList = dailyExp,
                            dailyIncomeList = dailyInc,
                            monthlyTrend = trend
                        )
                    }
                } else {
                    val yearExp = repository.getYearExpense(s.currentYear)
                    val yearInc = repository.getYearIncome(s.currentYear)
                    val yearCats = repository.getYearCategorySummary(s.currentYear)
                    _state.update {
                        it.copy(
                            yearExpense = yearExp,
                            yearIncome = yearInc,
                            yearCategoryList = yearCats
                        )
                    }
                }
                recalcBudgetProgress(_state.value.categoryBudgets)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 重新计算每个分类预算的"已花/百分比"。
     *
     * 选中"本月"时按 (year, month) 取值；选中"本年"时累加 1~12 月。
     */
    private suspend fun recalcBudgetProgress(
        categoryBudgets: List<BudgetRepository.CategoryBudget>
    ) {
        val s = _state.value
        val list = categoryBudgets.map { cb ->
            val spent = if (s.selectedTab == 0) {
                repository.getCategoryExpense(s.currentYear, s.currentMonth, cb.category) ?: 0.0
            } else {
                var yearSpent = 0.0
                for (m in 1..12) {
                    yearSpent += repository.getCategoryExpense(s.currentYear, m, cb.category) ?: 0.0
                }
                yearSpent
            }
            val percentage = if (cb.budget > 0) {
                (spent / cb.budget * 100).toFloat().coerceIn(0f, 100f)
            } else 0f
            BudgetProgress(
                budget = Budget(
                    category = cb.category,
                    monthlyLimit = cb.budget,
                    enabled = true
                ),
                spent = spent,
                percentage = percentage,
                remaining = cb.budget - spent
            )
        }
        _state.update { it.copy(budgetProgressList = list) }
    }

    /**
     * 异步获取明细列表（支出/收入），按时间倒序。
     */
    suspend fun loadTransactions(
        isExpense: Boolean,
        year: Int,
        month: Int,
        isYear: Boolean
    ): List<Transaction> {
        val raw = if (isYear) {
            repository.getTransactionsByYearRange(year)
        } else {
            repository.getTransactionsByMonthRange(year, month)
        }
        val type = if (isExpense) Transaction.TYPE_EXPENSE else Transaction.TYPE_INCOME
        return raw
            .filter { it.type == type && it.isDeleted == 0 }
            .sortedByDescending { it.timestamp }
    }

    /**
     * 设置分类预算。
     * UI 通过 ViewModel 调用，而非直接持有 Repository 引用，保证单一数据流。
     */
    fun setCategoryBudget(category: String, budget: Double?) {
        viewModelScope.launch {
            if (budget != null) {
                budgetRepository.setCategoryBudget(category, budget)
            } else {
                budgetRepository.removeCategoryBudget(category)
            }
        }
    }
}
