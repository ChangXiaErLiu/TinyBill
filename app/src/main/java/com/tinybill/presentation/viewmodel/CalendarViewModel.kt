package com.tinybill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarState(
    val currentYear: Int = 0,
    val currentMonth: Int = 0,
    val selectedDay: Int? = null,
    val viewMode: ViewMode = ViewMode.MONTH,
    val weekOffset: Int = 0,

    val datesWithTransactions: Set<Int> = emptySet(),
    val dailyExpenses: Map<Int, Double> = emptyMap(),
    val dailyIncomes: Map<Int, Double> = emptyMap(),

    val transactionsForSelectedDate: List<Transaction> = emptyList(),

    val monthTotalExpense: Double = 0.0,
    val monthTotalIncome: Double = 0.0,
    val monthBudget: Double? = null,
    val monthBudgetUsed: Double = 0.0,

    val weekTotalExpense: Double = 0.0,
    val weekTotalIncome: Double = 0.0,
    val weekBudget: Double? = null,
    val weekTransactions: List<Transaction> = emptyList(),
    val weekDailyTotals: Map<Int, Double> = emptyMap(),

    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null,
    val isExpanded: Boolean = false
) {
    val monthBalance: Double get() = monthTotalIncome - monthTotalExpense

    val monthBudgetProgress: Float get() {
        val budget = monthBudget ?: return 0f
        if (budget <= 0) return 0f
        return (monthBudgetUsed / budget).toFloat().coerceIn(0f, 1.5f)
    }

    val weekBudgetProgress: Float get() {
        val budget = weekBudget ?: return 0f
        if (budget <= 0) return 0f
        return (weekTotalExpense / budget).toFloat().coerceIn(0f, 1.5f)
    }

    val selectedDateTransactions: List<Transaction>
        get() {
            val filtered = if (selectedCategory != null) {
                transactionsForSelectedDate.filter { it.category == selectedCategory }
            } else {
                transactionsForSelectedDate
            }
            return if (transactionsForSelectedDate.any { it.type == Transaction.TYPE_INCOME }) {
                filtered.sortedByDescending { it.type == Transaction.TYPE_INCOME }
            } else {
                filtered.sortedByDescending { it.timestamp }
            }
        }

    val filteredExpense: Double
        get() = selectedDateTransactions
            .filter { it.type == Transaction.TYPE_EXPENSE }
            .sumOf { it.amount }

    val filteredIncome: Double
        get() = selectedDateTransactions
            .filter { it.type == Transaction.TYPE_INCOME }
            .sumOf { it.amount }

    enum class ViewMode { MONTH, WEEK }
}

sealed interface CalendarUserEvent {
    data class SelectDay(val day: Int) : CalendarUserEvent
    data class ChangeMonth(val year: Int, val month: Int) : CalendarUserEvent
    data class ChangeWeek(val offset: Int) : CalendarUserEvent
    data class SwitchViewMode(val mode: CalendarState.ViewMode) : CalendarUserEvent
    data object GoToToday : CalendarUserEvent
    data class SelectCategory(val category: String?) : CalendarUserEvent
    data class ToggleExpanded(val expanded: Boolean) : CalendarUserEvent
    data object Refresh : CalendarUserEvent
    data class LongPressDay(val day: Int) : CalendarUserEvent
}

sealed interface CalendarEvent {
    data class ShowQuickAdd(val year: Int, val month: Int, val day: Int) : CalendarEvent
    data class ShowError(val message: String) : CalendarEvent
}

class CalendarViewModel(
    private val repository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CalendarEvent>()
    val events = _events.asSharedFlow()

    private val today = Calendar.getInstance()

    init {
        initializeState()
        loadData()
    }

    private fun initializeState() {
        _state.update {
            it.copy(
                currentYear = today.get(Calendar.YEAR),
                currentMonth = today.get(Calendar.MONTH) + 1,
                selectedDay = today.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun onEvent(event: CalendarUserEvent) {
        when (event) {
            is CalendarUserEvent.SelectDay -> selectDay(event.day)
            is CalendarUserEvent.ChangeMonth -> changeMonth(event.year, event.month)
            is CalendarUserEvent.ChangeWeek -> changeWeek(event.offset)
            is CalendarUserEvent.SwitchViewMode -> switchViewMode(event.mode)
            is CalendarUserEvent.GoToToday -> goToToday()
            is CalendarUserEvent.SelectCategory -> selectCategory(event.category)
            is CalendarUserEvent.ToggleExpanded -> toggleExpanded(event.expanded)
            is CalendarUserEvent.Refresh -> loadData()
            is CalendarUserEvent.LongPressDay -> onLongPressDay(event.day)
        }
    }

    private fun selectDay(day: Int) {
        _state.update { it.copy(selectedDay = day, isExpanded = true) }
        viewModelScope.launch {
            loadTransactionsForSelectedDate()
        }
    }

    private fun changeMonth(year: Int, month: Int) {
        _state.update {
            it.copy(
                currentYear = year,
                currentMonth = month,
                selectedDay = null,
                transactionsForSelectedDate = emptyList(),
                selectedCategory = null
            )
        }
        loadData()
    }

    private fun changeWeek(offset: Int) {
        _state.update { it.copy(weekOffset = offset) }
        loadWeekData()
    }

    private fun switchViewMode(mode: CalendarState.ViewMode) {
        _state.update { it.copy(viewMode = mode) }
        if (mode == CalendarState.ViewMode.WEEK) {
            loadWeekData()
        } else {
            loadData()
        }
    }

    private fun goToToday() {
        val now = Calendar.getInstance()
        _state.update {
            it.copy(
                currentYear = now.get(Calendar.YEAR),
                currentMonth = now.get(Calendar.MONTH) + 1,
                selectedDay = now.get(Calendar.DAY_OF_MONTH),
                weekOffset = 0
            )
        }
        loadData()
    }

    private fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }

    private fun toggleExpanded(expanded: Boolean) {
        _state.update { it.copy(isExpanded = expanded) }
    }

    private fun onLongPressDay(day: Int) {
        val state = _state.value
        viewModelScope.launch {
            _events.emit(
                CalendarEvent.ShowQuickAdd(
                    year = state.currentYear,
                    month = state.currentMonth,
                    day = day
                )
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _state.value
                loadMonthData(state.currentYear, state.currentMonth)
                if (state.selectedDay != null) {
                    loadTransactionsForSelectedDate()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun loadMonthData(year: Int, month: Int) {
        val dates = repository.getDatesWithTransactions(year, month)
        val datesWithTransactions = dates.map { it.split("-").last().toInt() }.toSet()

        val dailyExpenseList = repository.getDailyExpenseForMonth(year, month)
        val dailyExpenses = dailyExpenseList.associate {
            it.date.split("-").last().toInt() to it.total
        }

        val dailyIncomeList = repository.getDailyIncomeForMonth(year, month)
        val dailyIncomes = dailyIncomeList.associate {
            it.date.split("-").last().toInt() to it.total
        }

        val monthExpense = repository.getMonthExpense(year, month)
        val monthIncome = repository.getMonthIncome(year, month)

        // 月度预算：先取 budgetEnabled，再根据 mode 选 monthlyBudget 还是 categoryBudgets 合计
        val budgetEnabled = budgetRepository.budgetEnabled.first()
        val budget = if (budgetEnabled) {
            val mode = budgetRepository.budgetMode.first()
            if (mode == BudgetRepository.MODE_CATEGORY_SUM) {
                budgetRepository.categoryBudgets.first().sumOf { it.budget }
            } else {
                budgetRepository.monthlyBudget.first()
            }
        } else null

        val categories = repository.getTransactionsByMonthRange(year, month)
            .map { it.category }
            .distinct()
            .sorted()

        _state.update {
            it.copy(
                datesWithTransactions = datesWithTransactions,
                dailyExpenses = dailyExpenses,
                dailyIncomes = dailyIncomes,
                monthTotalExpense = monthExpense,
                monthTotalIncome = monthIncome,
                monthBudget = budget,
                monthBudgetUsed = monthExpense,
                categories = categories,
                isLoading = false
            )
        }
    }

    private suspend fun loadTransactionsForSelectedDate() {
        val state = _state.value
        val day = state.selectedDay ?: return

        val transactions = repository.getTransactionsByDate(
            state.currentYear,
            state.currentMonth,
            day
        )

        _state.update { it.copy(transactionsForSelectedDate = transactions) }
    }

    private fun loadWeekData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val state = _state.value
                val weekExpense = repository.getWeekExpenseByOffset(state.weekOffset)
                val weekIncome = repository.getWeekIncomeByOffset(state.weekOffset)
                val weekTransactions = repository.getWeekTransactionsByOffset(state.weekOffset)
                val weekDailyTotalsList = repository.getWeekDailyTotalsByOffset(state.weekOffset)

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.add(Calendar.WEEK_OF_YEAR, state.weekOffset)

                val dailyTotals = weekDailyTotalsList.associate { dailyTotal ->
                    val dateParts = dailyTotal.date.split("-")
                    val day = if (dateParts.size >= 3) dateParts[2].toIntOrNull() ?: 0 else 0
                    day to dailyTotal.total
                }

                _state.update {
                    it.copy(
                        weekTotalExpense = weekExpense,
                        weekTotalIncome = weekIncome,
                        weekTransactions = weekTransactions.sortedByDescending { it.timestamp },
                        weekDailyTotals = dailyTotals,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun getWeekDateRange(): Pair<String, String> {
        val state = _state.value
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, state.weekOffset)

        val startCalendar = calendar.clone() as Calendar
        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_MONTH, 6)

        val format = java.text.SimpleDateFormat("M月d日", java.util.Locale.getDefault())
        return format.format(startCalendar.time) to format.format(endCalendar.time)
    }

    fun getCalendarForMonth(): Calendar {
        val state = _state.value
        return Calendar.getInstance().apply {
            set(state.currentYear, state.currentMonth - 1, 1)
        }
    }
}
