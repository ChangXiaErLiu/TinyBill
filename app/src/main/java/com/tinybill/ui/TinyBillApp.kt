package com.tinybill.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.presentation.state.DialogState
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import com.tinybill.service.BillAccessibilityService
import com.tinybill.ui.screen.OnboardingScreen
import com.tinybill.ui.screen.QuickAddScreen
import com.tinybill.util.HapticManager
import com.tinybill.util.PermissionHelper
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * App 入口。
 *
 * 负责：
 * - 生命周期级别的状态：首启、引导、无障碍引导
 * - 跨服务的副作用：震动初始化、收入到账监听
 * - 把子任务委托给 AppShell / DialogHost
 */
@Composable
fun TinyBillAppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appStateManager = remember { AppStateManager() }
    val viewModel: TransactionListViewModel = koinViewModel()
    val shellState = rememberAppShellState()
    val snackbarHostState = remember { SnackbarHostState() }

    val dialogState by appStateManager.dialogState.collectAsState()

    // 引导流程：null=加载中，true=首启，false=已引导
    var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }
    var showAccessibilityGuide by remember { mutableStateOf(false) }

    // 一次性副作用：首启检测
    LaunchedEffect(Unit) {
        shellState.settingsManager.isFirstLaunch.collect { isFirst ->
            if (isFirstLaunch == null) isFirstLaunch = isFirst
        }
    }

    // 副作用：检查是否需要展示无障碍引导
    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch == false) {
            shellState.settingsManager.isAccessibilityGuided.collect { isGuided ->
                if (!isGuided) showAccessibilityGuide = true
            }
        }
    }

    // 副作用：无障碍服务检测到收入时弹出确认（避免与已显示的确认页重复）
    LaunchedEffect(dialogState) {
        BillAccessibilityService.pendingIncomeTransaction.collect { transaction ->
            if (transaction != null && dialogState !is DialogState.IncomeConfirm) {
                appStateManager.showIncomeConfirm(transaction)
            }
        }
    }

    // 副作用：初始化震动管理器
    LaunchedEffect(Unit) {
        HapticManager.init(context)
    }

    if (isFirstLaunch == null) return

    if (isFirstLaunch == true) {
        OnboardingScreen(
            onComplete = {
                scope.launch {
                    shellState.settingsManager.setFirstLaunch(false)
                    shellState.settingsManager.setAccessibilityGuided(true)
                }
                isFirstLaunch = false
                appStateManager.setFirstLaunch(false)
                appStateManager.setShowAccessibilityGuide(true)
            },
            onSkip = {
                scope.launch { shellState.settingsManager.setFirstLaunch(false) }
                isFirstLaunch = false
                appStateManager.setFirstLaunch(false)
            }
        )
        return
    }

    AppShell(
        appStateManager = appStateManager,
        _shellState = shellState,
        snackbarHostState = snackbarHostState,
    )
    DialogHost(
        appStateManager = appStateManager,
        viewModel = viewModel,
        shellState = shellState,
        snackbarHostState = snackbarHostState,
    )

    if (showAccessibilityGuide) {
        com.tinybill.ui.screen.AccessibilityGuideDialog(
            onDismiss = { showAccessibilityGuide = false },
            onOpenSettings = {
                PermissionHelper.openAccessibilitySettings(context)
                scope.launch { shellState.settingsManager.setAccessibilityGuided(true) }
                showAccessibilityGuide = false
            }
        )
    }

    // Widget 快捷记账：从桌面 Widget 点击"记一笔"直接弹出记账
    val pendingAction = com.tinybill.TinyBillApp.pendingQuickAddAction
    if (pendingAction != null && !pendingAction.second) {
        val (isExpense, _) = pendingAction
        com.tinybill.TinyBillApp.pendingQuickAddAction = isExpense to true
        QuickAddScreen(
            onDismiss = { com.tinybill.TinyBillApp.pendingQuickAddAction = null },
            onQuickAdd = { amount, category, isExp ->
                val transaction = com.tinybill.data.entity.Transaction(
                    amount = amount,
                    merchant = if (isExp) "桌面记账" else "桌面收入",
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    type = if (isExp) com.tinybill.data.entity.Transaction.TYPE_EXPENSE
                            else com.tinybill.data.entity.Transaction.TYPE_INCOME,
                    source = com.tinybill.data.entity.Transaction.SOURCE_MANUAL
                )
                viewModel.onEvent(
                    com.tinybill.presentation.viewmodel.TransactionListViewModel
                        .TransactionListUserEvent.OnAddTransaction(transaction)
                )
            }
        )
    }
}
