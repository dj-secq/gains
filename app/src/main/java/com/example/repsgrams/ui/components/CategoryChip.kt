package com.example.repsgrams.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.repsgrams.ui.theme.CategoryColors

@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val accent = CategoryColors.getColor(category)
    Surface(modifier = modifier, color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp)) {
        Text(
            text = category.ifBlank { "Custom" },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (category == "Core") Color(0xFF746500) else accent,
        )
    }
}
