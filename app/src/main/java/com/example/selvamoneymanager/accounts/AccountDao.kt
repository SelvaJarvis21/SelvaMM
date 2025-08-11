package com.example.selvamoneymanager.accounts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AccountDao {

    @Insert
    suspend fun insertAccount(account: Account)
    @Query("SELECT * FROM accounts")

    suspend fun getAllAccounts(): List<Account>

    @Delete

    suspend fun deleteAccount(account: Account)

}