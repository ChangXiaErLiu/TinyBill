package com.tinybill.data.repository

import com.google.common.truth.Truth.assertThat
import com.tinybill.data.dao.TransactionDao
import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TransactionRepositoryTest {

    @Mock
    private lateinit var transactionDao: TransactionDao

    private lateinit var repository: TransactionRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TransactionRepository(transactionDao)
    }

    @Test
    fun `test getAllTransactions returns flow of transactions`() = runTest {
        val transactions = listOf(
            Transaction(1, 100.0, "美团", "餐饮", System.currentTimeMillis()),
            Transaction(2, 50.0, "超市", "购物", System.currentTimeMillis())
        )
        whenever(transactionDao.getAllTransactions()).thenReturn(flowOf(transactions))

        val result = repository.allTransactions

        result.collect { list ->
            assertThat(list).hasSize(2)
            assertThat(list[0].merchant).isEqualTo("美团")
            assertThat(list[1].merchant).isEqualTo("超市")
        }
    }

    @Test
    fun `test searchTransactions returns matching results`() = runTest {
        val transactions = listOf(
            Transaction(1, 100.0, "美团外卖", "餐饮", System.currentTimeMillis()),
            Transaction(2, 50.0, "美团打车", "交通", System.currentTimeMillis())
        )
        whenever(transactionDao.searchTransactionsOnce("美团")).thenReturn(transactions)

        val result = repository.searchTransactions("美团")

        assertThat(result).hasSize(2)
        assertThat(result.map { it.merchant }).containsExactly("美团外卖", "美团打车")
    }

    @Test
    fun `test searchTransactions with empty keyword returns empty`() = runTest {
        whenever(transactionDao.searchTransactionsOnce("")).thenReturn(emptyList())

        val result = repository.searchTransactions("")

        assertThat(result).isEmpty()
    }

    @Test
    fun `test insert transaction`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "测试商户",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )
        whenever(transactionDao.insert(any())).thenReturn(1L)

        val result = repository.insert(transaction)

        assertThat(result).isEqualTo(1L)
    }

    @Test
    fun `test update transaction`() = runTest {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            merchant = "测试商户",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )

        repository.update(transaction)
    }

    @Test
    fun `test softDelete transaction`() = runTest {
        whenever(transactionDao.softDelete(1L)).then { }

        repository.softDelete(1L)
    }

    @Test
    fun `test restore transaction`() = runTest {
        whenever(transactionDao.restore(1L)).then { }

        repository.restore(1L)
    }
}

class TransactionEntityTest {

    @Test
    fun `test transaction creation with default values`() {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "测试",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )

        assertThat(transaction.id).isEqualTo(0)
        assertThat(transaction.type).isEqualTo(Transaction.TYPE_EXPENSE)
        assertThat(transaction.source).isEqualTo(Transaction.SOURCE_AUTO)
        assertThat(transaction.note).isEqualTo("")
        assertThat(transaction.isDeleted).isEqualTo(0)
    }

    @Test
    fun `test transaction type constants`() {
        assertThat(Transaction.TYPE_EXPENSE).isEqualTo(0)
        assertThat(Transaction.TYPE_INCOME).isEqualTo(1)
    }

    @Test
    fun `test transaction source constants`() {
        assertThat(Transaction.SOURCE_AUTO).isEqualTo(0)
        assertThat(Transaction.SOURCE_MANUAL).isEqualTo(1)
    }

    @Test
    fun `test expense categories`() {
        val expectedCategories = listOf(
            "餐饮", "购物", "房租", "水电", "交通", "娱乐", "美妆", "其他"
        )
        assertThat(Transaction.EXPENSE_CATEGORIES).containsExactlyElementsIn(expectedCategories)
    }

    @Test
    fun `test income categories`() {
        val expectedCategories = listOf(
            "工资", "红包", "转账", "其他收入"
        )
        assertThat(Transaction.INCOME_CATEGORIES).containsExactlyElementsIn(expectedCategories)
    }

    @Test
    fun `test all categories includes both expense and income`() {
        val all = Transaction.ALL_CATEGORIES
        assertThat(all).containsAtLeastElementsIn(Transaction.EXPENSE_CATEGORIES)
        assertThat(all).containsAtLeastElementsIn(Transaction.INCOME_CATEGORIES)
    }
}
