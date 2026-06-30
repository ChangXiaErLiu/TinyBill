package com.tinybill.domain.usecase.transaction

import com.google.common.truth.Truth.assertThat
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.domain.model.AppException
import com.tinybill.domain.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AddTransactionUseCaseTest {

    @Mock
    private lateinit var repository: TransactionRepository

    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = AddTransactionUseCase(repository)
    }

    // 正向路径

    @Test
    fun `given valid transaction, when invoke, then returns Success`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "美团",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )
        whenever(repository.isDuplicate(any(), any(), any())).thenReturn(false)
        whenever(repository.insert(any())).thenReturn(1L)

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(1L)
        verify(repository).insert(transaction)
    }

    // 验证：金额

    @Test
    fun `given zero amount, when invoke, then returns ValidationError`() = runTest {
        val transaction = Transaction(
            amount = 0.0,
            merchant = "美团",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.ValidationException::class.java)
        assertThat(error.message).contains("金额")
    }

    @Test
    fun `given negative amount, when invoke, then returns ValidationError`() = runTest {
        val transaction = Transaction(
            amount = -50.0,
            merchant = "美团",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.ValidationException::class.java)
    }

    // 验证：商户名

    @Test
    fun `given blank merchant, when invoke, then returns ValidationError`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.ValidationException::class.java)
        assertThat(error.message).contains("商户")
    }

    // 验证：分类

    @Test
    fun `given blank category, when invoke, then returns ValidationError`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "美团",
            category = "",
            timestamp = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.ValidationException::class.java)
    }

    // 去重检测

    @Test
    fun `given duplicate transaction, when invoke, then returns DuplicateError`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "美团",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )
        whenever(repository.isDuplicate(any(), any(), any())).thenReturn(true)

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.DuplicateTransactionException::class.java)
    }

    // 数据库异常

    @Test
    fun `given database error, when invoke, then returns DatabaseError`() = runTest {
        val transaction = Transaction(
            amount = 100.0,
            merchant = "美团",
            category = "餐饮",
            timestamp = System.currentTimeMillis()
        )
        whenever(repository.isDuplicate(any(), any(), any())).thenReturn(false)
        whenever(repository.insert(any())).thenThrow(RuntimeException("DB error"))

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(AppException.DatabaseException::class.java)
    }
}
