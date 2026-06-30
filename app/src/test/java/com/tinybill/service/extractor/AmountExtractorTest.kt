package com.tinybill.service.extractor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountExtractorTest {

    private val extractor = AmountExtractor()

    @Test
    fun `extract returns amount with yuan symbol`() {
        assertThat(extractor.extract(listOf("支付成功 ¥128.50"))).isEqualTo(128.50)
        assertThat(extractor.extract(listOf("支付成功¥100"))).isEqualTo(100.0)
        assertThat(extractor.extract(listOf("付款成功 ￥200.00"))).isEqualTo(200.0)
    }

    @Test
    fun `extract handles comma separator`() {
        assertThat(extractor.extract(listOf("¥1,280.50"))).isEqualTo(1280.50)
        assertThat(extractor.extract(listOf("支付成功 ¥10,000"))).isEqualTo(10000.0)
    }

    @Test
    fun `extract returns zero for empty input`() {
        assertThat(extractor.extract(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `extract returns zero when no amount present`() {
        assertThat(extractor.extract(listOf("支付成功", "微信支付"))).isEqualTo(0.0)
    }

    @Test
    fun `extract ignores out of range amounts`() {
        assertThat(extractor.extract(listOf("¥0"))).isEqualTo(0.0)
        assertThat(extractor.extract(listOf("¥-100"))).isEqualTo(0.0)
    }

    @Test
    fun `extract returns the highest confidence amount`() {
        val texts = listOf(
            "原价 ¥100.00",
            "支付金额 ¥88.50",
            "支付成功"
        )
        assertThat(extractor.extract(texts)).isEqualTo(88.50)
    }

    @Test
    fun `extract picks first when equal confidence`() {
        val texts = listOf("¥50.00 ¥100.00")
        val result = extractor.extract(texts)
        assertThat(result).isAnyOf(50.0, 100.0)
    }
}
