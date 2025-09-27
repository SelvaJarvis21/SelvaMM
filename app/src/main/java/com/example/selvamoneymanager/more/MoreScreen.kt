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
    onOpenExpense: () -> Unit
) {
    SelvaMoneyManagerTheme {
        MoreScreen(onOpenIncome, onOpenExpense)
    }
}

@Composable
fun MoreScreen(
    onOpenIncome: () -> Unit,
    onOpenExpense: () -> Unit
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
        }
    }
}
