package com.tinybill.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.regex.Pattern

class AmountExtractionTest {

    private val amountPattern = Pattern.compile("[¥￥]\\s*([\\d,]+\\.?\\d*)")

    @Test
    fun `test amount extraction with yuan symbol`() {
        val testCases = listOf(
            "支付¥100" to 100.0,
            "支付 ¥ 100" to 100.0,
            "¥200" to 200.0,
            "付款¥50.50" to 50.50,
            "支付¥1,000" to 1000.0,
            "¥999.99" to 999.99
        )

        testCases.forEach { (text, expected) ->
            val result = extractAmount(text)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `test amount extraction with full width yuan symbol`() {
        val testCases = listOf(
            "支付￥100" to 100.0,
            "￥200" to 200.0,
            "￥50.50" to 50.50
        )

        testCases.forEach { (text, expected) ->
            val result = extractAmount(text)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `test amount extraction with comma separator`() {
        val testCases = listOf(
            "¥1,000" to 1000.0,
            "¥10,000" to 10000.0,
            "¥1,000,000" to 1000000.0,
            "¥100,000.50" to 100000.50
        )

        testCases.forEach { (text, expected) ->
            val result = extractAmount(text)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `test amount extraction edge cases`() {
        val testCases = listOf(
            "无金额" to null,
            "金额为0" to null,
            "" to null,
            "只含文本" to null,
            "¥abc" to null
        )

        testCases.forEach { (text, expected) ->
            val result = extractAmount(text)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `test amount extraction - multiple amounts returns first`() {
        val text = "原价¥100现价¥80"
        val result = extractAmount(text)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `test amount validation - valid range`() {
        val validAmounts = listOf(0.01, 1.0, 100.0, 999999.99)
        validAmounts.forEach { amount ->
            assertThat(isValidAmount(amount)).isTrue()
        }
    }

    @Test
    fun `test amount validation - invalid range`() {
        val invalidAmounts = listOf(0.0, -1.0, 1000000.0, -100.0)
        invalidAmounts.forEach { amount ->
            assertThat(isValidAmount(amount)).isFalse()
        }
    }

    private fun extractAmount(text: String): Double? {
        val matcher = amountPattern.matcher(text)
        val candidates = mutableListOf<Double>()

        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            val amount = amountStr?.toDoubleOrNull() ?: continue
            if (isValidAmount(amount)) {
                candidates.add(amount)
            }
        }

        return candidates.firstOrNull()
    }

    private fun isValidAmount(amount: Double): Boolean {
        return amount > 0 && amount < 1_000_000
    }
}

class TransactionValidationTest {

    @Test
    fun `test valid transaction data`() {
        val validCases = listOf(
            Triple(100.0, "美团外卖", "餐饮"),
            Triple(50.5, "超市", "购物"),
            Triple(1.0, "地铁", "交通")
        )

        validCases.forEach { (amount, merchant, category) ->
            assertThat(isValidTransaction(amount, merchant, category)).isTrue()
        }
    }

    @Test
    fun `test invalid transaction - zero amount`() {
        assertThat(isValidTransaction(0.0, "商户", "餐饮")).isFalse()
    }

    @Test
    fun `test invalid transaction - negative amount`() {
        assertThat(isValidTransaction(-100.0, "商户", "餐饮")).isFalse()
    }

    @Test
    fun `test invalid transaction - blank merchant`() {
        assertThat(isValidTransaction(100.0, "", "餐饮")).isFalse()
        assertThat(isValidTransaction(100.0, "   ", "餐饮")).isFalse()
    }

    @Test
    fun `test invalid transaction - too large amount`() {
        assertThat(isValidTransaction(1000001.0, "商户", "餐饮")).isFalse()
    }

    private fun isValidTransaction(amount: Double, merchant: String, category: String): Boolean {
        return amount > 0 &&
                amount < 1_000_000 &&
                merchant.isNotBlank() &&
                category.isNotBlank()
    }
}
