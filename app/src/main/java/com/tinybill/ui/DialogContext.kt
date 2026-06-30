package com.tinybill.ui

import androidx.compose.material3.SnackbarHostState
import com.tinybill.data.entity.Transaction
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * 渲染 Dialog 时共享的上下文。
 *
 * 把 DialogHost 里散落的 5 个参数（appStateManager/viewModel/shellState/...）
 * 收敛成单个 data class，让 DialogRegistry 的 entry lambda 只接收这一个对象。
 *
 * 不引入新概念：纯机械地把 DialogHost 的入参打包。
 */
data class DialogContext(
    val appStateManager: AppStateManager,
    val viewModel: TransactionListViewModel,
    val shellState: AppShellState,
    val snackbarHostState: SnackbarHostState,
    val transactions: List<Transaction>,
    val coroutineScope: CoroutineScope,
)
