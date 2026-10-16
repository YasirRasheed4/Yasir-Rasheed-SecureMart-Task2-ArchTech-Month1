@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.Product
import com.securemart.data.ProductCategory

@Composable
fun ProductDetailScreen(
    product: Product,
    isOwner: Boolean,
    onBack: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onEdit: () -> Unit
) {
    val category = ProductCategory.fromName(product.category)
    var quantity by remember { mutableIntStateOf(1) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("Product details", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ProductImage(product.imagePath, category, Modifier.fillMaxWidth().height(280.dp))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(category.label.uppercase(), color = category.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(product.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    money(product.priceCents),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                InfoRow(Icons.Filled.Person, "Sold by ${product.sellerName}")
                val stockText = when {
                    product.stock <= 0 -> "Sold out"
                    product.stock <= 3 -> "Only ${product.stock} left in stock"
                    else -> "${product.stock} in stock"
                }
                Text(
                    stockText,
                    color = if (product.stock <= 0) MaterialTheme.colorScheme.error else Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
                Text("About this item", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text(product.description, color = Color.DarkGray)

                Spacer(Modifier.height(6.dp))
                if (isOwner) {
                    Text("This is your listing.", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("  Edit listing")
                    }
                } else if (product.stock > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        QuantityStepper(
                            quantity = quantity,
                            max = minOf(product.stock, 20),
                            onChange = { quantity = it }
                        )
                    }
                    Button(onClick = { onAddToCart(quantity) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                        Text("  Add to cart")
                    }
                }
                InfoRow(Icons.Filled.Lock, "Secure checkout. Your card number and CVV are never stored.")
            }
        }
    }
}
