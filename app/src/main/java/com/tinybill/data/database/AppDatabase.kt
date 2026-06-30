package com.tinybill.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tinybill.data.dao.AccountDao
import com.tinybill.data.dao.ScheduledTransactionDao
import com.tinybill.data.dao.TemplateDao
import com.tinybill.data.dao.TransactionDao
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountTransaction
import com.tinybill.data.entity.ScheduledTransaction
import com.tinybill.data.entity.Template
import com.tinybill.data.entity.Transaction

/**
 * TinyBill 数据库。
 *
 * v11（2026-06）：移除从未写入数据的 custom_category / budget 表及相关 DAO，
 * 这些数据实际由 DataStore（CustomCategoryPrefsRepository / BudgetRepository）持久化。
 * Room 实体 Budget / CustomCategory 降级为纯数据类，仅作备份 DTO 使用。
 * 新增 template 表支持记账模板功能。
 */
@Database(
    entities = [
        Transaction::class,
        ScheduledTransaction::class,
        Template::class,
        Account::class,
        AccountTransaction::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledTransactionDao(): ScheduledTransactionDao
    abstract fun templateDao(): TemplateDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp_type ON transactions(timestamp, type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp_category ON transactions(timestamp, category)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scheduled_transaction (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        merchant TEXT NOT NULL,
                        category TEXT NOT NULL,
                        type INTEGER NOT NULL DEFAULT 0,
                        dayOfMonth INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        lastExecuted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_category (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        type INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget (
                        category TEXT PRIMARY KEY NOT NULL,
                        monthlyLimit REAL NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        alertThreshold REAL NOT NULL DEFAULT 0.8
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tinybill_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}