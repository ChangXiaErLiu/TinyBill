package com.tinybill.service.extractor

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.tinybill.data.entity.Transaction
import com.tinybill.util.CategoryKeywordManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class CategoryClassifierTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockManager = mockk<CategoryKeywordManager>()

    private val classifier = CategoryClassifier(mockManager)

    @Test
    fun `classify returns matched category from manager`() {
        every { mockManager.getMerchantCategory("美团外卖", Transaction.TYPE_EXPENSE) } returns Transaction.CATEGORY_FOOD

        val result = classifier.classify("美团外卖", Transaction.TYPE_EXPENSE)
        assertThat(result).isEqualTo(Transaction.CATEGORY_FOOD)
    }

    @Test
    fun `classify returns OTHER expense category when no match`() {
        every { mockManager.getMerchantCategory(any(), Transaction.TYPE_EXPENSE) } returns null

        val result = classifier.classify("未知商家", Transaction.TYPE_EXPENSE)
        assertThat(result).isEqualTo(Transaction.CATEGORY_OTHER)
    }

    @Test
    fun `classify returns OTHER_INCOME when no match for income type`() {
        every { mockManager.getMerchantCategory(any(), Transaction.TYPE_INCOME) } returns null

        val result = classifier.classify("未知来源", Transaction.TYPE_INCOME)
        assertThat(result).isEqualTo(Transaction.CATEGORY_OTHER_INCOME)
    }
}
