package com.example.selvamoneymanager.more

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This calls the composable defined in MoreScreen.kt
            MoreScreen(
                onOpenIncome = {
                    startActivity(
                        Intent(this, CategoryListActivity::class.java)
                            .putExtra(CategoryListActivity.EXTRA_TYPE, "INCOME")
                    )
                },
                onOpenExpense = {
                    startActivity(
                        Intent(this, CategoryListActivity::class.java)
                            .putExtra(CategoryListActivity.EXTRA_TYPE, "EXPENSE")
                    )
                }
            )
        }
    }
}
