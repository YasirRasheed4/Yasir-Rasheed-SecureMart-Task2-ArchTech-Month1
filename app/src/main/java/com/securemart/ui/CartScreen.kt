@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.CartLine
import com.securemart.data.ProductCategory

@Composable
fun CartScreen(
    lines: List<CartLine>,
    onQuantity: (CartLine, Int) -> Unit,
    onRemove: (CartLine) -> Unit,
    onCheckout: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("My cart")
        if (lines.isEmpty()) {
            EmptyState(Icons.Filled.ShoppingCart, "Your cart is empty", "Browse the shop and add something you like")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lines, key = { it.cartId }) { line ->
                    CartRow(line, onQuantity = { onQuantity(line, it) }, onRemove = { onRemove(line) })
                }
            }
            val total = lines.sumOf { it.priceCents * it.quantity }
            val itemCount = lines.sumOf { it.quantity }
            val problem = lines.any { it.stock <= 0 || it.quantity > it.stock }
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total ($itemCount items)", fontSize = 15.sp)
                        Text(
                            money(total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (problem) {
                        Text(
                            "Some items are no longer available in that quantity. Please update your cart.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = onCheckout,
                        enabled = !problem,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Secure checkout")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartRow(line: CartLine, onQuantity: (Int) -> Unit, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(
                line.imagePath,
                ProductCategory.fromName(line.category),
                Modifier.size(76.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(line.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(money(line.priceCents), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                QuantityStepper(quantity = line.quantity, max = minOf(line.stock, 20), onChange = onQuantity)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove from cart", tint = Color.Gray)
            }
        }
    }
}
