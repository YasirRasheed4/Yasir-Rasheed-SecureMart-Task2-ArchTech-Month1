@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.OrderWithItems

@Composable
fun OrdersScreen(orders: List<OrderWithItems>) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("My orders")
        if (orders.isEmpty()) {
            EmptyState(Icons.Filled.Receipt, "No orders yet", "Items you buy will appear here")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders, key = { it.order.id }) { entry -> OrderCard(entry) }
            }
        }
    }
}

@Composable
private fun OrderCard(entry: OrderWithItems) {
    val order = entry.order
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                Text(
                    order.status,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(formatDateTime(order.createdAt), fontSize = 12.sp, color = Color.Gray)
            HorizontalDivider()
            entry.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${item.title} x ${item.quantity}",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(money(item.priceCents * item.quantity), fontSize = 14.sp)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.SemiBold)
                Text(money(order.totalCents), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Paid with ${order.cardBrand} ending ${order.cardLast4}  -  Ref ${order.paymentRef}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text("Deliver to: ${order.shipName}, ${order.shipAddress}, ${order.shipCity}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
