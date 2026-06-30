package com.tinybill.service.extractor

import java.util.regex.Pattern

class AmountExtractor {

    data class Candidate(val amount: Double, val confidence: Float)

    fun extract(texts: List<String>): Double {
        val candidates = mutableListOf<Candidate>()

        for (text in texts) {
            val matcher = AMOUNT_PATTERN.matcher(text)
            while (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: continue
                if (amount in MIN_AMOUNT..MAX_AMOUNT) {
                    val confidence = calculateConfidence(text, matcher.group(0) ?: "")
                    candidates.add(Candidate(amount, confidence))
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.addAll(extractFallback(texts))
        }

        if (candidates.isEmpty()) return 0.0

        val best = candidates.maxByOrNull { it.confidence }
        return if (best != null && best.confidence >= MIN_CONFIDENCE) {
            best.amount
        } else {
            candidates.maxByOrNull { it.amount }?.amount ?: 0.0
        }
    }

    private fun extractFallback(texts: List<String>): List<Candidate> {
        val result = mutableListOf<Candidate>()
        for (text in texts) {
            val matcher = FALLBACK_NUMBER_PATTERN.matcher(text)
            while (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: continue
                if (amount in MIN_AMOUNT..MAX_AMOUNT) {
                    val confidence = calculateConfidence(text, matcher.group(0) ?: "") * FALLBACK_CONFIDENCE_FACTOR
                    result.add(Candidate(amount, confidence))
                }
            }
        }
        return result
    }

    private fun calculateConfidence(text: String, matchedAmount: String): Float {
        var confidence = BASE_CONFIDENCE
        if (text.contains("支付") || text.contains("付款") || text.contains("金额")) {
            confidence += PAYMENT_KEYWORD_BONUS
        }
        if (text.contains("成功") || text.contains("完成")) {
            confidence += SUCCESS_KEYWORD_BONUS
        }
        if (matchedAmount.isNotEmpty() && matchedAmount.length <= MAX_MATCHED_LENGTH) {
            confidence += LENGTH_BONUS
        }
        return confidence.coerceIn(0f, 1f)
    }

    companion object {
        private const val MIN_AMOUNT = 0.0
        private const val MAX_AMOUNT = 1_000_000.0
        private const val MIN_CONFIDENCE = 0.6f
        private const val BASE_CONFIDENCE = 0.5f
        private const val PAYMENT_KEYWORD_BONUS = 0.3f
        private const val SUCCESS_KEYWORD_BONUS = 0.2f
        private const val LENGTH_BONUS = 0.1f
        private const val MAX_MATCHED_LENGTH = 10
        private const val FALLBACK_CONFIDENCE_FACTOR = 0.8f

        private val AMOUNT_PATTERN = Pattern.compile("[¥￥]\\s*([0-9,]+\\.?[0-9]*)")
        private val FALLBACK_NUMBER_PATTERN = Pattern.compile("([0-9,]+\\.?[0-9]*)")
    }
}
