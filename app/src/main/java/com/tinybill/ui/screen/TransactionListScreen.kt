package com.tinybill.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import com.tinybill.ui.components.*
import com.tinybill.ui.components.designsystem.*
import com.tinybill.ui.theme.SuccessColor
import com.tinybill.util.HapticManager
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = koinViewModel(),
    appStateManager: AppStateManager,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuickAdd by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val budgetRepository = remember { BudgetRepository(context.applicationContext) }

    // 订阅三个 Flow 并 combine 算出"实际生效预算"
    val monthlyBudget by budgetRepository.monthlyBudget.collectAsState(initial = 0.0)
    val budgetMode by budgetRepository.budgetMode.collectAsState(initial = BudgetRepository.MODE_FIXED)
    val categoryBudgets by budgetRepository.categoryBudgets.collectAsState(initial = emptyList())
    val effectiveBudget = remember(monthlyBudget, budgetMode, categoryBudgets) {
        if (budgetMode == BudgetRepository.MODE_CATEGORY_SUM) {
            categoryBudgets.sumOf { it.budget }
        } else {
            monthlyBudget
        }
    }

    // Collect events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionListViewModel.TransactionListEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TransactionListViewModel.TransactionListEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TransactionListViewModel.TransactionListEvent.ShowUndoMessage -> {
                    // Handle undo
                }
            }
        }
    }
    
    // Calculate current month expense using derivedStateOf to avoid recomputation on unrelated changes
    val currentMonthExpense by remember(uiState) {
        derivedStateOf {
            if (uiState is TransactionListViewModel.TransactionListUiState.Success) {
                val transactions = (uiState as TransactionListViewModel.TransactionListUiState.Success).transactions
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                var sum = 0.0
                for (t in transactions) {
                    if (t.type == Transaction.TYPE_EXPENSE && isCurrentMonth(t.timestamp, currentYear, currentMonth)) {
                        sum += t.amount
                    }
                }
                sum
            } else {
                0.0
            }
        }
    }
    
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(pendingDelete) {
        val deleted = pendingDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "已删除",
            actionLabel = "撤销",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            // 撤销删除：调用 ViewModel 的 restore 路径，而不是新增一条
            viewModel.onEvent(
                TransactionListViewModel.TransactionListUserEvent.OnRestoreTransaction(deleted.id)
            )
        }
        pendingDelete = null
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAdd = true },
                containerColor = TinyBillColors.Primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 无障碍权限状态条：未开启时显示，点击跳转到无障碍设置
            AccessibilityStatusBanner()
            Spacer(modifier = Modifier.height(8.dp))

            // 自动记账摘要条：根据 ParserStats 实时反馈"自动记了 N 笔"
            // 与 AccessibilityStatusBanner 是互斥的：未开启无障碍时是黄色警告，
            // 已开启时根据成功率显示绿/黄/红。
            AutoBillSummaryBanner()
            Spacer(modifier = Modifier.height(12.dp))

            // Budget Card
            if (effectiveBudget > 0) {
                BudgetCard(
                    budget = effectiveBudget,
                    spent = currentMonthExpense,
                    onClick = { appStateManager.showBudgetSettings() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Transaction List
            when (val state = uiState) {
                is TransactionListViewModel.TransactionListUiState.Loading -> {
                    TransactionListSkeleton(itemCount = 5)
                }
                is TransactionListViewModel.TransactionListUiState.Success -> {
                    val transactions = state.transactions
                    if (transactions.isEmpty()) {
                        EmptyStateView(
                            title = "还没有账单",
                            description = "点击下方按钮添加第一笔账单\n支付后会自动记录",
                            action = {
                                TinyBillButton(
                                    text = "记一笔",
                                    onClick = { showQuickAdd = true }
                                )
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(
                                items = transactions,
                                key = { it.id }
                            ) { transaction ->
                                val onDelete = remember(transaction.id) {
                                    {
                                        pendingDelete = transaction
                                        viewModel.onEvent(
                                            TransactionListViewModel.TransactionListUserEvent.OnDeleteTransaction(transaction.id)
                                        )
                                    }
                                }
                                val onClick = remember(transaction.id) {
                                    {
                                        HapticManager.performClick()
                                        appStateManager.selectTransaction(transaction)
                                        appStateManager.showAddDialog(transaction)
                                    }
                                }
                                SwipeToDeleteTransactionCard(
                                    transaction = transaction,
                                    onDelete = onDelete,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                }
                is TransactionListViewModel.TransactionListUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = {
                            viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnRefresh)
                        }
                    )
                }
            }
        }
        
        // Quick Add Bottom Sheet
        if (showQuickAdd) {
            QuickAddScreen(
                onDismiss = { showQuickAdd = false },
                onQuickAdd = { amount, category, isExpense ->
                    val transaction = Transaction(
                        amount = amount,
                        merchant = if (isExpense) "手动记账" else "手动收入",
                        category = category,
                        timestamp = System.currentTimeMillis(),
                        type = if (isExpense) Transaction.TYPE_EXPENSE else Transaction.TYPE_INCOME,
                        source = Transaction.SOURCE_MANUAL
                    )
                    viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnAddTransaction(transaction))
                    showQuickAdd = false
                }
            )
        }
    }
}

@Composable
fun BudgetCard(
    budget: Double,
    spent: Double,
    onClick: () -> Unit
) {
    val percentage = if (budget > 0) (spent / budget * 100).coerceIn(0.0, 100.0) else 0.0
    val isOverBudget = spent > budget
    val progressColor = if (isOverBudget) TinyBillColors.Expense else TinyBillColors.Primary
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = progressColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "本月预算",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isOverBudget) "已超支，注意控制" else "剩余 ¥${String.format("%.0f", budget - spent)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverBudget) TinyBillColors.Expense else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar with percentage text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (percentage / 100).toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${String.format("%.0f", percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "已支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${String.format("%.2f", spent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) TinyBillColors.Expense else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "总预算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${String.format("%.2f", budget)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val (categoryIcon, categoryColor) = getCategoryIconAndColor(transaction.category)
    val isExpense = transaction.type == Transaction.TYPE_EXPENSE
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with gradient background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}¥${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) MaterialTheme.colorScheme.error else SuccessColor
                )
                if (transaction.source == Transaction.SOURCE_AUTO) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TinyBillColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "自动",
                            style = MaterialTheme.typography.labelSmall,
                            color = TinyBillColors.Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun isCurrentMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) + 1 == month
}

private fun getCategoryIcon(category: String): String {
    return when (category) {
        Transaction.CATEGORY_FOOD -> "🍔"
        Transaction.CATEGORY_SHOPPING -> "🛍️"
        Transaction.CATEGORY_TRANSPORT -> "🚗"
        Transaction.CATEGORY_ENTERTAINMENT -> "🎮"
        Transaction.CATEGORY_RENT -> "🏠"
        Transaction.CATEGORY_UTILITY -> "💡"
        Transaction.CATEGORY_BEAUTY -> "💄"
        Transaction.CATEGORY_SALARY -> "💰"
        Transaction.CATEGORY_RED_PACKET -> "🧧"
        Transaction.CATEGORY_TRANSFER -> "💸"
        else -> "📦"
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        Transaction.CATEGORY_FOOD -> TinyBillColors.CategoryFood
        Transaction.CATEGORY_SHOPPING -> TinyBillColors.CategoryShopping
        Transaction.CATEGORY_TRANSPORT -> TinyBillColors.CategoryTransport
        Transaction.CATEGORY_ENTERTAINMENT -> TinyBillColors.CategoryEntertainment
        Transaction.CATEGORY_RENT -> TinyBillColors.CategoryHousing
        Transaction.CATEGORY_UTILITY -> TinyBillColors.CategoryUtilities
        else -> TinyBillColors.CategoryOther
    }
}

private fun formatDate(timestamp: Long): String {
    return DateFormatters.SHORT_DATE_TIME.format(Date(timestamp))
}

private object DateFormatters {
    val SHORT_DATE_TIME = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
}
