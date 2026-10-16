@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.Product
import com.securemart.data.ProductCategory

@Composable
fun HomeScreen(products: List<Product>, myId: Long, onOpen: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ProductCategory?>(null) }

    val visible = products.filter { p ->
        (category == null || p.category == category?.name) &&
            (query.isBlank() ||
                p.title.contains(query.trim(), ignoreCase = true) ||
                p.description.contains(query.trim(), ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("SecureMart")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(60) },
            placeholder = { Text("Search products") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
            }
            items(ProductCategory.entries.toList()) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = if (category == c) null else c },
                    label = { Text(c.label) }
                )
            }
        }
        if (visible.isEmpty()) {
            EmptyState(Icons.Filled.Storefront, "No products found", "Try another search or category")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible, key = { it.id }) { p ->
                    ProductCard(p, isMine = p.sellerId == myId, onClick = { onOpen(p) })
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, isMine: Boolean, onClick: () -> Unit) {
    val category = ProductCategory.fromName(product.category)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            ProductImage(product.imagePath, category, Modifier.fillMaxWidth().aspectRatio(1.15f))
            Column(Modifier.padding(10.dp)) {
                Text(category.label.uppercase(), color = category.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    product.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    money(product.priceCents),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row {
                    val note = when {
                        product.stock <= 0 -> "Sold out"
                        isMine -> "Your listing"
                        product.stock <= 3 -> "Only ${product.stock} left"
                        else -> "by ${product.sellerName}"
                    }
                    val noteColor = when {
                        product.stock <= 0 -> MaterialTheme.colorScheme.error
                        isMine || product.stock <= 3 -> MaterialTheme.colorScheme.secondary
                        else -> Color.Gray
                    }
                    Text(note, fontSize = 11.sp, color = noteColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
