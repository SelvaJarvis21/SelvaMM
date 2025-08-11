package com.example.selvamoneymanager.more

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.selvamoneymanager.db.CategoryEntity
import com.example.selvamoneymanager.db.CategoryType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryListScreen(
    type: CategoryType,
    vm: CategoryViewModel,
) {
    // load once per 'type' change
    LaunchedEffect(type) { vm.load(type) }

    val items by vm.items.collectAsState() // StateFlow from the updated VM
    val ctx = LocalContext.current                        // <-- MISSING BEFORE ✅

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    var actionTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var showActions by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editErr by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            Text(
                text = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} Categories",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))
            items.forEach { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .combinedClickable(
                            onClick = { /* optional: inline edit later */ },
                            onLongClick = {
                                if (c.isDefault) {
                                    Toast.makeText(
                                        ctx,
                                        "Default categories can’t be edited or deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    actionTarget = c
                                    showActions = true
                                }
                            }
                        )
                ) {
                    Text(c.name, Modifier.weight(1f))
                    if (c.isDefault) {
                        // optional badge for defaults
                        AssistChip(onClick = {}, enabled = false, label = { Text("Default") })
                    }
                }
            }
        }
    }

    // Add dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.addCategory(
                        name = name,
                        type = type,
                        onError = { err = it },
                        onDone = { name = ""; err = null; showDialog = false }
                    )
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text("New ${type.name.lowercase().replaceFirstChar { it.uppercase() }} Category") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Actions dialog (Edit / Delete)
    if (showActions && actionTarget != null) {
        AlertDialog(
            onDismissRequest = { showActions = false; actionTarget = null },
            confirmButton = { /* none */ },
            dismissButton = { TextButton(onClick = { showActions = false; actionTarget = null }) { Text("Close") } },
            title = { Text(actionTarget!!.name) },
            text = {
                Column {
                    Button(
                        onClick = {
                            showActions = false
                            editName = actionTarget!!.name
                            editErr = null
                            showEdit = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit category") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showActions = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete category") }
                }
            }
        )
    }

    // Edit dialog
    if (showEdit && actionTarget != null) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.editCategory(
                        target = actionTarget!!,
                        newName = editName,
                        onError = { editErr = it },
                        onDone = {
                            editErr = null
                            showEdit = false
                            actionTarget = null
                        }
                    )
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } },
            title = { Text("Edit category") },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                    if (editErr != null) Text(editErr!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Delete confirm
    if (showDeleteConfirm && actionTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCategory(
                        target = actionTarget!!,
                        onError = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() },
                        onDone = {
                            showDeleteConfirm = false
                            actionTarget = null
                        }
                    )
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
            title = { Text("Delete category?") },
            text = { Text("This will remove the category. Existing transactions will keep their amounts but the category will be cleared.") }
        )
    }
}
