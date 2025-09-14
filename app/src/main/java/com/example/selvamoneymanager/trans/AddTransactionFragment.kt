package com.example.selvamoneymanager.trans

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

class AddTransactionFragment : Fragment() {

    private lateinit var txnDao: TransactionDao
    private lateinit var accDao: AccountDao
    private lateinit var categoryDao: CategoryDao

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

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var accounts: List<Account> = emptyList()
    private var categories: List<CategoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        txnDao = db.transactionDao()
        accDao = db.accountDao()
        categoryDao = db.categoryDao()

        etDate = view.findViewById(R.id.etDate)
        spAccount = view.findViewById(R.id.spAccount)
        spCategory = view.findViewById(R.id.spCategory)
        etDesc = view.findViewById(R.id.etDescription)
        etAmount = view.findViewById(R.id.etAmount)
        btnSave = view.findViewById(R.id.btnSaveTxn)
        rgType = view.findViewById(R.id.rgType)
        rbIncome = view.findViewById(R.id.rbIncome)
        rbExpense = view.findViewById(R.id.rbExpense)
        rbTransfer = view.findViewById(R.id.rbTransfer)
        layoutIE = view.findViewById(R.id.layoutIE)
        layoutTransfer = view.findViewById(R.id.layoutTransfer)
        spFrom = view.findViewById(R.id.spFrom)
        spTo = view.findViewById(R.id.spTo)

        // Set initial categories
        currentType()?.let { loadCategories(it) }

        // Toggle UI based on type
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
                requireContext(),
                { _, y, m, d ->
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
        viewLifecycleOwner.lifecycleScope.launch {
            accounts = accDao.getAllAccounts()
            if (accounts.isEmpty()) {
                Toast.makeText(requireContext(), "No accounts. Add one first!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            } else {
                val names = accounts.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spAccount.adapter = spinnerAdapter
                spFrom.adapter = spinnerAdapter
                spTo.adapter = spinnerAdapter
            }
        }

        // Reload categories when type changes
        rgType.setOnCheckedChangeListener { _, checkedId ->
            applyTypeUI()
            when (checkedId) {
                R.id.rbIncome -> loadCategories(CategoryType.INCOME)
                R.id.rbExpense -> loadCategories(CategoryType.EXPENSE)
            }
        }

        // Save transaction
        btnSave.setOnClickListener {
            val desc = etDesc.text.toString().trim()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            val txnTypeStr = when {
                rbTransfer.isChecked -> "TRANSFER"
                rbIncome.isChecked -> "INCOME"
                else -> "EXPENSE"
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val txn = when (txnTypeStr) {
                    "TRANSFER" -> {
                        val fromAcc = accounts.getOrNull(spFrom.selectedItemPosition)
                        val toAcc = accounts.getOrNull(spTo.selectedItemPosition)
                        if (fromAcc == null || toAcc == null || fromAcc.id == toAcc.id) {
                            Toast.makeText(requireContext(), "Choose different From/To accounts", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        Transaction(
                            type = "TRANSFER",
                            dateMillis = selectedDateMillis,
                            accountId = null,
                            categoryId = null,
                            description = desc,
                            amount = amount,
                            fromAccountId = fromAcc.id,
                            toAccountId = toAcc.id
                        )
                    }
                    else -> {
                        val account = accounts.getOrNull(spAccount.selectedItemPosition)
                        if (account == null) {
                            Toast.makeText(requireContext(), "Select an account", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Transaction added!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun currentType(): CategoryType? = when {
        rbIncome.isChecked -> CategoryType.INCOME
        rbExpense.isChecked -> CategoryType.EXPENSE
        else -> null
    }

    private fun loadCategories(type: CategoryType) {
        viewLifecycleOwner.lifecycleScope.launch {
            categories = categoryDao.categoriesByType(type)
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                names
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spCategory.adapter = adapter
        }
    }
}
