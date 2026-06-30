package com.tinybill.presentation.state

import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppStateManager {
    
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()
    
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()
    
    private val _isFirstLaunch = MutableStateFlow<Boolean?>(null)
    val isFirstLaunch: StateFlow<Boolean?> = _isFirstLaunch.asStateFlow()
    
    private val _showAccessibilityGuide = MutableStateFlow(false)
    val showAccessibilityGuide: StateFlow<Boolean> = _showAccessibilityGuide.asStateFlow()
    
    // Dialog State Management
    fun showAddDialog(transaction: Transaction? = null) {
        _dialogState.value = DialogState.AddTransaction(transaction)
    }

    fun showAddDialogWithDate(year: Int, month: Int, day: Int) {
        _dialogState.value = DialogState.AddTransaction(
            editTransaction = null,
            presetYear = year,
            presetMonth = month,
            presetDay = day
        )
    }
    
    fun showBudgetSettings() {
        _dialogState.value = DialogState.BudgetSettings
    }
    
    fun showSearchDialog(keyword: String = "") {
        _dialogState.value = DialogState.Search(keyword)
    }
    
    fun showExportDialog(transactions: List<Transaction> = emptyList()) {
        _dialogState.value = DialogState.Export(transactions)
    }
    
    fun showBackupDialog(transactions: List<Transaction> = emptyList()) {
        _dialogState.value = DialogState.Backup(transactions)
    }
    
    fun showAppLockDialog(isSetup: Boolean = true) {
        _dialogState.value = DialogState.AppLock(isSetup)
    }
    
    fun showAACalculator() {
        _dialogState.value = DialogState.AACalculator
    }
    
    fun showExpenseList(transactions: List<Transaction>) {
        _dialogState.value = DialogState.ExpenseList(transactions)
    }
    
    fun showIncomeList(transactions: List<Transaction>) {
        _dialogState.value = DialogState.IncomeList(transactions)
    }

    fun showIncomeConfirm(transaction: Transaction) {
        _dialogState.value = DialogState.IncomeConfirm(transaction)
    }

    fun showBudgetChallenge() {
        _dialogState.value = DialogState.BudgetChallenge
    }

    fun showSettings() {
        _dialogState.value = DialogState.Settings
    }

    fun hideDialog() {
        _dialogState.value = DialogState.Hidden
    }
    
    // Transaction Selection
    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
    }
    
    // First Launch
    fun setFirstLaunch(isFirst: Boolean) {
        _isFirstLaunch.value = isFirst
    }
    
    // Accessibility Guide
    fun setShowAccessibilityGuide(show: Boolean) {
        _showAccessibilityGuide.value = show
    }
}
