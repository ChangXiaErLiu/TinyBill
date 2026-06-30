package com.tinybill.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create accounts table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                initialBalance REAL NOT NULL,
                currentBalance REAL NOT NULL,
                icon TEXT NOT NULL DEFAULT 'account_balance_wallet',
                color INTEGER NOT NULL DEFAULT 16777215,
                isDefault INTEGER NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT ''
            )
        """)

        // Create account_transactions table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS account_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                accountId INTEGER NOT NULL,
                transactionId INTEGER NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                balanceAfter REAL NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)

        // Insert default accounts
        db.execSQL("""
            INSERT INTO accounts (name, type, initialBalance, currentBalance, icon, color, isDefault, note)
            VALUES ('现金', 'CASH', 0.0, 0.0, 'payments', 10921638, 1, '')
        """)

        db.execSQL("""
            INSERT INTO accounts (name, type, initialBalance, currentBalance, icon, color, isDefault, note)
            VALUES ('支付宝', 'E_WALLET', 0.0, 0.0, 'account_balance', 147455, 1, '')
        """)

        db.execSQL("""
            INSERT INTO accounts (name, type, initialBalance, currentBalance, icon, color, isDefault, note)
            VALUES ('微信支付', 'E_WALLET', 0.0, 0.0, 'chat', 509792, 1, '')
        """)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDeleted_timestamp ON transactions(isDeleted, timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_merchant_amount_timestamp ON transactions(merchant, amount, timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category_timestamp_type ON transactions(category, timestamp, type)")
    }
}

/**
 * 把 transactions 表从 8 个索引瘦身到 3 个。
 * 索引并不是越多越好：每条 INSERT/UPDATE 都要维护 8 棵 B-Tree，对
 * "写多读少" 的记账应用是显著负担。8 -> 3 的裁剪理论上能让写入
 * 路径的写放大减少 ~60%。
 *
 * 保留的 3 个索引都是经过实际调用方验证的：
 * - (isDeleted, timestamp)  : 主页 + 所有时间范围查询都走它
 * - (merchant, amount, timestamp) : 去重检测
 * - (category, timestamp, type) : 分类支出 / 分类汇总
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 删除旧版冗余索引。IF EXISTS 保证在某些早期未带索引的数据库上不会崩
        db.execSQL("DROP INDEX IF EXISTS index_transactions_timestamp")
        db.execSQL("DROP INDEX IF EXISTS index_transactions_category")
        db.execSQL("DROP INDEX IF EXISTS index_transactions_type")
        db.execSQL("DROP INDEX IF EXISTS index_transactions_timestamp_type")
        db.execSQL("DROP INDEX IF EXISTS index_transactions_timestamp_category")
    }
}

/**
 * v10→v11：
 * - 删除从未写入数据的 custom_category / budget 表
 * - 创建 template 表（支持记账模板功能）
 *
 * 这两个表的 DAO 从未被调用过——所有分类和预算数据实际通过
 * DataStore（CustomCategoryPrefsRepository / BudgetRepository）持久化。
 * 删除空表不影响任何用户数据。
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS custom_category")
        db.execSQL("DROP TABLE IF EXISTS budget")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS template (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                type INTEGER NOT NULL DEFAULT 0,
                useCount INTEGER NOT NULL DEFAULT 0,
                lastUsed INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}
