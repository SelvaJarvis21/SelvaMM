package com.example.selvamoneymanager.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    val group: String,              // e.g., "Cash", "Card", "Savings"
    val name: String,               // e.g., "RBO", "Cash Wallet"
    val amount: Double,             // Main balance
    val balancePayable: Double = 0.0,    // For credit cards (optional)
    val outstandingBalance: Double = 0.0, // For cards/loans (optional)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)