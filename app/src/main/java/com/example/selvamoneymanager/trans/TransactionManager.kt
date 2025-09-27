package com.example.selvamoneymanager.db

class TransactionManager(
    private val txnDao: TransactionDao
) {

    // Add new transaction
    suspend fun addTransaction(txn: Transaction) {
        txnDao.insert(txn)
    }

    // Update existing transaction
    suspend fun updateTransaction(newTxn: Transaction) {
        txnDao.update(newTxn)
    }

    // Delete transaction
    suspend fun deleteTransaction(txn: Transaction) {
        txnDao.delete(txn)
    }
}
