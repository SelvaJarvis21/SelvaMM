package com.example.selvamoneymanager.trans

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.accounts.Account
import com.example.selvamoneymanager.accounts.AccountDao
import com.example.selvamoneymanager.accounts.AddAccountActivity
import com.example.selvamoneymanager.AppDatabase
import com.example.selvamoneymanager.R
import kotlinx.coroutines.launch
import java.util.Calendar
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.LinearLayout
import android.view.View






class AddTransactionActivity : AppCompatActivity() {

    private lateinit var txnDao: TransactionDao
    private lateinit var accDao: AccountDao

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var accounts: List<Account> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        txnDao = AppDatabase.Companion.getDatabase(this).transactionDao()
        accDao = AppDatabase.Companion.getDatabase(this).accountDao()

        val etDate = findViewById<EditText>(R.id.etDate)
        val spAccount = findViewById<Spinner>(R.id.spAccount)
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        val etDesc = findViewById<EditText>(R.id.etDescription)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnSave = findViewById<Button>(R.id.btnSaveTxn)
        val rgType = findViewById<RadioGroup>(R.id.rgType)
        val rbIncome = findViewById<RadioButton>(R.id.rbIncome)
        val rbExpense = findViewById<RadioButton>(R.id.rbExpense)
        val rbTransfer = findViewById<RadioButton>(R.id.rbTransfer)
        val layoutIE = findViewById<LinearLayout>(R.id.layoutIE)
        val layoutTransfer = findViewById<LinearLayout>(R.id.layoutTransfer)
        val spFrom = findViewById<Spinner>(R.id.spFrom)
        val spTo = findViewById<Spinner>(R.id.spTo)

        // toggle UI
        fun applyTypeUI() {
            when {
                rbTransfer.isChecked -> { layoutIE.visibility = View.GONE; layoutTransfer.visibility = View.VISIBLE }
                else -> { layoutIE.visibility = View.VISIBLE; layoutTransfer.visibility = View.GONE }
            }
        }
        rgType.setOnCheckedChangeListener { _, _ -> applyTypeUI() }
        applyTypeUI()


        // --- Date picker ---
        fun updateDateText() {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            etDate.setText(
                "%02d-%02d-%04d".format(
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR)
                )
            )
        }
        updateDateText()

        etDate.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(
                this@AddTransactionActivity,
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply {
                        set(y, m, d, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedDateMillis = cal.timeInMillis
                    updateDateText()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // --- Categories spinner (static list for now) ---
        val categories = listOf("Food", "Travel", "Bills", "Shopping", "Salary", "Other")
        val catAdapter = ArrayAdapter(
            this@AddTransactionActivity,
            android.R.layout.simple_spinner_item,
            categories
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spCategory.adapter = catAdapter

// Load accounts (reuse your existing code & adapter)
        lifecycleScope.launch {
            accounts = accDao.getAllAccounts()
            if (accounts.isEmpty()) {
                AlertDialog.Builder(this@AddTransactionActivity)
                    .setTitle("No Accounts")
                    .setMessage("You need to add an account before creating a transaction.")
                    .setPositiveButton("Add Account") { _, _ ->
                        startActivity(
                            Intent(
                                this@AddTransactionActivity,
                                AddAccountActivity::class.java
                            )
                        )
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@launch
            } else {
                val names = accounts.map { it.name }
                val spinnerAdapter  = ArrayAdapter(
                    this@AddTransactionActivity,
                    android.R.layout.simple_spinner_item,
                    names
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spAccount.adapter = spinnerAdapter
                spFrom.adapter = spinnerAdapter
                spTo.adapter = spinnerAdapter

            }
        }

        // --- Save transaction ---
        btnSave.setOnClickListener {
            val selAccount = accounts.getOrNull(spAccount.selectedItemPosition)
            val category = spCategory.selectedItem?.toString() ?: "Other"
            val desc = etDesc.text.toString().trim()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val isTransfer = rbTransfer.isChecked
            val isIncome = rbIncome.isChecked
            val type = when {
                isTransfer -> "TRANSFER"
                isIncome -> "INCOME"
                else -> "EXPENSE"
            }
            if (selAccount == null) {
                Toast.makeText(this@AddTransactionActivity, "Please add an account first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val txn = when (type) {
                    "TRANSFER" -> {
                        val fromAcc = accounts.getOrNull(spFrom.selectedItemPosition)
                        val toAcc = accounts.getOrNull(spTo.selectedItemPosition)
                        if (fromAcc == null || toAcc == null || fromAcc.id == toAcc.id) {
                            Toast.makeText(
                                this@AddTransactionActivity,
                                "Choose different From/To accounts",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        Transaction(
                            type = "TRANSFER",
                            dateMillis = selectedDateMillis,
                            accountId = null,
                            category = null,
                            description = desc,
                            amount = amount,
                            fromAccountId = fromAcc.id,
                            toAccountId = toAcc.id
                        )
                    }

                    "INCOME", "EXPENSE" -> {
                        val account = accounts.getOrNull(spAccount.selectedItemPosition)
                        if (account == null) {
                            Toast.makeText(
                                this@AddTransactionActivity,
                                "Select an account",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        val cat = spCategory.selectedItem?.toString()
                        // convention: store expenses as negative amounts (or keep sign as entered—your call)
                        val signedAmount =
                            if (type == "EXPENSE") -kotlin.math.abs(amount) else kotlin.math.abs(
                                amount
                            )

                        Transaction(
                            type = type,
                            dateMillis = selectedDateMillis,
                            amount = signedAmount,
                            category = cat,
                            description = desc,
                            accountId = account.id,
                            fromAccountId = null,
                            toAccountId = null
                        )
                    }

                    else -> error("Unknown type")
                }
                txnDao.insert(txn)
                finish() // return to TransactionsActivity
            }
        }
    }
}