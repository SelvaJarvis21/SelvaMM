package com.example.selvamoneymanager.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.selvamoneymanager.db.Account

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: Account)
    @Query("SELECT * FROM accounts")

    suspend fun getAllAccounts(): List<Account>

    @Delete

    suspend fun deleteAccount(account: Account)

}