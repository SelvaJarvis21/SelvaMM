package com.example.selvamoneymanager.trans

data class TransactionRow(
    val id: Long,
    val type: String,
    val dateMillis: Long,
    val amount: Double,
    val category: String?,
    val description: String,
    val accountId: Long?,
    val accountName: String?,        // for income/expense
    val fromAccountId: Long?,         // for transfer
    val fromAccountName: String?,    // for transfer
    val toAccountId: Long?,           // for transfer
    val toAccountName: String?       // for transfer
)