package com.tinybill.service.extractor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MerchantExtractorTest {

    private val extractor = MerchantExtractor()

    @Test
    fun `extract rejects payment related texts`() {
        assertThat(extractor.isValidMerchantText("支付成功")).isFalse()
        assertThat(extractor.isValidMerchantText("付款金额")).isFalse()
        assertThat(extractor.isValidMerchantText("商户名称")).isFalse()
    }

    @Test
    fun `extract rejects texts with currency symbols`() {
        assertThat(extractor.isValidMerchantText("¥100")).isFalse()
        assertThat(extractor.isValidMerchantText("￥88")).isFalse()
        assertThat(extractor.isValidMerchantText("美金 $50")).isFalse()
    }

    @Test
    fun `extract rejects pure numeric texts`() {
        assertThat(extractor.isValidMerchantText("12345")).isFalse()
        assertThat(extractor.isValidMerchantText("100.50")).isFalse()
    }

    @Test
    fun `extract rejects too short or too long texts`() {
        assertThat(extractor.isValidMerchantText("美")).isFalse()
        assertThat(extractor.isValidMerchantText("")).isFalse()
        assertThat(extractor.isValidMerchantText("x".repeat(50))).isFalse()
    }

    @Test
    fun `extract accepts valid merchant names`() {
        assertThat(extractor.isValidMerchantText("美团外卖")).isTrue()
        assertThat(extractor.isValidMerchantText("Starbucks")).isTrue()
        assertThat(extractor.isValidMerchantText("盒马鲜生")).isTrue()
    }

    @Test
    fun `extract from keyword strategy strips prefix`() {
        val texts = listOf("商户名称:美团外卖", "支付成功")
        val result = extractor.extract(texts, emptyList(), "微信")
        assertThat(result).isEqualTo("美团外卖")
    }

    @Test
    fun `extract returns default prefix when nothing matches`() {
        val result = extractor.extract(listOf("支付成功", "¥100"), emptyList(), "微信")
        assertThat(result).isEqualTo("微信商户")
    }

    @Test
    fun `extract red packet merchant from sender prefix`() {
        val texts = listOf("来自 张三", "¥88.00")
        val result = extractor.extractRedPacketMerchant(texts, isReceive = true)
        assertThat(result).isEqualTo("张三")
    }

    @Test
    fun `extract transfer merchant filters out transfer keywords`() {
        val texts = listOf("张三", "转账成功", "¥200")
        val result = extractor.extractTransferMerchant(texts, isReceive = false)
        assertThat(result).isEqualTo("张三")
    }

    @Test
    fun `isLowConfidence detects generic placeholder`() {
        val nodes = listOf(MerchantExtractor.TextNode("微信商户", 0.4f))
        assertThat(extractor.isLowConfidence("微信商户", nodes)).isTrue()
    }

    @Test
    fun `isLowConfidence passes confident merchant`() {
        val nodes = listOf(MerchantExtractor.TextNode("美团外卖", 0.9f))
        assertThat(extractor.isLowConfidence("美团外卖", nodes)).isFalse()
    }
}
