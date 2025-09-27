package com.example.selvamoneymanager.db

import androidx.room.*
import com.example.selvamoneymanager.trans.CategoryTotal
import com.example.selvamoneymanager.trans.TransactionRow

@Dao
interface TransactionDao {

    // Insert single transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: Transaction): Long

    // ✅ Insert multiple transactions at once (needed for restore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    // Update transaction
    @Update
    suspend fun update(txn: Transaction)

    // Delete transaction
    @Delete
    suspend fun delete(txn: Transaction)

    // ✅ Plain getAll for backup/export
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<Transaction>

    // Get all rows within a date range (with joins)
    @Query("""
        SELECT 
          t.id, t.type, t.dateMillis, t.amount, 
          cat.name AS category,
          t.description,
          t.accountId, acc.name AS accountName,
          t.fromAccountId, accFrom.name AS fromAccountName,
          t.toAccountId, accTo.name AS toAccountName
        FROM transactions t
        LEFT JOIN accounts acc      ON acc.id = t.accountId
        LEFT JOIN accounts accFrom  ON accFrom.id = t.fromAccountId
        LEFT JOIN accounts accTo    ON accTo.id = t.toAccountId
        LEFT JOIN categories cat    ON cat.id = t.categoryId
        WHERE t.dateMillis BETWEEN :startMillis AND :endMillis
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    suspend fun getRowsInRange(
        startMillis: Long,
        endMillis: Long
    ): List<TransactionRow>

    @Query("SELECT * FROM transactions WHERE id = :txnId LIMIT 1")
    suspend fun getById(txnId: Long): Transaction?

    // Get all rows (latest first, with joins)
    @Query("""
        SELECT 
          t.id, t.type, t.dateMillis, t.amount, 
          cat.name AS category,
          t.description,
          t.accountId, acc.name AS accountName,
          t.fromAccountId, accFrom.name AS fromAccountName,
          t.toAccountId, accTo.name AS toAccountName
        FROM transactions t
        LEFT JOIN accounts acc      ON acc.id = t.accountId
        LEFT JOIN accounts accFrom  ON accFrom.id = t.fromAccountId
        LEFT JOIN accounts accTo    ON accTo.id = t.toAccountId
        LEFT JOIN categories cat    ON cat.id = t.categoryId
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    suspend fun getAllRows(): List<TransactionRow>

    // ✅ Totals grouped by category
    @Query("""
        SELECT 
          t.categoryId AS categoryId,
          COALESCE(cat.name, 'Uncategorized') AS category,
          ABS(SUM(t.amount)) AS total
        FROM transactions t
        LEFT JOIN categories cat ON cat.id = t.categoryId
        WHERE t.type = :type
          AND t.dateMillis BETWEEN :startMillis AND :endMillis
        GROUP BY t.categoryId, COALESCE(cat.name, 'Uncategorized')
        HAVING ABS(SUM(t.amount)) > 0
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotalsInRange(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): List<CategoryTotal>

    // Get all txns for a given categoryId
    @Query("""
        SELECT * FROM transactions
        WHERE categoryId = :categoryId
          AND dateMillis BETWEEN :startDate AND :endDate
        ORDER BY dateMillis DESC
    """)
    suspend fun getTransactionsByCategory(
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :catId")
    suspend fun countByCategory(catId: Int): Int

    // ✅ Per-account totals including transfers
    @Query("""
    SELECT
      a.id AS accountId,
      COALESCE((
        SELECT SUM(t.amount)
        FROM transactions t
        WHERE t.accountId = a.id AND t.type = 'INCOME'
      ), 0) AS income,
      COALESCE((
        SELECT SUM(ABS(t.amount))
        FROM transactions t
        WHERE t.accountId = a.id AND t.type = 'EXPENSE'
      ), 0) AS expense,
      COALESCE((
        SELECT SUM(ABS(t.amount))
        FROM transactions t
        WHERE t.type = 'TRANSFER' AND t.toAccountId = a.id
      ), 0) AS transferIn,
      COALESCE((
        SELECT SUM(ABS(t.amount))
        FROM transactions t
        WHERE t.type = 'TRANSFER' AND t.fromAccountId = a.id
      ), 0) AS transferOut
    FROM accounts a
    ORDER BY a.id
    """)
    suspend fun getBalancesByAccount(): List<AccountBalanceResult>

    // (Optional) debug helpers
    @Query("SELECT DISTINCT type FROM transactions")
    suspend fun distinctTypes(): List<String>

    @Query("SELECT COUNT(*) FROM transactions WHERE type = 'transfer'")
    suspend fun countTransfers(): Int
}
