package com.example.selvamoneymanager.db

import androidx.room.*
import com.example.selvamoneymanager.trans.CategoryTotal
import com.example.selvamoneymanager.trans.TransactionRow

@Dao
interface TransactionDao {

    // Insert transaction, return ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: Transaction): Long

    // Update transaction
    @Update
    suspend fun update(txn: Transaction)

    // Delete transaction
    @Delete
    suspend fun delete(txn: Transaction)

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

    // Get all rows (latest first)
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

    // Get totals grouped by category
    @Query("""
        SELECT 
          COALESCE(cat.name, 'Uncategorized') AS category,
          ABS(SUM(t.amount)) AS total
        FROM transactions t
        LEFT JOIN categories cat ON cat.id = t.categoryId
        WHERE t.type = :type
          AND t.dateMillis BETWEEN :startMillis AND :endMillis
        GROUP BY COALESCE(cat.name, 'Uncategorized')
        HAVING ABS(SUM(t.amount)) > 0
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotalsInRange(
        type: TransactionType,   // enum, thanks to TypeConverter
        startMillis: Long,
        endMillis: Long
    ): List<CategoryTotal>
}
