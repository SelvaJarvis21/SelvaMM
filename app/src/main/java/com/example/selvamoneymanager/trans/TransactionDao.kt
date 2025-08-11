package com.example.selvamoneymanager.trans

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(txn: Transaction)

    @Query("""
        SELECT 
          t.id, t.type, t.dateMillis, t.amount, t.category, t.description,
          t.accountId, acc.name AS accountName,
          t.fromAccountId, accFrom.name AS fromAccountName,
          t.toAccountId, accTo.name AS toAccountName
        FROM transactions t
        LEFT JOIN accounts acc      ON acc.id = t.accountId
        LEFT JOIN accounts accFrom  ON accFrom.id = t.fromAccountId
        LEFT JOIN accounts accTo    ON accTo.id = t.toAccountId
        WHERE t.dateMillis BETWEEN :startMillis AND :endMillis
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    suspend fun getRowsInRange(startMillis: Long, endMillis: Long): List<TransactionRow>

    @Query("""
        SELECT 
          t.id, t.type, t.dateMillis, t.amount, t.category, t.description,
          t.accountId, acc.name AS accountName,
          t.fromAccountId, accFrom.name AS fromAccountName,
          t.toAccountId, accTo.name AS toAccountName
        FROM transactions t
        LEFT JOIN accounts acc      ON acc.id = t.accountId
        LEFT JOIN accounts accFrom  ON accFrom.id = t.fromAccountId
        LEFT JOIN accounts accTo    ON accTo.id = t.toAccountId
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    suspend fun getAllRows(): List<TransactionRow>

    @Query("""
    SELECT 
      COALESCE(t.category, 'Uncategorized') AS category,
      ABS(SUM(t.amount)) AS total
    FROM transactions t
    WHERE t.type = :type
      AND t.dateMillis BETWEEN :startMillis AND :endMillis
    GROUP BY COALESCE(t.category, 'Uncategorized')
    HAVING ABS(SUM(t.amount)) > 0
    ORDER BY total DESC
""")

    suspend fun getCategoryTotalsInRange(
        type: String,           // "INCOME" or "EXPENSE"
        startMillis: Long,
        endMillis: Long
    ): List<CategoryTotal>
}
