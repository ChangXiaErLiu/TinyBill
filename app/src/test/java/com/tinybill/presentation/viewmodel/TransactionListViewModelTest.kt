package com.tinybill.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result
import com.tinybill.domain.usecase.transaction.AddTransactionUseCase
import com.tinybill.domain.usecase.transaction.SearchTransactionsUseCase
import com.tinybill.domain.usecase.transaction.UpdateTransactionUseCase
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
class TransactionListViewModelTest {

    @Mock
    private lateinit var addTransactionUseCase: AddTransactionUseCase

    @Mock
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase

    @Mock
    private lateinit var searchTransactionsUseCase: SearchTransactionsUseCase

    @Mock
    private lateinit var repository: TransactionRepository

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TransactionListViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(repository.allTransactions).thenReturn(
            flowOf(
                listOf(
                    Transaction(1, 100.0, "美团", "餐饮", System.currentTimeMillis()),
                    Transaction(2, 200.0, "滴滴", "交通", System.currentTimeMillis())
                )
            )
        )

        viewModel = TransactionListViewModel(
            addTransactionUseCase = addTransactionUseCase,
            updateTransactionUseCase = updateTransactionUseCase,
            searchTransactionsUseCase = searchTransactionsUseCase,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- UI State ----------

    @Test
    fun `init loads transactions and emits Success state`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TransactionListViewModel.TransactionListUiState.Success::class.java)
        val successState = state as TransactionListViewModel.TransactionListUiState.Success
        assertThat(successState.transactions).hasSize(2)
    }

    @Test
    fun `add transaction success emits ShowMessage event`() = runTest(testDispatcher) {
        val transaction = Transaction(
            amount = 50.0,
            merchant = "全家",
            category = "购物",
            timestamp = System.currentTimeMillis()
        )
        whenever(addTransactionUseCase(any())).thenReturn(Result.Success(3L))

        // 启动事件收集
        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(TransactionListViewModel.TransactionListEvent.ShowMessage::class.java)
            assertThat((event as TransactionListViewModel.TransactionListEvent.ShowMessage).message).isEqualTo("保存成功")
        }

        viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnAddTransaction(transaction))
        testDispatcher.scheduler.advanceUntilIdle()

        eventJob.cancel()
    }

    @Test
    fun `add transaction failure emits ShowError event`() = runTest(testDispatcher) {
        val transaction = Transaction(
            amount = 0.0,
            merchant = "",
            category = "其他",
            timestamp = System.currentTimeMillis()
        )
        whenever(addTransactionUseCase(any())).thenReturn(
            Result.Error(AppException.ValidationException("amount", "金额必须大于0"))
        )

        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(TransactionListViewModel.TransactionListEvent.ShowError::class.java)
        }

        viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnAddTransaction(transaction))
        testDispatcher.scheduler.advanceUntilIdle()

        eventJob.cancel()
    }

    // ---------- 软删除 ----------

    @Test
    fun `delete transaction calls repository and emits ShowUndoMessage`() = runTest(testDispatcher) {
        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(TransactionListViewModel.TransactionListEvent.ShowUndoMessage::class.java)
            val undoEvent = event as TransactionListViewModel.TransactionListEvent.ShowUndoMessage
            assertThat(undoEvent.transactionId).isEqualTo(1L)
        }

        viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnDeleteTransaction(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).softDelete(1L)
        eventJob.cancel()
    }

    // ---------- 恢复 ----------

    @Test
    fun `restore transaction calls repository and emits ShowMessage`() = runTest(testDispatcher) {
        val eventJob = launch {
            val event = viewModel.events.first()
            assertThat(event).isInstanceOf(TransactionListViewModel.TransactionListEvent.ShowMessage::class.java)
            assertThat((event as TransactionListViewModel.TransactionListEvent.ShowMessage).message).isEqualTo("已恢复")
        }

        viewModel.onEvent(TransactionListViewModel.TransactionListUserEvent.OnRestoreTransaction(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).restore(1L)
        eventJob.cancel()
    }

    // ---------- 获取 Repository 引用 ----------

    @Test
    fun `getRepository returns injected repository`() {
        val repo = viewModel.getRepository()
        assertThat(repo).isSameInstanceAs(repository)
    }
}
