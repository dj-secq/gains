package com.example.repsgrams.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun RestockDialog(
    name: String,
    initialServings: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var servings by remember(name, initialServings) { mutableStateOf(initialServings) }
    val parsed = servings.toIntOrNull()?.takeIf { it > 0 }
    BoardDialog(
        title = "Restock $name",
        onDismiss = onDismiss,
        confirmText = "Restock",
        onConfirm = { parsed?.let(onConfirm) },
        confirmEnabled = parsed != null,
    ) {
        OutlinedTextField(
            value = servings,
            onValueChange = { next -> if (next.all(Char::isDigit)) servings = next },
            label = { Text("Total servings") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}
