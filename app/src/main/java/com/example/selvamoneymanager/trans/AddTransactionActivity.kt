package com.example.selvamoneymanager.trans

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.accounts.AddAccountActivity
import com.example.selvamoneymanager.db.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

class AddTransactionActivity : AppCompatActivity() {

    // DAOs
    private lateinit var txnDao: TransactionDao
    private lateinit var accDao: AccountDao
    private lateinit var categoryDao: CategoryDao

    // UI
    private lateinit var rbIncome: RadioButton
    private lateinit var rbExpense: RadioButton
    private lateinit var rbTransfer: RadioButton
    private lateinit var rgType: RadioGroup
    private lateinit var spAccount: Spinner
    private lateinit var spCategory: Spinner
    private lateinit var spFrom: Spinner
    private lateinit var spTo: Spinner
    private lateinit var layoutIE: LinearLayout
    private lateinit var layoutTransfer: LinearLayout
    private lateinit var etDate: EditText
    private lateinit var etDesc: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnSave: Button

    // Data
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var accounts: List<Account> = emptyList()
    private var categories: List<CategoryEntity> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // DAOs
        val db = AppDatabase.getDatabase(this)
        txnDao = db.transactionDao()
        accDao = db.accountDao()
        categoryDao = db.categoryDao()

        // Bind views
        etDate = findViewById(R.id.etDate)
        spAccount = findViewById(R.id.spAccount)
        spCategory = findViewById(R.id.spCategory)
        etDesc = findViewById(R.id.etDescription)
        etAmount = findViewById(R.id.etAmount)
        btnSave = findViewById(R.id.btnSaveTxn)
        rgType = findViewById(R.id.rgType)
        rbIncome = findViewById(R.id.rbIncome)
        rbExpense = findViewById(R.id.rbExpense)
        rbTransfer = findViewById(R.id.rbTransfer)
        layoutIE = findViewById(R.id.layoutIE)
        layoutTransfer = findViewById(R.id.layoutTransfer)
        spFrom = findViewById(R.id.spFrom)
        spTo = findViewById(R.id.spTo)

        // Initial categories based on current radio
        currentType()?.let { loadCategories(it) }

        // Toggle UI
        fun applyTypeUI() {
            if (rbTransfer.isChecked) {
                layoutIE.visibility = View.GONE
                layoutTransfer.visibility = View.VISIBLE
            } else {
                layoutIE.visibility = View.VISIBLE
                layoutTransfer.visibility = View.GONE
            }
        }
        rgType.setOnCheckedChangeListener { _, _ -> applyTypeUI() }
        applyTypeUI()

        // Date picker
        fun updateDateText() {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            etDate.setText("%02d-%02d-%04d".format(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            ))
        }
        updateDateText()
        etDate.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(
                this, { _, y, m, d ->
                    val cal = Calendar.getInstance().apply {
                        set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                    }
                    selectedDateMillis = cal.timeInMillis
                    updateDateText()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Load accounts
        lifecycleScope.launch {
            accounts = accDao.getAllAccounts()
            if (accounts.isEmpty()) {
                AlertDialog.Builder(this@AddTransactionActivity)
                    .setTitle("No Accounts")
                    .setMessage("You need to add an account before creating a transaction.")
                    .setPositiveButton("Add Account") { _, _ ->
                        startActivity(Intent(this@AddTransactionActivity, AddAccountActivity::class.java))
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@launch
            } else {
                val names = accounts.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    this@AddTransactionActivity,
                    android.R.layout.simple_spinner_item,
                    names
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spAccount.adapter = spinnerAdapter
                spFrom.adapter = spinnerAdapter
                spTo.adapter = spinnerAdapter
            }
        }



        // Type change -> reload categories for that enum
        rgType.setOnCheckedChangeListener { _, checkedId ->
            applyTypeUI()
            when (checkedId) {
                R.id.rbIncome -> loadCategories(CategoryType.INCOME)
                R.id.rbExpense -> loadCategories(CategoryType.EXPENSE)
                else -> { /* transfer: no categories */ }
            }
        }

        // Save
        btnSave.setOnClickListener {
            val desc = etDesc.text.toString().trim()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            val txnTypeStr = when {
                rbTransfer.isChecked -> "TRANSFER"
                rbIncome.isChecked -> "INCOME"
                else -> "EXPENSE"
            }

            lifecycleScope.launch {
                val txn = when (txnTypeStr) {
                    "TRANSFER" -> {
                        val fromAcc = accounts.getOrNull(spFrom.selectedItemPosition)
                        val toAcc = accounts.getOrNull(spTo.selectedItemPosition)
                        if (fromAcc == null || toAcc == null || fromAcc.id == toAcc.id) {
                            Toast.makeText(this@AddTransactionActivity, "Choose different From/To accounts", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        Transaction(
                            type = "TRANSFER",
                            dateMillis = selectedDateMillis,
                            accountId = null,
                            categoryId = null,
                            description = desc,
                            amount = amount, // neutral
                            fromAccountId = fromAcc.id,
                            toAccountId = toAcc.id
                        )
                    }
                    else -> {
                        val account = accounts.getOrNull(spAccount.selectedItemPosition)
                        if (account == null) {
                            Toast.makeText(this@AddTransactionActivity, "Select an account", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val selectedCat = categories.getOrNull(spCategory.selectedItemPosition)
                        val categoryId = selectedCat?.id
                        val signedAmount = if (txnTypeStr == "EXPENSE") -abs(amount) else abs(amount)
                        Transaction(
                            type = txnTypeStr,
                            dateMillis = selectedDateMillis,
                            amount = signedAmount,
                            categoryId = categoryId,
                            description = desc,
                            accountId = account.id,
                            fromAccountId = null,
                            toAccountId = null
                        )
                    }
                }
                txnDao.insert(txn)
                finish()
            }
        }
    }

    // ---------- Helpers ----------

    private fun currentType(): CategoryType? = when {
        rbIncome.isChecked -> CategoryType.INCOME
        rbExpense.isChecked -> CategoryType.EXPENSE
        else -> null // transfer: no category list
    }




    private fun loadCategories(type: CategoryType) {
        lifecycleScope.launch {
            categories = categoryDao.categoriesByType(type)
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(
                this@AddTransactionActivity,
                android.R.layout.simple_spinner_item,
                names
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spCategory.adapter = adapter
        }
    }
}
