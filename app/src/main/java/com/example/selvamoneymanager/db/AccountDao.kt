package com.example.selvamoneymanager.db

import androidx.room.*

@Dao
interface AccountDao {

    // Insert account and return its ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    // Update account fields (e.g., name, balance)
    @Update
    suspend fun update(account: Account)

    // Delete account
    @Delete
    suspend fun delete(account: Account)

    // Get all accounts sorted by name
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAllAccounts(): List<Account>

    // Get account by ID
    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): Account?

    // Get accounts by group (Cash, Card, Savings, etc.)
    @Query("SELECT * FROM accounts WHERE group_name = :group ORDER BY name ASC")
    suspend fun getAccountsByGroup(group: String): List<Account>
}
