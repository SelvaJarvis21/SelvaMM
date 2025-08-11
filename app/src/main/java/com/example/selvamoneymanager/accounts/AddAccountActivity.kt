package com.example.selvamoneymanager.accounts

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.AppDatabase
import com.example.selvamoneymanager.R
import kotlinx.coroutines.launch

class AddAccountActivity : AppCompatActivity() {
    private var accountId: Int? = null
    private lateinit var dao: AccountDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        val etGroup = findViewById<EditText>(R.id.etGroup)
        val etName = findViewById<EditText>(R.id.etName)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnSave = findViewById<Button>(R.id.btnSave)

        dao = AppDatabase.Companion.getDatabase(this).accountDao()

        // Load existing data for editing
        accountId = intent.getIntExtra("id", -1).takeIf { it != -1 }

        etGroup.setText(intent.getStringExtra("group") ?: "")
        etName.setText(intent.getStringExtra("name") ?: "")

        val amount = intent.getDoubleExtra("amount", 0.0)
        etAmount.setText(String.format("%.2f", amount)) // ✅ Ensure it's formatted and converted to string

        btnSave.setOnClickListener {
            val group = etGroup.text.toString()
            val name = etName.text.toString()
            val amountInput = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            lifecycleScope.launch {
                val account = Account(
                    group = group,
                    name = name,
                    amount = amountInput,
                    id = accountId ?: 0
                )
                dao.insertAccount(account)

                setResult(RESULT_OK)
                finish()
            }
        }

    }
}
