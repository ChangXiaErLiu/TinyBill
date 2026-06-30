package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.presentation.viewmodel.StatisticsUserEvent
import com.tinybill.presentation.viewmodel.StatisticsViewModel
import com.tinybill.ui.components.BarChartWithIncome
import com.tinybill.ui.components.CategoryBudgetDialog
import com.tinybill.ui.components.ChartData
import com.tinybill.ui.components.LineChart
import com.tinybill.ui.components.PieChart
import com.tinybill.ui.components.TrendData
import com.tinybill.ui.components.getCategoryIconAndColor
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.SuccessColor
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 统计页 UI 壳。
 *
 * 拆分后职责：
 *  - ViewModel（[StatisticsViewModel]）：状态聚合 + 数据加载 + 事件分发
 *  - 组件（[StatisticsComponents]）：SummaryCard / CategoryProgressItem / 网格
 *  - 这里只负责：Scaffold + LazyColumn 编排 + 把事件透传给 ViewModel
 *
 * 整个文件从 700 行瘦身到 ~400 行（含本注释）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onTransactionClick: (Transaction) -> Unit = {},
    onShowExpenseList: (List<Transaction>) -> Unit = {},
    onShowIncomeList: (List<Transaction>) -> Unit = {},
    onCategoryClick: (String, Boolean, Int, Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab 切换
            item { StatisticsTabRow(state.selectedTab) { viewModel.onEvent(StatisticsUserEvent.SelectTab(it)) } }

            // 月份选择器
            if (state.selectedTab == 0) {
                item {
                    MonthNavigator(
                        year = state.currentYear,
                        month = state.currentMonth,
                        onPrev = { viewModel.onEvent(StatisticsUserEvent.PrevMonth) },
                        onNext = { viewModel.onEvent(StatisticsUserEvent.NextMonth) }
                    )
                }
            }

            // 支出 / 收入汇总
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "支出",
                        amount = if (state.selectedTab == 0) state.monthExpense else state.yearExpense,
                        color = ErrorColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val list = viewModel.loadTransactions(
                                    isExpense = true,
                                    year = state.currentYear,
                                    month = state.currentMonth,
                                    isYear = state.selectedTab == 1
                                )
                                onShowExpenseList(list)
                            }
                        }
                    )
                    SummaryCard(
                        title = "收入",
                        amount = if (state.selectedTab == 0) state.monthIncome else state.yearIncome,
                        color = SuccessColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val list = viewModel.loadTransactions(
                                    isExpense = false,
                                    year = state.currentYear,
                                    month = state.currentMonth,
                                    isYear = state.selectedTab == 1
                                )
                                onShowIncomeList(list)
                            }
                        }
                    )
                }
            }

            // 结余
            item { NetBalanceCard(netAmount = state.netAmount) }

            // 分类预算
            item {
                Text(
                    text = "分类预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                if (state.budgetProgressList.isNotEmpty()) {
                    CategoryBudgetGrid(
                        budgetProgressList = state.budgetProgressList,
                        onCategoryClick = { cat ->
                            selectedCategoryForBudget = cat
                            showCategoryBudgetDialog = true
                        },
                        onCategoryLongClick = { cat ->
                            selectedCategoryForBudget = cat
                            showCategoryBudgetDialog = true
                        }
                    )
                } else {
                    EmptyCategoryBudget()
                }
            }

            // 月度支出趋势（仅本月显示）
            if (state.selectedTab == 0 && state.monthlyTrend.isNotEmpty()) {
                item {
                    Text(
                        text = "支出趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LineChart(
                            data = state.monthlyTrend.map { (label, expense) ->
                                TrendData(
                                    date = label.replaceFirst(".*?(\\d+)月".toRegex(), "$1"),
                                    expense = expense
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }

            // 本月图表区（仅本月显示）
            if (state.selectedTab == 0 && !state.isLoading) {
                item { MonthlyChartsSection(state, onCategoryClick) }
            }

            // 分类明细标题 + 切换
            item { CategoryListHeader(state.showExpense) { viewModel.onEvent(StatisticsUserEvent.SwitchType(it)) } }

            // 分类明细列表
            val categoryList = if (state.showExpense) state.monthCategoryList else state.monthIncomeCategoryList
            if (categoryList.isNotEmpty()) {
                items(categoryList) { categoryTotal ->
                    val categoryBudget = state.categoryBudgets
                        .find { it.category == categoryTotal.category }?.budget
                    CategoryProgressItem(
                        categoryTotal = categoryTotal,
                        total = if (state.showExpense) state.monthExpense else state.monthIncome,
                        categoryBudget = categoryBudget,
                        onClick = {
                            onCategoryClick(
                                categoryTotal.category,
                                state.showExpense,
                                state.currentYear,
                                state.currentMonth
                            )
                        },
                        onLongClick = {
                            selectedCategoryForBudget = categoryTotal.category
                            showCategoryBudgetDialog = true
                        }
                    )
                }
            } else if (!state.isLoading) {
                item { EmptyTransactions() }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showCategoryBudgetDialog) {
        val currentBudget = state.categoryBudgets
            .find { it.category == selectedCategoryForBudget }?.budget
        CategoryBudgetDialog(
            category = selectedCategoryForBudget,
            currentBudget = currentBudget,
            onDismiss = { showCategoryBudgetDialog = false },
            onConfirm = { newBudget: Double? ->
                viewModel.setCategoryBudget(selectedCategoryForBudget, newBudget)
            }
        )
    }
}

// -------------------- 子区块 --------------------

@Composable
private fun StatisticsTabRow(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("本月", "本年")
    TabRow(
        selectedTabIndex = selected,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = PrimaryGreen
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun MonthNavigator(
    year: Int,
    month: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun NetBalanceCard(netAmount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (netAmount >= 0) SuccessColor.copy(alpha = 0.1f) else ErrorColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "结余",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${if (netAmount >= 0) "+" else ""}¥${String.format("%.2f", netAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (netAmount >= 0) SuccessColor else ErrorColor
            )
        }
    }
}

@Composable
private fun MonthlyChartsSection(
    state: com.tinybill.presentation.viewmodel.StatisticsState,
    onCategoryClick: (String, Boolean, Int, Int) -> Unit
) {
    val calendar = remember { java.util.Calendar.getInstance() }
    val year = state.currentYear
    val month = state.currentMonth
    calendar.set(year, month - 1, 1)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    val expenseMap = state.dailyExpenseList.associate {
        it.date.split("-").last().toInt() to it.total
    }
    val incomeMap = state.dailyIncomeList.associate {
        it.date.split("-").last().toInt() to it.total
    }

    val expenseChartData = (1..daysInMonth).map { day ->
        ChartData(
            label = "${day}日",
            value = expenseMap[day] ?: 0.0,
            color = ErrorColor
        )
    }
    val incomeChartData = (1..daysInMonth).map { day ->
        ChartData(
            label = "${day}日",
            value = incomeMap[day] ?: 0.0,
            color = SuccessColor
        )
    }

    val categoryData = state.monthCategoryList.map { ct ->
        val (_, color) = getCategoryIconAndColor(ct.category)
        ChartData(label = ct.category, value = ct.total, color = color)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本月收支",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            BarChartWithIncome(
                expenseData = expenseChartData,
                incomeData = incomeChartData
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "本月分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (categoryData.isNotEmpty()) {
                PieChart(data = categoryData, total = state.monthExpense)
            }
        }
    }
}

@Composable
private fun CategoryListHeader(showExpense: Boolean, onSwitch: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (showExpense) "支出分类明细" else "收入分类明细",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showExpense,
                onClick = { onSwitch(true) },
                label = { Text("支出") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ErrorColor.copy(alpha = 0.2f),
                    selectedLabelColor = ErrorColor
                )
            )
            FilterChip(
                selected = !showExpense,
                onClick = { onSwitch(false) },
                label = { Text("收入") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SuccessColor.copy(alpha = 0.2f),
                    selectedLabelColor = SuccessColor
                )
            )
        }
    }
}
