package com.securemart.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ProductCategory(val label: String, val color: Color, val icon: ImageVector) {
    ELECTRONICS("Electronics", Color(0xFF2563EB), Icons.Filled.PhoneAndroid),
    FASHION("Fashion", Color(0xFFDB2777), Icons.Filled.Checkroom),
    HOME("Home", Color(0xFF16A34A), Icons.Filled.Home),
    BOOKS("Books", Color(0xFF7C3AED), Icons.AutoMirrored.Filled.MenuBook),
    SPORTS("Sports", Color(0xFFEA580C), Icons.Filled.SportsSoccer),
    OTHER("Other", Color(0xFF64748B), Icons.Filled.Category);

    companion object {
        fun fromName(name: String): ProductCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
