package com.tinybill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinybill.data.entity.Account
import com.tinybill.domain.model.Result
import com.tinybill.domain.repository.IAccountRepository
import com.tinybill.domain.usecase.account.AddAccountUseCase
import com.tinybill.domain.usecase.account.GetAccountSummaryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val addAccountUseCase: AddAccountUseCase,
    private val getAccountSummaryUseCase: GetAccountSummaryUseCase,
    private val repository: IAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccountEvent>()
    val events: MutableSharedFlow<AccountEvent> = _events

    private val _summary = MutableStateFlow<GetAccountSummaryUseCase.AccountSummary?>(null)
    val summary: StateFlow<GetAccountSummaryUseCase.AccountSummary?> = _summary.asStateFlow()

    init {
        loadAccounts()
        loadSummary()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            try {
                repository.getAllAccounts().collect { accounts ->
                    _uiState.value = AccountUiState.Success(accounts)
                }
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            try {
                _summary.value = getAccountSummaryUseCase()
            } catch (e: Exception) {
                // Silent fail for summary
            }
        }
    }

    fun onEvent(event: AccountUserEvent) {
        when (event) {
            is AccountUserEvent.OnAddAccount -> addAccount(event.account)
            is AccountUserEvent.OnUpdateAccount -> updateAccount(event.account)
            is AccountUserEvent.OnDeleteAccount -> deleteAccount(event.account)
            is AccountUserEvent.OnRefresh -> {
                loadAccounts()
                loadSummary()
            }
        }
    }

    private fun addAccount(account: Account) {
        viewModelScope.launch {
            when (val result = addAccountUseCase(account)) {
                is Result.Success -> {
                    _events.emit(AccountEvent.ShowMessage("账户添加成功"))
                    loadSummary()
                }
                is Result.Error -> {
                    _events.emit(AccountEvent.ShowError(result.exception.message ?: "添加失败"))
                }
            }
        }
    }

    private fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.update(account)
                _events.emit(AccountEvent.ShowMessage("账户更新成功"))
                loadSummary()
            } catch (e: Exception) {
                _events.emit(AccountEvent.ShowError(e.message ?: "更新失败"))
            }
        }
    }

    private fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.delete(account)
                _events.emit(AccountEvent.ShowMessage("账户已删除"))
                loadSummary()
            } catch (e: Exception) {
                _events.emit(AccountEvent.ShowError(e.message ?: "删除失败"))
            }
        }
    }

    sealed class AccountUiState {
        data object Loading : AccountUiState()
        data class Success(val accounts: List<Account>) : AccountUiState()
        data class Error(val message: String) : AccountUiState()
    }

    sealed class AccountUserEvent {
        data class OnAddAccount(val account: Account) : AccountUserEvent()
        data class OnUpdateAccount(val account: Account) : AccountUserEvent()
        data class OnDeleteAccount(val account: Account) : AccountUserEvent()
        data object OnRefresh : AccountUserEvent()
    }

    sealed class AccountEvent {
        data class ShowMessage(val message: String) : AccountEvent()
        data class ShowError(val message: String) : AccountEvent()
    }
}
