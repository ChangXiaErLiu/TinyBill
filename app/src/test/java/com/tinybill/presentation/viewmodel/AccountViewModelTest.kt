package com.tinybill.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountType
import com.tinybill.domain.model.Result
import com.tinybill.domain.repository.IAccountRepository
import com.tinybill.domain.usecase.account.AddAccountUseCase
import com.tinybill.domain.usecase.account.GetAccountSummaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @Mock
    private lateinit var addAccountUseCase: AddAccountUseCase

    @Mock
    private lateinit var getAccountSummaryUseCase: GetAccountSummaryUseCase

    @Mock
    private lateinit var repository: IAccountRepository

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AccountViewModel

    private val sampleAccounts = listOf(
        Account(1, "现金", AccountType.CASH, 0.0, 1000.0),
        Account(2, "支付宝", AccountType.E_WALLET, 0.0, 500.0)
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(repository.getAllAccounts()).thenReturn(flowOf(sampleAccounts))
        whenever(getAccountSummaryUseCase()).thenReturn(
            GetAccountSummaryUseCase.AccountSummary(
                totalAssets = 1500.0,
                totalLiabilities = 200.0,
                netWorth = 1300.0
            )
        )

        viewModel = AccountViewModel(
            addAccountUseCase = addAccountUseCase,
            getAccountSummaryUseCase = getAccountSummaryUseCase,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- 初始化状态 ----------

    @Test
    fun `init loads accounts into Success state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AccountViewModel.AccountUiState.Success::class.java)
        val successState = state as AccountViewModel.AccountUiState.Success
        assertThat(successState.accounts).hasSize(2)
        assertThat(successState.accounts[0].name).isEqualTo("现金")
        assertThat(successState.accounts[1].name).isEqualTo("支付宝")
    }

    @Test
    fun `init loads summary`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.summary.value
        assertThat(summary).isNotNull()
        assertThat(summary!!.totalAssets).isEqualTo(1500.0)
        assertThat(summary.totalLiabilities).isEqualTo(200.0)
        assertThat(summary.netWorth).isEqualTo(1300.0)
    }

    // ---------- 添加账户 ----------

    @Test
    fun `add account success emits ShowMessage`() = runTest(testDispatcher) {
        val newAccount = Account(name = "新卡", type = AccountType.BANK_CARD, initialBalance = 0.0, currentBalance = 0.0)
        whenever(addAccountUseCase(any())).thenReturn(Result.Success(3L))

        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(AccountViewModel.AccountEvent.ShowMessage::class.java)
            assertThat((event as AccountViewModel.AccountEvent.ShowMessage).message).contains("添加成功")
        }

        viewModel.onEvent(AccountViewModel.AccountUserEvent.OnAddAccount(newAccount))
        testDispatcher.scheduler.advanceUntilIdle()

        eventJob.cancel()
    }

    @Test
    fun `add account with blank name returns error`() = runTest(testDispatcher) {
        val blankAccount = Account(name = "", type = AccountType.OTHER, initialBalance = 0.0, currentBalance = 0.0)
        whenever(addAccountUseCase(any())).thenReturn(
            Result.Error(com.tinybill.domain.model.AppException.ValidationException("name", "账户名称不能为空"))
        )

        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(AccountViewModel.AccountEvent.ShowError::class.java)
        }

        viewModel.onEvent(AccountViewModel.AccountUserEvent.OnAddAccount(blankAccount))
        testDispatcher.scheduler.advanceUntilIdle()

        eventJob.cancel()
    }

    // ---------- 更新账户 ----------

    @Test
    fun `update account calls repository and emits ShowMessage`() = runTest(testDispatcher) {
        val updatedAccount = sampleAccounts[0].copy(name = "备用金")

        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(AccountViewModel.AccountEvent.ShowMessage::class.java)
            assertThat((event as AccountViewModel.AccountEvent.ShowMessage).message).contains("更新成功")
        }

        viewModel.onEvent(AccountViewModel.AccountUserEvent.OnUpdateAccount(updatedAccount))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).update(updatedAccount)
        eventJob.cancel()
    }

    // ---------- 删除账户 ----------

    @Test
    fun `delete account calls repository and emits ShowMessage`() = runTest(testDispatcher) {
        val accountToDelete = sampleAccounts[0]

        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(AccountViewModel.AccountEvent.ShowMessage::class.java)
            assertThat((event as AccountViewModel.AccountEvent.ShowMessage).message).contains("删除")
        }

        viewModel.onEvent(AccountViewModel.AccountUserEvent.OnDeleteAccount(accountToDelete))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).delete(accountToDelete)
        eventJob.cancel()
    }

    // ---------- 刷新 ----------

    @Test
    fun `refresh reloads accounts and summary`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(AccountViewModel.AccountUserEvent.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // After refresh, state should still be Success
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AccountViewModel.AccountUiState.Success::class.java)
    }

    // ---------- 各种状态 ----------

    @Test
    fun `state and summary are independent`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(AccountViewModel.AccountUiState.Success::class.java)
        assertThat(viewModel.summary.value).isNotNull()
    }
}
