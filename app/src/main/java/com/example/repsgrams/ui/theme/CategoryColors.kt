package com.example.repsgrams.ui.theme

import androidx.compose.ui.graphics.Color

object CategoryColors {
    val Categories = listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full Body", "Core", "Custom")

    fun getColor(category: String): Color {
        return when (category) {
            "Push" -> Color(0xFFFF9800)
            "Pull" -> Color(0xFF2196F3)
            "Legs" -> Color(0xFF4CAF50)
            "Upper" -> Color(0xFFF44336)
            "Lower" -> Color(0xFF9C27B0)
            "Full Body" -> Color(0xFF009688)
            "Core" -> Color(0xFFFFEB3B)
            else -> Color(0xFF9E9E9E)
        }
    }
}
