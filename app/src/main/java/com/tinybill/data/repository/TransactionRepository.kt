package com.tinybill.data.repository

import com.tinybill.data.dao.CategoryTotal
import com.tinybill.data.dao.DailyTotal
import com.tinybill.data.dao.TransactionDao
import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    val deletedTransactions: Flow<List<Transaction>> = transactionDao.getDeletedTransactions()
    
    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }
    
    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }
    
    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
    
    suspend fun softDelete(id: Long) {
        transactionDao.softDelete(id)
    }
    
    suspend fun restore(id: Long) {
        transactionDao.restore(id)
    }
    
    suspend fun searchTransactions(keyword: String): List<Transaction> {
        return transactionDao.searchTransactionsOnce(keyword)
    }
    
    fun searchTransactionsFlow(keyword: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(keyword)
    }
    
    suspend fun isDuplicate(merchant: String, amount: Double, timestamp: Long): Boolean {
        val fiveMinutes = 5 * 60 * 1000
        val startTime = timestamp - fiveMinutes
        val endTime = timestamp + fiveMinutes
        
        // 首先使用精确匹配
        val exactMatch = transactionDao.findDuplicate(merchant, amount, startTime, endTime)
        if (exactMatch != null) {
            return true
        }
        
        // 然后使用相似度算法
        val similarTransactions = transactionDao.findSimilarTransactions(merchant, amount, startTime, endTime)
        return similarTransactions.isNotEmpty()
    }
    
    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis
        return transactionDao.getTransactionsByTimeRange(startTime, endTime)
    }

    fun getMonthStartAndEnd(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis
        return startTime to endTime
    }

    fun getYearStartAndEnd(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1)
        val endTime = calendar.timeInMillis
        return startTime to endTime
    }
    
    fun getWeekStartAndEnd(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endTime = calendar.timeInMillis
        return startTime to endTime
    }

    fun getWeekStartAndEndByOffset(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, offset)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endTime = calendar.timeInMillis
        return startTime to endTime
    }

    suspend fun getMonthTotal(year: Int, month: Int): Double {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getTotalAmountByType(startTime, endTime, Transaction.TYPE_EXPENSE) ?: 0.0
    }
    
    suspend fun getMonthExpense(year: Int, month: Int): Double {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getTotalExpense(startTime, endTime) ?: 0.0
    }
    
    suspend fun getMonthIncome(year: Int, month: Int): Double {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getTotalIncome(startTime, endTime) ?: 0.0
    }

    suspend fun getYearTotal(year: Int): Double {
        val (startTime, endTime) = getYearStartAndEnd(year)
        return transactionDao.getTotalAmountByType(startTime, endTime, Transaction.TYPE_EXPENSE) ?: 0.0
    }

    suspend fun getYearExpense(year: Int): Double {
        val (startTime, endTime) = getYearStartAndEnd(year)
        return transactionDao.getTotalExpense(startTime, endTime) ?: 0.0
    }
    
    suspend fun getYearIncome(year: Int): Double {
        val (startTime, endTime) = getYearStartAndEnd(year)
        return transactionDao.getTotalIncome(startTime, endTime) ?: 0.0
    }

    suspend fun getMonthCategorySummary(year: Int, month: Int): List<CategoryTotal> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getCategorySummary(startTime, endTime, Transaction.TYPE_EXPENSE)
    }
    
    suspend fun getMonthIncomeCategorySummary(year: Int, month: Int): List<CategoryTotal> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getCategorySummary(startTime, endTime, Transaction.TYPE_INCOME)
    }

    suspend fun getYearCategorySummary(year: Int): List<CategoryTotal> {
        val (startTime, endTime) = getYearStartAndEnd(year)
        return transactionDao.getCategorySummary(startTime, endTime, Transaction.TYPE_EXPENSE)
    }

    suspend fun getDatesWithTransactions(year: Int, month: Int): List<String> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getDatesWithTransactions(startTime, endTime)
    }

    suspend fun getTransactionsByDate(year: Int, month: Int, day: Int): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis
        return transactionDao.getTransactionsByDate(startTime, endTime)
    }

    suspend fun getTransactionsByMonthRange(year: Int, month: Int): List<Transaction> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getTransactionsByTimeRange(startTime, endTime).first()
    }

    suspend fun getTransactionsByYearRange(year: Int): List<Transaction> {
        val (startTime, endTime) = getYearStartAndEnd(year)
        return transactionDao.getTransactionsByTimeRange(startTime, endTime).first()
    }
    
    suspend fun getTransactionsByTimeRange(startTime: Long, endTime: Long): List<Transaction> {
        return transactionDao.getTransactionsByTimeRange(startTime, endTime).first()
    }

    suspend fun getTodayExpense(): Double {
        val (startTime, endTime) = getTodayStartAndEnd()
        return transactionDao.getTotalExpense(startTime, endTime) ?: 0.0
    }

    suspend fun getTodayIncome(): Double {
        val (startTime, endTime) = getTodayStartAndEnd()
        return transactionDao.getTotalIncome(startTime, endTime) ?: 0.0
    }

    suspend fun getTodayTransactionCount(): Int {
        val (startTime, endTime) = getTodayStartAndEnd()
        // 走 (isDeleted, timestamp) 索引的 COUNT(*)，避免拉取整张表
        return transactionDao.getCountByTimeRange(startTime, endTime)
    }

    private fun getTodayStartAndEnd(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis
        return startTime to endTime
    }

    suspend fun getTodayTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis
        return transactionDao.getTransactionsByDate(startTime, endTime)
    }
    
    suspend fun getWeekExpense(): Double {
        val (startTime, endTime) = getWeekStartAndEnd()
        return transactionDao.getTotalExpense(startTime, endTime) ?: 0.0
    }

    suspend fun getWeekExpenseByOffset(offset: Int): Double {
        val (startTime, endTime) = getWeekStartAndEndByOffset(offset)
        return transactionDao.getTotalExpense(startTime, endTime) ?: 0.0
    }

    suspend fun getWeekIncome(): Double {
        val (startTime, endTime) = getWeekStartAndEnd()
        return transactionDao.getTotalIncome(startTime, endTime) ?: 0.0
    }

    suspend fun getWeekIncomeByOffset(offset: Int): Double {
        val (startTime, endTime) = getWeekStartAndEndByOffset(offset)
        return transactionDao.getTotalIncome(startTime, endTime) ?: 0.0
    }

    suspend fun getWeekTransactionsByOffset(offset: Int): List<Transaction> {
        val (startTime, endTime) = getWeekStartAndEndByOffset(offset)
        return transactionDao.getTransactionsByDate(startTime, endTime)
    }

    suspend fun getWeekDailyTotalsByOffset(offset: Int): List<DailyTotal> {
        val (startTime, endTime) = getWeekStartAndEndByOffset(offset)
        return transactionDao.getDailyExpense(startTime, endTime)
    }
    
    suspend fun getDailyExpenseForMonth(year: Int, month: Int): List<DailyTotal> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getDailyExpense(startTime, endTime)
    }
    
    suspend fun getDailyIncomeForMonth(year: Int, month: Int): List<DailyTotal> {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getDailyIncome(startTime, endTime)
    }
    
    suspend fun getCategoryExpense(year: Int, month: Int, category: String): Double {
        val (startTime, endTime) = getMonthStartAndEnd(year, month)
        return transactionDao.getCategoryExpense(category, startTime, endTime) ?: 0.0
    }

    /**
     * 获取过去 N 个月的月度支出趋势数据。
     * 每个元素为 Pair(年月标签, 支出总额)，按月升序排列。
     * 用于统计页的支出趋势折线图。
     */
    suspend fun getMonthlyExpenseTrend(
        endYear: Int,
        endMonth: Int,
        months: Int = 12
    ): List<Pair<String, Double>> {
        val result = mutableListOf<Pair<String, Double>>()
        var year = endYear
        var month = endMonth

        for (i in 0 until months) {
            val expense = getMonthExpense(year, month)
            result.add(0, "${year}年${month}月" to expense) // 从最远到最近插入

            month--
            if (month == 0) {
                month = 12
                year--
            }
        }
        return result
    }
    
    suspend fun getDeletedTransactions(): List<Transaction> {
        return transactionDao.getDeletedTransactionsOnce()
    }
    
    suspend fun purgeOldDeleted() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        transactionDao.purgeOldDeleted(thirtyDaysAgo)
    }

    suspend fun getMerchantSuggestions(keyword: String): List<String> {
        return transactionDao.getMerchantSuggestions(keyword)
    }

    suspend fun getMostUsedCategoryForMerchant(merchant: String): String? {
        return transactionDao.getMostUsedCategoryForMerchant(merchant)?.category
    }
}
