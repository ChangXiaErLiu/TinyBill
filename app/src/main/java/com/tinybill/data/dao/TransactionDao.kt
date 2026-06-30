package com.tinybill.data.dao

import androidx.room.*
import com.tinybill.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (merchant LIKE '%' || :keyword || '%' OR note LIKE '%' || :keyword || '%') AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchTransactions(keyword: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (merchant LIKE '%' || :keyword || '%' OR note LIKE '%' || :keyword || '%') AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun searchTransactionsOnce(keyword: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsByDate(startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE merchant = :merchant AND amount = :amount AND timestamp >= :startTime AND timestamp <= :endTime LIMIT 1")
    suspend fun findDuplicate(merchant: String, amount: Double, startTime: Long, endTime: Long): Transaction?

    @Query("""
        SELECT * FROM transactions
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND ABS(amount - :amount) <= :amount * 0.1
        ORDER BY (CASE
            WHEN merchant = :merchant THEN 1
            WHEN merchant LIKE '%' || :merchant || '%' OR :merchant LIKE '%' || merchant || '%' THEN 2
            ELSE 3
        END),
        ABS(amount - :amount)
        LIMIT 1
    """)
    suspend fun findSimilarTransactions(merchant: String, amount: Double, startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = :type AND isDeleted = 0")
    suspend fun getTotalAmountByType(startTime: Long, endTime: Long, type: Int): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = 0 AND isDeleted = 0")
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = 1 AND isDeleted = 0")
    suspend fun getTotalIncome(startTime: Long, endTime: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = :type AND isDeleted = 0 GROUP BY category")
    suspend fun getCategorySummary(startTime: Long, endTime: Long, type: Int): List<CategoryTotal>

    @Query("SELECT DISTINCT date(timestamp/1000, 'unixepoch', 'localtime') as date FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0")
    suspend fun getDatesWithTransactions(startTime: Long, endTime: Long): List<String>

    @Query("SELECT DISTINCT merchant FROM transactions WHERE merchant LIKE '%' || :keyword || '%' AND isDeleted = 0 ORDER BY timestamp DESC LIMIT 10")
    suspend fun getMerchantSuggestions(keyword: String): List<String>

    @Query("SELECT category, COUNT(*) as count FROM transactions WHERE merchant = :merchant AND isDeleted = 0 GROUP BY category ORDER BY count DESC LIMIT 1")
    suspend fun getMostUsedCategoryForMerchant(merchant: String): CategoryCount?

    @Query("SELECT SUM(amount) as total, date(timestamp/1000, 'unixepoch', 'localtime') as date FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = 0 AND isDeleted = 0 GROUP BY date ORDER BY date")
    suspend fun getDailyExpense(startTime: Long, endTime: Long): List<DailyTotal>

    @Query("SELECT SUM(amount) as total, date(timestamp/1000, 'unixepoch', 'localtime') as date FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND type = 1 AND isDeleted = 0 GROUP BY date ORDER BY date")
    suspend fun getDailyIncome(startTime: Long, endTime: Long): List<DailyTotal>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :category AND timestamp >= :startTime AND timestamp <= :endTime AND type = 0 AND isDeleted = 0")
    suspend fun getCategoryExpense(category: String, startTime: Long, endTime: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0")
    suspend fun getCountByTimeRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY timestamp DESC")
    suspend fun getDeletedTransactionsOnce(): List<Transaction>

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE transactions SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND timestamp < :threshold")
    suspend fun purgeOldDeleted(threshold: Long)

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

data class CategoryTotal(
    val category: String,
    val total: Double
)

data class DailyTotal(
    val date: String,
    val total: Double
)

data class CategoryCount(
    val category: String,
    val count: Int
)
