package com.tinybill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result
import com.tinybill.domain.usecase.transaction.AddTransactionUseCase
import com.tinybill.domain.usecase.transaction.SearchTransactionsUseCase
import com.tinybill.domain.usecase.transaction.UpdateTransactionUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主页账单列表 + 搜索的 ViewModel。
 *
 * UseCase 使用原则：
 * - 透传（Delete/Restore/Search 防抖除外）→ 直接用 Repository
 * - 有业务规则 → 保留 UseCase
 *
 * 当前保留的 UseCase：
 * - AddTransactionUseCase（验证 + 去重）
 * - UpdateTransactionUseCase（验证）
 * - SearchTransactionsUseCase（debounce + flatMapLatest）
 */
@OptIn(FlowPreview::class)
class TransactionListViewModel(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val repository: TransactionRepository,
) : ViewModel() {

    fun getRepository(): TransactionRepository = repository

    // UI State
    private val _uiState = MutableStateFlow<TransactionListUiState>(TransactionListUiState.Loading)
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    // Search
    private val _searchQuery = MutableSharedFlow<String>(replay = 1)
    val searchQuery: Flow<String> = _searchQuery

    val searchResults: StateFlow<List<Transaction>> = searchTransactionsUseCase(_searchQuery)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Events
    private val _events = MutableSharedFlow<TransactionListEvent>()
    val events: Flow<TransactionListEvent> = _events

    init {
        observeTransactions()
    }

    /**
     * 订阅所有账单的 Flow，自动推动 UiState 变化。
     * 直接调 Repository（取代已删除的 GetTransactionsUseCase）。
     */
    private fun observeTransactions() {
        viewModelScope.launch {
            repository.allTransactions
                .catch { e -> _uiState.value = TransactionListUiState.Error(e.message ?: "加载失败") }
                .collect { transactions ->
                    _uiState.value = TransactionListUiState.Success(transactions)
                }
        }
    }

    fun onEvent(event: TransactionListUserEvent) {
        when (event) {
            is TransactionListUserEvent.OnSearchQueryChange -> {
                viewModelScope.launch { _searchQuery.emit(event.query) }
            }
            is TransactionListUserEvent.OnDeleteTransaction -> {
                deleteTransaction(event.id)
            }
            is TransactionListUserEvent.OnRestoreTransaction -> {
                restoreTransaction(event.id)
            }
            is TransactionListUserEvent.OnRefresh -> {
                observeTransactions()
            }
            is TransactionListUserEvent.OnAddTransaction -> {
                addTransaction(event.transaction)
            }
            is TransactionListUserEvent.OnUpdateTransaction -> {
                updateTransaction(event.transaction)
            }
        }
    }

    private fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (val result = addTransactionUseCase(transaction)) {
                is Result.Success -> _events.emit(TransactionListEvent.ShowMessage("保存成功"))
                is Result.Error -> _events.emit(TransactionListEvent.ShowError(result.exception.message ?: "保存失败"))
            }
        }
    }

    private fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (val result = updateTransactionUseCase(transaction)) {
                is Result.Success -> _events.emit(TransactionListEvent.ShowMessage("更新成功"))
                is Result.Error -> _events.emit(TransactionListEvent.ShowError(result.exception.message ?: "更新失败"))
            }
        }
    }

    /**
     * 直接调 Repository.softDelete（取代已删除的 DeleteTransactionUseCase）。
     * ViewModel 自己 try/catch，错误经 events 推给 UI。
     */
    private fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                repository.softDelete(id)
                _events.emit(TransactionListEvent.ShowUndoMessage(id))
            } catch (e: Exception) {
                _events.emit(TransactionListEvent.ShowError(AppException.DatabaseException(e.message ?: "删除失败").message ?: "删除失败"))
            }
        }
    }

    /**
     * 直接调 Repository.restore（取代已删除的 RestoreTransactionUseCase）。
     */
    private fun restoreTransaction(id: Long) {
        viewModelScope.launch {
            try {
                repository.restore(id)
                _events.emit(TransactionListEvent.ShowMessage("已恢复"))
            } catch (e: Exception) {
                _events.emit(TransactionListEvent.ShowError(AppException.DatabaseException(e.message ?: "恢复失败").message ?: "恢复失败"))
            }
        }
    }

    sealed class TransactionListUiState {
        data object Loading : TransactionListUiState()
        data class Success(val transactions: List<Transaction>) : TransactionListUiState()
        data class Error(val message: String) : TransactionListUiState()
    }

    sealed class TransactionListUserEvent {
        data class OnSearchQueryChange(val query: String) : TransactionListUserEvent()
        data class OnDeleteTransaction(val id: Long) : TransactionListUserEvent()
        data class OnRestoreTransaction(val id: Long) : TransactionListUserEvent()
        data object OnRefresh : TransactionListUserEvent()
        data class OnAddTransaction(val transaction: Transaction) : TransactionListUserEvent()
        data class OnUpdateTransaction(val transaction: Transaction) : TransactionListUserEvent()
    }

    sealed class TransactionListEvent {
        data class ShowMessage(val message: String) : TransactionListEvent()
        data class ShowError(val message: String) : TransactionListEvent()
        data class ShowUndoMessage(val transactionId: Long) : TransactionListEvent()
    }
}
