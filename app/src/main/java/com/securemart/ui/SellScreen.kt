@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.Product
import com.securemart.data.ProductCategory
import com.securemart.data.SaleLine

@Composable
fun SellScreen(
    myProducts: List<Product>,
    sales: List<SaleLine>,
    onNew: () -> Unit,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<Product?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("Sell")
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My listings (${myProducts.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Sales (${sales.size})") })
        }
        if (tab == 0) {
            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  List a new product")
            }
            if (myProducts.isEmpty()) {
                EmptyState(Icons.Filled.Sell, "You have no listings", "Sell something you no longer need")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(myProducts, key = { it.id }) { p ->
                        ListingRow(p, onEdit = { onEdit(p) }, onDelete = { toDelete = p })
                    }
                }
            }
        } else {
            SalesList(sales)
        }
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Remove listing?") },
            text = { Text("\"${pending.title}\" will no longer be visible in the shop. Past orders are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    onDelete(pending)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ListingRow(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(
                product.imagePath,
                ProductCategory.fromName(product.category),
                Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(product.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(money(product.priceCents), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (product.stock <= 0) "Sold out" else "${product.stock} in stock",
                    fontSize = 12.sp,
                    color = if (product.stock <= 0) MaterialTheme.colorScheme.error else Color.Gray
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit listing") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove listing", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SalesList(sales: List<SaleLine>) {
    if (sales.isEmpty()) {
        EmptyState(Icons.Filled.Sell, "No sales yet", "When someone buys your product it shows up here")
        return
    }
    val itemsSold = sales.sumOf { it.quantity }
    val revenue = sales.sumOf { it.priceCents * it.quantity }
    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Items sold", fontSize = 12.sp)
                    Text(itemsSold.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Revenue", fontSize = 12.sp)
                    Text(money(revenue), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sales, key = { it.id }) { s ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${s.title} x ${s.quantity}", fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Order #${s.orderId} - ${formatDate(s.orderedAt)}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(money(s.priceCents * s.quantity), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
