package com.example.selvamoneymanager.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["group_name"]) // note: quoted to avoid SQL keyword issues
        // If you want to prevent duplicate names per group, use:
        // Index(value = ["group_name", "name"], unique = true)
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Keep the column name as `group` for compatibility, but quote it so Room always escapes it.
    @ColumnInfo(name = "group_name")
    val group: String,                 // e.g., "Cash", "Card", "Savings"

    @ColumnInfo(name = "name")
    val name: String,                  // e.g., "RBO", "Cash Wallet"

    @ColumnInfo(name = "amount")
    val amount: Double,                // Main balance (consider minor units later)

    @ColumnInfo(name = "balance_payable")
    val balancePayable: Double = 0.0,  // For credit cards (optional)

    @ColumnInfo(name = "outstanding_balance")
    val outstandingBalance: Double = 0.0 // For cards/loans (optional)
)
