package com.example.selvamoneymanager.db

data class AccountBalanceResult(
    val accountId: Int,
    val income: Double,
    val expense: Double,
    val transferIn: Double,
    val transferOut: Double
)