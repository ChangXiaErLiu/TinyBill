package com.tinybill.service.extractor

import android.view.accessibility.AccessibilityNodeInfo
import com.tinybill.service.BillAccessibilityService
import java.util.regex.Pattern

class MerchantExtractor {

    data class TextNode(val text: String, val confidence: Float)

    fun extract(
        texts: List<String>,
        nodeInfoList: List<TextNode>,
        defaultPrefix: String
    ): String? {
        val strategies = listOf(
            { extractFromNodeInfo(nodeInfoList) },
            { extractFromKeyword(texts) },
            { extractFromTextLength(texts) },
            { extractFromUnionPay(texts) },
            { extractFromPosition(texts) },
            { extractFromPattern(texts) }
        )

        for (strategy in strategies) {
            try {
                val result = strategy()
                if (result != null && result.length in MIN_MERCHANT_LENGTH..MAX_MERCHANT_LENGTH) {
                    return result
                }
            } catch (_: Exception) {
            }
        }

        return "${defaultPrefix}商户"
    }

    fun extractRedPacketMerchant(texts: List<String>, isReceive: Boolean): String? {
        for (text in texts) {
            if (isReceive && (text.contains("来自") || text.contains("发送者"))) {
                val cleaned = text.replace("来自", "").replace("发送者", "").trim()
                if (cleaned.isNotEmpty() && cleaned.length <= MAX_MERCHANT_LENGTH) {
                    return cleaned
                }
            }
        }
        return texts.firstOrNull {
            !it.contains("¥") && !it.contains("￥") &&
            !it.contains("红包") && !it.contains("成功") &&
            it.length in MIN_MERCHANT_LENGTH..RED_PACKET_NAME_MAX_LENGTH
        }
    }

    fun extractTransferMerchant(texts: List<String>, isReceive: Boolean): String? {
        for (text in texts) {
            if (isReceive && (text.contains("来自") || text.contains("转账来自"))) {
                val cleaned = text.replace("来自", "").replace("转账来自", "").trim()
                if (cleaned.isNotEmpty() && cleaned.length <= MAX_MERCHANT_LENGTH) {
                    return cleaned
                }
            }
        }
        return texts.firstOrNull {
            !it.contains("¥") && !it.contains("￥") &&
            !it.contains("转账") && !it.contains("成功") &&
            it.length in MIN_MERCHANT_LENGTH..TRANSFER_NAME_MAX_LENGTH
        }
    }

    fun isLowConfidence(merchant: String, nodeInfoList: List<TextNode>): Boolean {
        val nodeConfidence = nodeInfoList.find { it.text == merchant }?.confidence ?: 0.5f
        return nodeConfidence < LOW_CONFIDENCE_THRESHOLD &&
            (merchant.contains("商户") || merchant.contains("商家"))
    }

    private fun extractFromNodeInfo(nodeInfoList: List<TextNode>): String? {
        return nodeInfoList
            .filter { it.confidence > NODE_CONFIDENCE_THRESHOLD }
            .map { it.text }
            .firstOrNull { isValidMerchantText(it) }
    }

    private fun extractFromKeyword(texts: List<String>): String? {
        for (text in texts) {
            if (text.contains("商户") || text.contains("商家") || text.contains("收款方")) {
                val cleaned = text.replace("商户", "")
                    .replace("商家", "")
                    .replace("收款方", "")
                    .replace(":", "")
                    .replace("：", "")
                    .trim()
                if (cleaned.isNotEmpty() && cleaned != "支付成功" && cleaned.length <= MAX_MERCHANT_LENGTH) {
                    return cleaned
                }
            }
        }
        return null
    }

    private fun extractFromTextLength(texts: List<String>): String? {
        return texts
            .filter { isValidMerchantText(it) }
            .filter { it.length in MIN_MERCHANT_LENGTH..SHORT_NAME_MAX_LENGTH }
            .firstOrNull()
    }

    private fun extractFromUnionPay(texts: List<String>): String? {
        for (text in texts) {
            if (text.contains("消费") || text.contains("收款")) {
                val cleaned = text.replace("消费", "")
                    .replace("收款", "")
                    .replace(":", "")
                    .replace("：", "")
                    .trim()
                if (cleaned.isNotEmpty() && cleaned.length <= MAX_MERCHANT_LENGTH) {
                    return cleaned
                }
            }
        }
        return null
    }

    private fun extractFromPosition(texts: List<String>): String? {
        val pattern = Pattern.compile("[¥￥]\\s*([0-9,]+\\.?[0-9]*)")
        val amountIndex = texts.indexOfFirst { pattern.matcher(it).find() }
        if (amountIndex != -1) {
            val candidates = mutableListOf<String>()
            if (amountIndex > 0) candidates.add(texts[amountIndex - 1])
            if (amountIndex < texts.size - 1) candidates.add(texts[amountIndex + 1])
            for (candidate in candidates) {
                if (isValidMerchantText(candidate)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun extractFromPattern(texts: List<String>): String? {
        for (text in texts) {
            for (pattern in MERCHANT_PATTERNS) {
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    val candidate = matcher.group()
                    if (isValidMerchantText(candidate)) {
                        return candidate
                    }
                }
            }
        }
        return null
    }

    fun isValidMerchantText(text: String): Boolean {
        if (text.isBlank() || text.length < MIN_MERCHANT_LENGTH || text.length > MAX_MERCHANT_LENGTH) {
            return false
        }
        val lowerText = text.lowercase()
        for (excluded in EXCLUDED_MERCHANT_TEXTS) {
            if (lowerText.contains(excluded.lowercase())) return false
        }
        if (text.contains("¥") || text.contains("￥") || text.contains("$")) return false
        if (text.contains("成功") || text.contains("失败") || text.contains("错误")) return false
        if (text.all { it.isDigit() || it == '.' }) return false
        return true
    }

    fun collectAllTexts(rootNode: AccessibilityNodeInfo): Pair<List<String>, List<TextNode>> {
        val texts = mutableListOf<String>()
        val nodeInfoList = mutableListOf<TextNode>()
        findAllTextWithConfidence(rootNode, texts, nodeInfoList)
        return texts to nodeInfoList
    }

    private fun findAllTextWithConfidence(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        nodeInfoList: MutableList<TextNode>
    ) {
        val text = node.text?.toString()
        if (!text.isNullOrEmpty() && !texts.contains(text)) {
            texts.add(text)
            nodeInfoList.add(TextNode(text, calculateTextConfidence(node, text)))
        }

        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrEmpty() && !texts.contains(desc)) {
            texts.add(desc)
            nodeInfoList.add(TextNode(desc, calculateTextConfidence(node, desc)))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                findAllTextWithConfidence(child, texts, nodeInfoList)
            } finally {
                child.recycle()
            }
        }
    }

    private fun calculateTextConfidence(node: AccessibilityNodeInfo, text: String): Float {
        var confidence = 0.5f
        if (node.isClickable) confidence += 0.1f
        if (node.isFocusable) confidence += 0.1f
        if (node.isEditable) confidence -= 0.2f
        val className = node.className?.toString() ?: ""
        if (className.contains("TextView") || className.contains("Button")) {
            confidence += 0.15f
        }
        if (text.length in IDEAL_TEXT_LENGTH_RANGE) confidence += 0.1f
        return confidence.coerceIn(0f, 1f)
    }

    companion object {
        private const val MIN_MERCHANT_LENGTH = 2
        private const val MAX_MERCHANT_LENGTH = 30
        private const val SHORT_NAME_MAX_LENGTH = 20
        private const val RED_PACKET_NAME_MAX_LENGTH = 15
        private const val TRANSFER_NAME_MAX_LENGTH = 15
        private const val NODE_CONFIDENCE_THRESHOLD = 0.7f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
        private val IDEAL_TEXT_LENGTH_RANGE = 2..20

        private val EXCLUDED_MERCHANT_TEXTS = setOf(
            "支付", "付款", "商户", "金额", "成功", "失败", "错误", "订单", "交易",
            "时间", "日期", "编号", "备注", "合计", "总计", "小计", "优惠", "折扣",
            "红包", "转账", "收款", "发送", "来自", "发给", "状态", "完成", "等待",
            "¥", "￥", "$", "USD", "EUR", "GBP"
        )

        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("[\u4e00-\u9fa5]{2,15}"),
            Pattern.compile("[A-Za-z]{2,15}"),
            Pattern.compile("[\u4e00-\u9fa5A-Za-z]{2,20}")
        )
    }
}
