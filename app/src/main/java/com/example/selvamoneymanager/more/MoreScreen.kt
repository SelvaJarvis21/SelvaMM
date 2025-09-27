package com.example.selvamoneymanager.more

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.selvamoneymanager.ui.theme.SelvaMoneyManagerTheme

@Composable
fun MoreScreenWrapper(
    onOpenIncome: () -> Unit,
    onOpenExpense: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onImportCsv: () -> Unit
) {
    SelvaMoneyManagerTheme {
        MoreScreen(onOpenIncome, onOpenExpense, onBackup, onRestore, onImportCsv)
    }
}

@Composable
fun MoreScreen(
    onOpenIncome: () -> Unit,
    onOpenExpense: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onImportCsv: () -> Unit
) {
    Scaffold { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            Text("More", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenIncome, modifier = Modifier.fillMaxWidth()) {
                Text("Income Category")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenExpense, modifier = Modifier.fillMaxWidth()) {
                Text("Expense Category")
            }
            Spacer(Modifier.height(24.dp))

            // Backup & Restore buttons
            Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup Database")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text("Restore Database")
            }
            Button(onClick = onImportCsv) { Text("Import CSV") }
        }
    }
}
