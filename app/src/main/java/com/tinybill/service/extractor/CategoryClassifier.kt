package com.tinybill.service.extractor

import com.tinybill.data.entity.Transaction
import com.tinybill.util.CategoryKeywordManager

class CategoryClassifier(
    private val keywordManager: CategoryKeywordManager
) {

    fun classify(merchant: String, type: Int = Transaction.TYPE_EXPENSE): String {
        keywordManager.getMerchantCategory(merchant, type)?.let { return it }
        return if (type == Transaction.TYPE_EXPENSE) {
            Transaction.CATEGORY_OTHER
        } else {
            Transaction.CATEGORY_OTHER_INCOME
        }
    }
}
