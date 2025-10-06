package com.example.selvamoneymanager.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.selvamoneymanager.ui.theme.SelvaMoneyManagerTheme
import com.example.selvamoneymanager.util.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun MoreScreenWrapper(
    onOpenIncome: () -> Unit,
    onOpenExpense: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onImportCsv: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode = ThemePreference.getThemeMode(context)
        .collectAsState(initial = false)

    SelvaMoneyManagerTheme(darkTheme = isDarkMode.value) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            MoreScreen(
                onOpenIncome,
                onOpenExpense,
                onBackup,
                onRestore,
                onImportCsv
            )
        }
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode = ThemePreference.getThemeMode(context).collectAsState(initial = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Text(
                text = "More",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            // Categories
            ThemedButton(text = "Income Category", onClick = onOpenIncome)
            Spacer(Modifier.height(12.dp))
            ThemedButton(text = "Expense Category", onClick = onOpenExpense)

            Spacer(Modifier.height(24.dp))

            // Backup & Restore
            ThemedButton(text = "Backup Database", onClick = onBackup)
            Spacer(Modifier.height(12.dp))
            ThemedButton(text = "Restore Database", onClick = onRestore)
            Spacer(Modifier.height(12.dp))
            ThemedButton(text = "Import CSV", onClick = onImportCsv)

            Spacer(Modifier.height(32.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))

            // 🌙 Theme toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = isDarkMode.value,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            ThemePreference.setDarkMode(context, enabled)
                            ThemePreference.applyTheme(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

@Composable
private fun ThemedButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text)
    }
}
