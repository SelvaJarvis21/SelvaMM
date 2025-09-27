package com.example.selvamoneymanager.util

import android.content.Context
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.Transaction
import com.example.selvamoneymanager.db.TransactionType
import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CsvImportResult(
    val totalRows: Int,
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

/**
 * CSV v1 schema (header required, case-insensitive):
 * type,amount,account_id,category_id,date,description
 * - type: INCOME | EXPENSE (TRANSFER optional – ignored for v1 import)
 * - amount: decimal
 * - account_id: Long (id from accounts table)
 * - category_id: Int  (id from categories table)
 * - date: ISO "yyyy-MM-dd"  (e.g., 2025-09-27)
 * - description: optional
 */
object CsvImportUtil {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    suspend  fun importTransactionsCsv(context: Context, csvFile: File): CsvImportResult {
        val db = AppDatabase.getDatabase(context)
        val txnDao = db.transactionDao()

        var total = 0
        var ok = 0
        val errors = mutableListOf<String>()
        val toInsert = mutableListOf<Transaction>()

        CSVReaderBuilder(FileReader(csvFile)).withSkipLines(0).build().use { reader ->
            val all = reader.readAll()
            if (all.isEmpty()) return CsvImportResult(0,0,0, listOf("CSV empty"))

            // Map headers (case-insensitive)
            val header = all.first().map { it.trim().lowercase(Locale.US) }
            val body = all.drop(1)

            fun col(name: String): Int {
                val idx = header.indexOf(name)
                if (idx == -1) throw IllegalArgumentException("Missing column: $name")
                return idx
            }

            val idxType = col("type")
            val idxAmount = col("amount")
            val idxAccountId = col("account_id")
            val idxCategoryId = col("category_id")
            val idxDate = col("date")
            val idxDesc = header.indexOf("description") // optional

            body.forEachIndexed { i, row ->
                if (row.isEmpty() || row.all { it.isBlank() }) return@forEachIndexed
                total++

                try {
                    val typeStr = row.getOrNull(idxType)?.trim().orEmpty().uppercase(Locale.US)
                    val txnType = when (typeStr) {
                        "INCOME" -> TransactionType.INCOME
                        "EXPENSE" -> TransactionType.EXPENSE
                        // Ignore/skip TRANSFER in v1 import (or handle later)
                        "TRANSFER" -> throw IllegalArgumentException("TRANSFER not supported in CSV v1 import")
                        else -> throw IllegalArgumentException("Invalid type '$typeStr'")
                    }

                    val amount = row.getOrNull(idxAmount)?.trim()?.toDouble()
                        ?: throw IllegalArgumentException("Invalid amount")

                    val accountId = row.getOrNull(idxAccountId)?.trim()?.toLong()
                        ?: throw IllegalArgumentException("Invalid account_id")

                    val categoryId = row.getOrNull(idxCategoryId)?.trim()?.toInt()
                        ?: throw IllegalArgumentException("Invalid category_id")

                    val dateStr = row.getOrNull(idxDate)?.trim()
                        ?: throw IllegalArgumentException("Missing date")
                    val localDate = LocalDate.parse(dateStr, dateFmt)
                    val dateMillis = localDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()

                    val description = if (idxDesc >= 0) row.getOrNull(idxDesc)?.trim().orEmpty() else ""

                    // Build Transaction (adjust to your actual data class fields)
                    val tx = Transaction(
                        // id = 0 (auto)
                        type = txnType,
                        dateMillis = dateMillis,
                        amount = amount,
                        description = description,
                        accountId = accountId,
                        categoryId = categoryId,
                        fromAccountId = null,
                        toAccountId = null
                    )
                    toInsert.add(tx)
                    ok++
                } catch (e: Exception) {
                    errors.add("Row ${i + 2}: ${e.message ?: "parse error"}")
                }
            }
        }

        // Insert directly (suspend-safe)
        if (toInsert.isNotEmpty()) {
            txnDao.insertAll(toInsert)
        }

        return CsvImportResult(
            totalRows = total,
            imported = ok,
            skipped = total - ok,
            errors = errors
        )
    }
}
