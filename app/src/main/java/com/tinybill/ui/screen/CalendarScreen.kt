package com.tinybill.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinybill.data.entity.Transaction
import com.tinybill.presentation.viewmodel.CalendarState
import com.tinybill.presentation.viewmodel.CalendarUserEvent
import com.tinybill.presentation.viewmodel.CalendarViewModel
import com.tinybill.ui.components.TransactionCard
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.SuccessColor
import com.tinybill.ui.theme.WarningColor
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    onTransactionClick: (Transaction) -> Unit,
    onQuickAdd: (year: Int, month: Int, day: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is com.tinybill.presentation.viewmodel.CalendarEvent.ShowQuickAdd -> {
                    onQuickAdd(event.year, event.month, event.day)
                }
                is com.tinybill.presentation.viewmodel.CalendarEvent.ShowError -> {
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                state = state,
                onTodayClick = { viewModel.onEvent(CalendarUserEvent.GoToToday) },
                onViewModeChange = { viewModel.onEvent(CalendarUserEvent.SwitchViewMode(it)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 100) {
                                val current = state.currentMonth
                                val year = if (current == 1) state.currentYear - 1 else state.currentYear
                                val month = if (current == 1) 12 else current - 1
                                viewModel.onEvent(CalendarUserEvent.ChangeMonth(year, month))
                            } else if (dragOffset < -100) {
                                val current = state.currentMonth
                                val year = if (current == 12) state.currentYear + 1 else state.currentYear
                                val month = if (current == 12) 1 else current + 1
                                viewModel.onEvent(CalendarUserEvent.ChangeMonth(year, month))
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
        ) {
            when (state.viewMode) {
                CalendarState.ViewMode.MONTH -> MonthView(
                    state = state,
                    onDayClick = { viewModel.onEvent(CalendarUserEvent.SelectDay(it)) },
                    onDayLongClick = { viewModel.onEvent(CalendarUserEvent.LongPressDay(it)) },
                    onMonthChange = { year, month ->
                        viewModel.onEvent(CalendarUserEvent.ChangeMonth(year, month))
                    },
                    onTransactionClick = onTransactionClick,
                    onCategorySelect = { viewModel.onEvent(CalendarUserEvent.SelectCategory(it)) },
                    onToggleExpand = { viewModel.onEvent(CalendarUserEvent.ToggleExpanded(it)) }
                )
                CalendarState.ViewMode.WEEK -> WeekView(
                    state = state,
                    viewModel = viewModel,
                    onWeekChange = { viewModel.onEvent(CalendarUserEvent.ChangeWeek(it)) },
                    onTransactionClick = onTransactionClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    state: CalendarState,
    onTodayClick: () -> Unit,
    onViewModeChange: (CalendarState.ViewMode) -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "日历",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.currentYear}年${state.currentMonth}月",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onTodayClick) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "今天"
                )
            }

            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onViewModeChange(CalendarState.ViewMode.MONTH) },
                    label = { Text("月") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.viewMode == CalendarState.ViewMode.MONTH)
                            PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent,
                        labelColor = if (state.viewMode == CalendarState.ViewMode.MONTH)
                            PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        borderColor = if (state.viewMode == CalendarState.ViewMode.MONTH)
                            PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        enabled = true
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                AssistChip(
                    onClick = { onViewModeChange(CalendarState.ViewMode.WEEK) },
                    label = { Text("周") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.viewMode == CalendarState.ViewMode.WEEK)
                            PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent,
                        labelColor = if (state.viewMode == CalendarState.ViewMode.WEEK)
                            PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        borderColor = if (state.viewMode == CalendarState.ViewMode.WEEK)
                            PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        enabled = true
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun MonthView(
    state: CalendarState,
    onDayClick: (Int) -> Unit,
    onDayLongClick: (Int) -> Unit,
    onMonthChange: (Int, Int) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onToggleExpand: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            MonthStatisticsCard(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            MonthNavigator(
                year = state.currentYear,
                month = state.currentMonth,
                onPreviousMonth = {
                    val (year, month) = if (state.currentMonth == 1) {
                        state.currentYear - 1 to 12
                    } else {
                        state.currentYear to state.currentMonth - 1
                    }
                    onMonthChange(year, month)
                },
                onNextMonth = {
                    val (year, month) = if (state.currentMonth == 12) {
                        state.currentYear + 1 to 1
                    } else {
                        state.currentYear to state.currentMonth + 1
                    }
                    onMonthChange(year, month)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            EnhancedCalendarGrid(
                year = state.currentYear,
                month = state.currentMonth,
                dailyExpenses = state.dailyExpenses,
                dailyIncomes = state.dailyIncomes,
                selectedDay = state.selectedDay,
                onDayClick = onDayClick,
                onDayLongClick = onDayLongClick,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (state.categories.isNotEmpty()) {
            item {
                CategoryFilterChips(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = onCategorySelect,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        if (state.selectedDay != null) {
            item {
                AnimatedVisibility(
                    visible = state.selectedDay != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DayDetailSection(
                        state = state,
                        onTransactionClick = onTransactionClick,
                        onToggleExpand = onToggleExpand
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthStatisticsCard(
    state: CalendarState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "本月支出",
                    value = state.monthTotalExpense,
                    color = ErrorColor,
                    icon = "📉"
                )
                StatItem(
                    title = "本月收入",
                    value = state.monthTotalIncome,
                    color = SuccessColor,
                    icon = "📈"
                )
                StatItem(
                    title = "本月结余",
                    value = state.monthBalance,
                    color = if (state.monthBalance >= 0) SuccessColor else ErrorColor,
                    icon = "💰"
                )
            }

            if (state.monthBudget != null && state.monthBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                BudgetProgressIndicator(
                    budget = state.monthBudget!!,
                    spent = state.monthBudgetUsed,
                    progress = state.monthBudgetProgress
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: Double,
    color: Color,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // 结余显示正负号，支出和收入只显示绝对值
        val displayValue = if (title == "本月结余") value else kotlin.math.abs(value)
        val prefix = if (title == "本月结余" && value > 0) "+" else ""
        Text(
            text = "¥$prefix${String.format("%.0f", displayValue)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetProgressIndicator(
    budget: Double,
    spent: Double,
    progress: Float
) {
    val remaining = budget - spent
    val progressAnimated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            progress < 0.5f -> SuccessColor
            progress < 0.8f -> WarningColor
            else -> ErrorColor
        },
        animationSpec = tween(300),
        label = "progressColor"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 预算进度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(progressColor.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressAnimated)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (remaining >= 0) "剩余 ¥${String.format("%.0f", remaining)}" else "超支 ¥${String.format("%.0f", -remaining)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (remaining >= 0) SuccessColor else ErrorColor
        )
    }
}

@Composable
private fun MonthNavigator(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月")
        }

        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedCalendarGrid(
    year: Int,
    month: Int,
    dailyExpenses: Map<Int, Double>,
    dailyIncomes: Map<Int, Double>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    onDayLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
    }

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) + 1 == month
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    val maxExpense = dailyExpenses.values.maxOrNull() ?: 0.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - firstDayOfWeek + 1

                    if (dayIndex in 1..daysInMonth) {
                        val isSelected = selectedDay == dayIndex
                        val isToday = isCurrentMonth && todayDay == dayIndex
                        val expense = dailyExpenses[dayIndex] ?: 0.0
                        val income = dailyIncomes[dayIndex] ?: 0.0
                        val hasTransaction = expense > 0 || income > 0

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> PrimaryGreen
                                        hasTransaction -> getHeatmapColor(expense, maxExpense).copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(2.dp, PrimaryGreen, CircleShape)
                                    } else Modifier
                                )
                                .combinedClickable(
                                    onClick = { onDayClick(dayIndex) },
                                    onLongClick = { onDayLongClick(dayIndex) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> PrimaryGreen
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )

                                if (expense > 0 && !isSelected) {
                                    Text(
                                        text = when {
                                            expense >= 1000 -> "${(expense / 1000).toInt()}k"
                                            expense >= 100 -> String.format("%.0f", expense)
                                            else -> String.format("%.0f", expense)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 7.sp,
                                        color = ErrorColor.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HeatmapLegend(maxExpense = maxExpense)
    }
}

@Composable
private fun getHeatmapColor(expense: Double, maxExpense: Double): Color {
    if (expense <= 0 || maxExpense <= 0) return Color.Transparent

    val ratio = (expense / maxExpense).coerceIn(0.0, 1.0)

    return when {
        ratio < 0.2 -> PrimaryGreen.copy(alpha = 0.15f)
        ratio < 0.4 -> PrimaryGreen.copy(alpha = 0.3f)
        ratio < 0.6 -> PrimaryGreen.copy(alpha = 0.45f)
        ratio < 0.8 -> PrimaryGreen.copy(alpha = 0.6f)
        else -> PrimaryGreen.copy(alpha = 0.8f)
    }
}

@Composable
private fun HeatmapLegend(maxExpense: Double) {
    if (maxExpense <= 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "低",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0).forEach { intensity ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (intensity == 0.0) MaterialTheme.colorScheme.surfaceVariant
                            else PrimaryGreen.copy(alpha = intensity.toFloat())
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "高",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryFilterChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "筛选分类",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("全部") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryGreen
                    )
                )
            }

            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun DayDetailSection(
    state: CalendarState,
    onTransactionClick: (Transaction) -> Unit,
    onToggleExpand: (Boolean) -> Unit
) {
    val day = state.selectedDay ?: return
    val dayExpense = state.filteredExpense
    val dayIncome = state.filteredIncome

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onToggleExpand(!state.isExpanded) }
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (state.isExpanded) "▲ ${day}日" else "▼ ${day}日",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "支出 ¥${String.format("%.2f", dayExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor
                        )
                        Text(
                            text = "收入 ¥${String.format("%.2f", dayIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessColor
                        )
                    }
                }

                IconButton(onClick = { onToggleExpand(false) }) {
                    Icon(
                        imageVector = if (state.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (state.isExpanded) "收起" else "展开"
                    )
                }
            }

            AnimatedVisibility(
                visible = state.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    if (state.selectedDateTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        state.selectedDateTransactions.forEach { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "当日无收支记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    state: CalendarState,
    viewModel: CalendarViewModel,
    onWeekChange: (Int) -> Unit,
    onTransactionClick: ((Transaction) -> Unit)? = null
) {
    val (startDate, endDate) = remember(state.weekOffset) {
        viewModel.getWeekDateRange()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onWeekChange(state.weekOffset - 1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一周")
                }

                Text(
                    text = "$startDate - $endDate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = { onWeekChange(state.weekOffset + 1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一周")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeekStatItem(
                            title = "支出",
                            value = state.weekTotalExpense,
                            color = ErrorColor,
                            icon = "📉"
                        )
                        WeekStatItem(
                            title = "收入",
                            value = state.weekTotalIncome,
                            color = SuccessColor,
                            icon = "📈"
                        )
                    }

                    if (state.weekBudget != null && state.weekBudget > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetProgressIndicator(
                            budget = state.weekBudget!!,
                            spent = state.weekTotalExpense,
                            progress = state.weekBudgetProgress
                        )
                    }
                }
            }
        }

        if (state.weekTransactions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "📝 收支明细 (${state.weekTransactions.size}笔)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(state.weekTransactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick?.invoke(transaction) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(48.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "本周暂无收支记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStatItem(
    title: String,
    value: Double,
    color: Color,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "¥${String.format("%.0f", kotlin.math.abs(value))}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
