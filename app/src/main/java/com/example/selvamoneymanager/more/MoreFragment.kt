package com.example.selvamoneymanager.more

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.ui.more.CategoryListFragment
import com.example.selvamoneymanager.ui.theme.SelvaMoneyManagerTheme
import com.example.selvamoneymanager.util.BackupUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.selvamoneymanager.util.CsvImportUtil
import com.example.selvamoneymanager.util.CsvImportResult


class MoreFragment : Fragment() {

    private val csvPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            lifecycleScope.launch {
                val res = importCsvFromUri(uri)
                val msg = buildString {
                    append("CSV Import → Imported ${res.imported}/${res.totalRows}")
                    if (res.skipped > 0) append(" (skipped ${res.skipped})")
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                if (res.errors.isNotEmpty()) {
                    // Keep toast short; log details for dev
                    android.util.Log.w("CSV_IMPORT", res.errors.joinToString("\n"))
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SelvaMoneyManagerTheme {
                    MoreScreen(
                        onOpenIncome = {
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    CategoryListFragment.newInstance("INCOME")
                                )
                                .addToBackStack(null)
                                .commit()
                        },
                        onOpenExpense = {
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    CategoryListFragment.newInstance("EXPENSE")
                                )
                                .addToBackStack(null)
                                .commit()
                        },
                        onBackup = {
                            lifecycleScope.launch {
                                val file = BackupUtil.exportBackup(requireContext())
                                Toast.makeText(
                                    context,
                                    if (file != null) "Backup saved: ${file.name}" else "Backup failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onRestore = {
                            // Launch file picker for ".mmbak"
                            restoreFilePicker.launch("*/*")
                        },
                        onImportCsv = {
                            // open a file picker for CSV
                            csvPicker.launch("text/*") // or "text/csv" (some OEMs are picky)
                        }
                    )
                }
            }
        }
    }
    // File picker launcher for restore

    private val restoreFilePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                lifecycleScope.launch {
                    val result = restoreFromUri(it)
                    Toast.makeText(
                        context,
                        if (result) "Restore successful. Restarting app..." else "Restore failed",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (result) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            kotlin.system.exitProcess(0)
                        }, 1500)
                    }
                }
            }
        }


    // Helper: copy picked Uri to a File and restore
    private suspend fun restoreFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val backupDir = requireContext().cacheDir
            val tempFile = File(backupDir, "restore_temp.mmbak")

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            BackupUtil.importBackup(requireContext(), tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun importCsvFromUri(uri: Uri): CsvImportResult =
        withContext(Dispatchers.IO) {
            val input = requireContext().contentResolver.openInputStream(uri)
                ?: return@withContext CsvImportResult(0,0,0, listOf("Unable to open CSV"))
            val tempFile = File(requireContext().cacheDir, "import_temp.csv")
            input.use { inp -> tempFile.outputStream().use { out -> inp.copyTo(out) } }
            return@withContext CsvImportUtil.importTransactionsCsv(requireContext(), tempFile)
        }

}
