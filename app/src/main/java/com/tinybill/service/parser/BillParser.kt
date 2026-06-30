package com.tinybill.service.parser

import android.view.accessibility.AccessibilityNodeInfo
import com.tinybill.data.entity.Transaction
import com.tinybill.service.extractor.AmountExtractor
import com.tinybill.service.extractor.CategoryClassifier
import com.tinybill.service.extractor.MerchantExtractor

class BillParser(
    private val amountExtractor: AmountExtractor,
    private val merchantExtractor: MerchantExtractor,
    private val categoryClassifier: CategoryClassifier
) {

    sealed class ParseResult {
        data class Expense(
            val amount: Double,
            val merchant: String,
            val category: String
        ) : ParseResult()
        data class Income(
            val amount: Double,
            val merchant: String,
            val category: String,
            val type: Int
        ) : ParseResult()
        data object Ignored : ParseResult()
        data object NotApplicable : ParseResult()
    }

    fun parse(rootNode: AccessibilityNodeInfo, packageName: String): ParseResult {
        val (texts, nodeInfoList) = merchantExtractor.collectAllTexts(rootNode)
        return when (packageName) {
            WECHAT -> parseWeChat(rootNode, texts, nodeInfoList)
            ALIPAY -> parseAlipay(rootNode, texts, nodeInfoList)
            else -> if (packageName.startsWith(UNIONPAY)) {
                parseUnionPay(rootNode, texts, nodeInfoList)
            } else {
                ParseResult.NotApplicable
            }
        }
    }

    private fun parseWeChat(
        rootNode: AccessibilityNodeInfo,
        texts: List<String>,
        nodeInfoList: List<MerchantExtractor.TextNode>
    ): ParseResult {
        val isRedPacketReceive = hasAnyKeyword(texts, WECHAT_RED_PACKET_RECEIVE_KEYWORDS)
        val isRedPacketSend = hasAnyKeyword(texts, WECHAT_RED_PACKET_SEND_KEYWORDS)
        val isTransferReceive = hasAnyKeyword(texts, WECHAT_TRANSFER_RECEIVE_KEYWORDS)
        val isTransferSend = hasAnyKeyword(texts, WECHAT_TRANSFER_SEND_KEYWORDS)
        val isPayment = hasAnyKeyword(texts, WECHAT_PAY_SUCCESS_KEYWORDS)

        return when {
            isRedPacketReceive -> parseSpecialEvent(
                texts, isReceive = true,
                category = Transaction.CATEGORY_RED_PACKET,
                type = Transaction.TYPE_INCOME,
                defaultName = "微信红包",
                isRedPacket = true
            )
            isRedPacketSend -> parseSpecialEvent(
                texts, isReceive = false,
                category = Transaction.CATEGORY_RED_PACKET,
                type = Transaction.TYPE_EXPENSE,
                defaultName = "微信红包",
                isRedPacket = true
            )
            isTransferReceive -> parseSpecialEvent(
                texts, isReceive = true,
                category = Transaction.CATEGORY_TRANSFER,
                type = Transaction.TYPE_INCOME,
                defaultName = "微信转账",
                isRedPacket = false
            )
            isTransferSend -> parseSpecialEvent(
                texts, isReceive = false,
                category = Transaction.CATEGORY_TRANSFER,
                type = Transaction.TYPE_EXPENSE,
                defaultName = "微信转账",
                isRedPacket = false
            )
            isPayment -> parsePayment(rootNode, texts, nodeInfoList, "微信")
            else -> ParseResult.Ignored
        }
    }

    private fun parseAlipay(
        rootNode: AccessibilityNodeInfo,
        texts: List<String>,
        nodeInfoList: List<MerchantExtractor.TextNode>
    ): ParseResult {
        val isRedPacketReceive = hasAnyKeyword(texts, ALIPAY_RED_PACKET_RECEIVE_KEYWORDS)
        val isRedPacketSend = hasAnyKeyword(texts, ALIPAY_RED_PACKET_SEND_KEYWORDS)
        val isTransferReceive = hasAnyKeyword(texts, ALIPAY_TRANSFER_RECEIVE_KEYWORDS)
        val isTransferSend = hasAnyKeyword(texts, ALIPAY_TRANSFER_SEND_KEYWORDS)
        val isPayment = hasAnyKeyword(texts, ALIPAY_PAY_SUCCESS_KEYWORDS)

        return when {
            isRedPacketReceive -> parseSpecialEvent(
                texts, isReceive = true,
                category = Transaction.CATEGORY_RED_PACKET,
                type = Transaction.TYPE_INCOME,
                defaultName = "支付宝红包",
                isRedPacket = true
            )
            isRedPacketSend -> parseSpecialEvent(
                texts, isReceive = false,
                category = Transaction.CATEGORY_RED_PACKET,
                type = Transaction.TYPE_EXPENSE,
                defaultName = "支付宝红包",
                isRedPacket = true
            )
            isTransferReceive -> parseSpecialEvent(
                texts, isReceive = true,
                category = Transaction.CATEGORY_TRANSFER,
                type = Transaction.TYPE_INCOME,
                defaultName = "支付宝转账",
                isRedPacket = false
            )
            isTransferSend -> parseSpecialEvent(
                texts, isReceive = false,
                category = Transaction.CATEGORY_TRANSFER,
                type = Transaction.TYPE_EXPENSE,
                defaultName = "支付宝转账",
                isRedPacket = false
            )
            isPayment -> parsePayment(rootNode, texts, nodeInfoList, "支付宝")
            else -> ParseResult.Ignored
        }
    }

    private fun parseUnionPay(
        rootNode: AccessibilityNodeInfo,
        texts: List<String>,
        nodeInfoList: List<MerchantExtractor.TextNode>
    ): ParseResult {
        val isPayment = hasAnyKeyword(texts, UNIONPAY_PAY_SUCCESS_KEYWORDS)
        if (!isPayment) return ParseResult.Ignored

        val amount = amountExtractor.extract(texts)
        if (amount <= 0) return ParseResult.Ignored

        val merchant = merchantExtractor.extract(texts, nodeInfoList, "云闪付") ?: "云闪付商户"
        val category = categoryClassifier.classify(merchant, Transaction.TYPE_EXPENSE)
        return ParseResult.Expense(amount, merchant, category)
    }

    private fun parsePayment(
        rootNode: AccessibilityNodeInfo,
        texts: List<String>,
        nodeInfoList: List<MerchantExtractor.TextNode>,
        defaultPrefix: String
    ): ParseResult {
        if (!hasPaymentSuccessIndicator(rootNode)) {
            return ParseResult.Ignored
        }
        val amount = amountExtractor.extract(texts)
        if (amount <= 0) return ParseResult.Ignored

        val merchant = merchantExtractor.extract(texts, nodeInfoList, defaultPrefix)
            ?: return ParseResult.Ignored
        if (merchantExtractor.isLowConfidence(merchant, nodeInfoList)) {
            return ParseResult.Ignored
        }

        val category = categoryClassifier.classify(merchant, Transaction.TYPE_EXPENSE)
        return ParseResult.Expense(amount, merchant, category)
    }

    private fun parseSpecialEvent(
        texts: List<String>,
        isReceive: Boolean,
        category: String,
        type: Int,
        defaultName: String,
        isRedPacket: Boolean
    ): ParseResult {
        val amount = amountExtractor.extract(texts)
        if (amount <= 0) return ParseResult.Ignored

        val merchant = if (isRedPacket) {
            merchantExtractor.extractRedPacketMerchant(texts, isReceive) ?: defaultName
        } else {
            merchantExtractor.extractTransferMerchant(texts, isReceive) ?: defaultName
        }

        return ParseResult.Income(amount, merchant, category, type)
    }

    private fun hasAnyKeyword(texts: List<String>, keywords: List<String>): Boolean {
        return texts.any { text -> keywords.any { text.contains(it) } }
    }

    private fun hasPaymentSuccessIndicator(rootNode: AccessibilityNodeInfo): Boolean {
        val successNodes = rootNode.findAccessibilityNodeInfosByText("支付成功")
        val paySuccessNodes = rootNode.findAccessibilityNodeInfosByText("付款成功")
        val transactionNodes = rootNode.findAccessibilityNodeInfosByText("交易成功")
        val result = successNodes.isNotEmpty() || paySuccessNodes.isNotEmpty() || transactionNodes.isNotEmpty()
        successNodes.forEach { it.recycle() }
        paySuccessNodes.forEach { it.recycle() }
        transactionNodes.forEach { it.recycle() }
        return result
    }

    companion object {
        const val WECHAT = "com.tencent.mm"
        const val ALIPAY = "com.eg.android.AlipayGphone"
        const val UNIONPAY = "com.unionpay"

        private val WECHAT_RED_PACKET_RECEIVE_KEYWORDS = listOf(
            "已收款", "红包已领取", "收钱成功", "收到红包", "已收到", "收钱",
            "收款成功", "红包领取成功", "微信红包", "收到钱款"
        )
        private val WECHAT_RED_PACKET_SEND_KEYWORDS = listOf(
            "红包已发送", "发送成功", "红包发送成功", "已发送红包", "已发出红包"
        )
        private val WECHAT_TRANSFER_RECEIVE_KEYWORDS = listOf(
            "转账收款", "已收款", "收到转账", "转账成功", "收钱成功", "转账存入"
        )
        private val WECHAT_TRANSFER_SEND_KEYWORDS = listOf(
            "已转账", "转账成功", "转账已发出", "已发出转账", "转账转出"
        )
        private val WECHAT_PAY_SUCCESS_KEYWORDS = listOf(
            "支付成功", "付款成功", "已支付", "微信支付", "支付¥", "¥支付"
        )

        private val ALIPAY_RED_PACKET_RECEIVE_KEYWORDS = listOf(
            "收钱成功", "已收款", "收到红包", "收红包", "红包已领取", "红包领取成功"
        )
        private val ALIPAY_RED_PACKET_SEND_KEYWORDS = listOf(
            "发送成功", "红包已发送", "发红包成功", "已发红包"
        )
        private val ALIPAY_TRANSFER_RECEIVE_KEYWORDS = listOf(
            "收钱成功", "已收款", "收到转账", "转账成功", "收款成功", "收钱到账"
        )
        private val ALIPAY_TRANSFER_SEND_KEYWORDS = listOf(
            "已转账", "转账成功", "转出成功", "已转出", "转账转出"
        )
        private val ALIPAY_PAY_SUCCESS_KEYWORDS = listOf(
            "支付成功", "付款成功", "已付款", "交易成功", "支付宝支付", "支付¥", "¥支付"
        )

        private val UNIONPAY_PAY_SUCCESS_KEYWORDS = listOf(
            "支付成功", "交易成功", "付款成功", "云闪付"
        )
    }
}
