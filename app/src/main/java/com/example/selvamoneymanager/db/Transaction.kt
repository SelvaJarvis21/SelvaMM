package com.example.selvamoneymanager.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class, // NEW: link to category table
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["fromAccountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["categoryId"]) // NEW: index for category
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,                // "INCOME", "EXPENSE", "TRANSFER"
    val dateMillis: Long,
    val accountId: Int?,              // For income/expense
    val categoryId: Int?,              // NEW: links to CategoryEntity
    val description: String,
    val amount: Double,

    // For transfers
    val fromAccountId: Int?,
    val toAccountId: Int?
)
