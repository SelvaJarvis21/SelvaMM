package com.example.selvamoneymanager.trans

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.selvamoneymanager.accounts.Account   // ✅ import the moved Account

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["fromAccountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["fromAccountId"]), Index(value = ["toAccountId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,                // "INCOME", "EXPENSE", "TRANSFER"
    val dateMillis: Long,
    val accountId: Int?,     // For income/expense
    val category: String?,    // only for income/expense
    val description: String,
    val amount: Double,

    // For transfers
    val fromAccountId: Int?,
    val toAccountId: Int?

)
