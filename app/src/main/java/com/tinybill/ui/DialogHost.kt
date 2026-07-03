package com.tinybill.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.tinybill.data.entity.Budget
import com.tinybill.data.entity.CustomCategory
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.presentation.state.DialogState
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import com.tinybill.service.BillAccessibilityService
import com.tinybill.ui.screen.AACalculatorDialog
import com.tinybill.ui.screen.AddTransactionDialog
import com.tinybill.ui.screen.AppLockDialog
import com.tinybill.ui.screen.BackupDialog
import com.tinybill.ui.screen.BudgetChallengeDialog
import com.tinybill.ui.screen.BudgetSettingsDialog
import com.tinybill.ui.screen.ExportDialog
import com.tinybill.ui.screen.IncomeConfirmDialog
import com.tinybill.ui.screen.SearchDialog
import com.tinybill.ui.screen.TransactionListDialog
import com.tinybill.util.BackupManager
import com.tinybill.util.ExportManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Dialog 分发器。
 *
 * ## 设计：Map 注册表替代 when 分支
 *
 * Before: 12 分支 `when (val state = dialogState)` + 5+ 散落参数
 * After: 一张 `Map<KClass, @Composable (state, ctx) -> Unit>` 注册表
 *
 * 加新 Dialog 不再需要改 DialogHost.kt，只在 [dialogRegistry] 里加 1 条
 * `DialogState.Xxx::class to { state, ctx -> ... }` 即可。
 *
 * 关键的"ctx": [DialogContext] 把 DialogHost 散落的 5 个参数打包成一个对象，
 * 让 entry lambda 签名保持简洁。
 */
@Composable
fun DialogHost(
    appStateManager: AppStateManager,
    viewModel: TransactionListViewModel,
    shellState: AppShellState,
    snackbarHostState: SnackbarHostState,
) {
    val dialogState by appStateManager.dialogState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val transactions = if (uiState is TransactionListViewModel.TransactionListUiState.Success) {
        (uiState as TransactionListViewModel.TransactionListUiState.Success).transactions
    } else {
        emptyList()
    }

    val ctx = DialogContext(
        appStateManager = appStateManager,
        viewModel = viewModel,
        shellState = shellState,
        snackbarHostState = snackbarHostState,
        transactions = transactions,
        coroutineScope = scope,
    )

    // Settings 是 "跳到独立 Screen" 不是 Dialog，不需要 Composable 渲染
    if (dialogState is DialogState.Hidden || dialogState is DialogState.Settings) return

    val entry = dialogRegistry[dialogState::class]
        ?: error("未注册的 DialogState: ${dialogState::class.simpleName}。请在 dialogRegistry 中添加映射。")

    entry(dialogState, ctx)
}

/**
 * 全局 Dialog 注册表。
 *
 * 新增 Dialog 步骤：
 * 1. 在 DialogState.kt 加新的 data class/object
 * 2. 在 AppStateManager 加 showXxx() 便捷方法（可选，仍然推荐）
 * 3. 在下面加一行 `DialogState.Xxx::class to { state, ctx -> ... }`
 *    - `state` 已自动 cast 为具体类型
 *    - `ctx` 提供所有依赖
 *
 * 顺序不重要。相同 KClass 注册两次会抛 IllegalStateException。
 */
private val dialogRegistry: Map<KClass<out DialogState>, @Composable (DialogState, DialogContext) -> Unit> = run {
    val list = mutableListOf<Pair<KClass<out DialogState>, @Composable (DialogState, DialogContext) -> Unit>>()

    list += DialogState.AddTransaction::class to { state, ctx ->
        val s = state as DialogState.AddTransaction
        AddTransactionDialog(
            onDismiss = { ctx.appStateManager.hideDialog() },
            onConfirm = { transaction ->
                if (s.editTransaction != null) {
                    ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnUpdateTransaction(transaction))
                } else {
                    ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnAddTransaction(transaction))
                }
                ctx.appStateManager.hideDialog()
            },
            repository = ctx.viewModel.getRepository(),
            editTransaction = s.editTransaction,
            presetTimestamp = s.getPresetTimestamp(),
        )
    }

    list += DialogState.Search::class to { state, ctx ->
        val s = state as DialogState.Search
        SearchDialog(
            initialKeyword = s.keyword,
            transactions = ctx.transactions,
            onDismiss = { ctx.appStateManager.hideDialog() },
            onSearch = { keyword ->
                ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnSearchQueryChange(keyword))
            },
            onTransactionClick = { transaction ->
                ctx.appStateManager.selectTransaction(transaction)
                ctx.appStateManager.showAddDialog(transaction)
            },
        )
    }

    list += DialogState.Export::class to { _, ctx ->
        val context = LocalContext.current
        ExportDialog(
            onDismiss = { ctx.appStateManager.hideDialog() },
            transactionCount = ctx.transactions.size,
            onExport = { options ->
                ctx.coroutineScope.launch {
                    try {
                        val result = ExportManager.exportTransactions(context, ctx.transactions, options)
                        showSnackbar(ctx, "导出成功: ${result.recordCount} 条记录")
                    } catch (e: Exception) {
                        showSnackbar(ctx, "导出失败: ${e.message}")
                    }
                }
            },
        )
    }

    list += DialogState.Backup::class to { _, ctx ->
        val context = LocalContext.current
        BackupDialog(
            onDismiss = { ctx.appStateManager.hideDialog() },
            onBackup = {
                ctx.coroutineScope.launch {
                    try {
                        val budgets = ctx.shellState.budgetRepository.categoryBudgets.first().map {
                            Budget(
                                category = it.category,
                                monthlyLimit = it.budget,
                                enabled = true,
                                alertThreshold = 0.8f,
                            )
                        }
                        val customCategories = ctx.shellState.customCategoryRepository.categories.first().map {
                            CustomCategory(
                                id = 0,
                                name = it.name,
                                icon = it.icon,
                                color = it.color.value.toLong(),
                                type = it.type,
                                isDefault = 0,
                            )
                        }
                        val result = BackupManager.createBackup(
                            context = context,
                            transactions = ctx.transactions,
                            budgets = budgets,
                            templates = emptyList(),
                            customCategories = customCategories,
                        )
                        showSnackbar(ctx, if (result.success) "备份成功" else "备份失败")
                    } catch (e: Exception) {
                        showSnackbar(ctx, "备份失败: ${e.message}")
                    }
                }
            },
            onRestore = { filePath ->
                ctx.coroutineScope.launch {
                    try {
                        val result = BackupManager.restoreBackup(context, filePath)
                        if (result.success) {
                            showSnackbar(ctx, "恢复成功，请重启应用")
                        } else {
                            showSnackbar(ctx, "恢复失败: ${result.errorMessage}")
                        }
                    } catch (e: Exception) {
                        showSnackbar(ctx, "恢复失败: ${e.message}")
                    }
                }
            },
        )
    }

    list += DialogState.AppLock::class to { _, ctx ->
        AppLockDialog(
            onDismiss = { ctx.appStateManager.hideDialog() },
            onSetupComplete = { ctx.appStateManager.hideDialog() },
        )
    }

    list += DialogState.AACalculator::class to { _, ctx ->
        AACalculatorDialog(
            onDismiss = { ctx.appStateManager.hideDialog() },
        )
    }

    list += DialogState.ExpenseList::class to { state, ctx ->
        val s = state as DialogState.ExpenseList
        TransactionListDialog(
            title = "支出明细",
            transactions = s.transactions,
            onTransactionClick = { transaction ->
                ctx.appStateManager.selectTransaction(transaction)
                ctx.appStateManager.showAddDialog(transaction)
                ctx.appStateManager.hideDialog()
            },
            onDeleteTransaction = { transaction ->
                ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnDeleteTransaction(transaction.id))
                showSnackbar(ctx, "已删除")
            },
            onDismiss = { ctx.appStateManager.hideDialog() },
        )
    }

    list += DialogState.IncomeList::class to { state, ctx ->
        val s = state as DialogState.IncomeList
        TransactionListDialog(
            title = "收入明细",
            transactions = s.transactions,
            onTransactionClick = { transaction ->
                ctx.appStateManager.selectTransaction(transaction)
                ctx.appStateManager.showAddDialog(transaction)
                ctx.appStateManager.hideDialog()
            },
            onDeleteTransaction = { transaction ->
                ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnDeleteTransaction(transaction.id))
                showSnackbar(ctx, "已删除")
            },
            onDismiss = { ctx.appStateManager.hideDialog() },
        )
    }

    list += DialogState.IncomeConfirm::class to { state, ctx ->
        val s = state as DialogState.IncomeConfirm
        IncomeConfirmDialog(
            transaction = s.transaction,
            onDismiss = {
                BillAccessibilityService.clearPendingIncomeTransaction()
                ctx.appStateManager.hideDialog()
            },
            onView = {
                BillAccessibilityService.clearPendingIncomeTransaction()
                ctx.appStateManager.hideDialog()
                ctx.appStateManager.selectTransaction(s.transaction)
                ctx.appStateManager.showAddDialog(s.transaction)
            },
            onEdit = {
                BillAccessibilityService.clearPendingIncomeTransaction()
                ctx.appStateManager.hideDialog()
                ctx.appStateManager.selectTransaction(s.transaction)
                ctx.appStateManager.showAddDialog(s.transaction)
            },
            onIgnore = {
                BillAccessibilityService.clearPendingIncomeTransaction()
                ctx.coroutineScope.launch {
                    ctx.viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnDeleteTransaction(s.transaction.id))
                }
                ctx.appStateManager.hideDialog()
                showSnackbar(ctx, "已忽略")
            },
        )
    }

    list += DialogState.BudgetSettings::class to { _, ctx ->
        BudgetSettingsDialog(
            budgetRepository = ctx.shellState.budgetRepository,
            onDismiss = { ctx.appStateManager.hideDialog() },
        )
    }

    list += DialogState.BudgetChallenge::class to { _, ctx ->
        BudgetChallengeDialog(
            challengeManager = ctx.shellState.budgetChallengeManager,
            categoryBudgets = ctx.shellState.budgetRepository.categoryBudgets.collectAsState(
                initial = emptyList()
            ).value,
            onDismiss = { ctx.appStateManager.hideDialog() },
        )
    }

    // 重复检测：相同 KClass 不应注册两次
    val map = mutableMapOf<KClass<out DialogState>, @Composable (DialogState, DialogContext) -> Unit>()
    for ((k, v) in list) {
        check(k !in map) { "DialogState 重复注册: ${k.simpleName}" }
        map[k] = v
    }
    map.toMap()
}

private fun showSnackbar(ctx: DialogContext, message: String) {
    ctx.coroutineScope.launch {
        ctx.snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
    }
}
