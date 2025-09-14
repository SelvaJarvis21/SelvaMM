package com.example.selvamoneymanager.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.Account
import kotlinx.coroutines.launch

class AddAccountFragment : Fragment() {

    private lateinit var etGroup: EditText
    private lateinit var etName: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etGroup = view.findViewById(R.id.etGroup)
        etName = view.findViewById(R.id.etName)
        etAmount = view.findViewById(R.id.etAmount)
        btnSave = view.findViewById(R.id.btnSave)

        val dao = AppDatabase.getDatabase(requireContext()).accountDao()

        btnSave.setOnClickListener {
            val group = etGroup.text.toString().trim()
            val name = etName.text.toString().trim()
            val amountText = etAmount.text.toString().trim()

            if (group.isEmpty() || name.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val account = Account(group = group, name = name, amount = amount)

            viewLifecycleOwner.lifecycleScope.launch {
                dao.insert(account)
                Toast.makeText(requireContext(), "Account added!", Toast.LENGTH_SHORT).show()

                // Go back to AccountsFragment
                parentFragmentManager.popBackStack()
            }
        }
    }
}
