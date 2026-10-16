@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.payment.CardInput
import com.securemart.payment.ShippingInfo
import com.securemart.security.CardValidator

@Composable
fun CheckoutScreen(
    totalCents: Long,
    itemCount: Int,
    onBack: () -> Unit,
    onPay: (ShippingInfo, CardInput, onError: (String) -> Unit) -> Unit
) {
    // Plain remember (not rememberSaveable): card details are never written to saved instance state.
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expMonth by remember { mutableStateOf("") }
    var expYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val brand = CardValidator.brand(cardNumber)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("Checkout", onBack = { if (!busy) onBack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Demo payment - no real money is charged", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Text(
                        "Use test card 4242 4242 4242 4242, any future expiry and any 3-digit CVV. " +
                            "Card number and CVV are never saved. Only the card brand and last 4 digits are kept on your order.",
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("  Delivery details", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(60) },
                label = { Text("Recipient name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it.take(20) },
                label = { Text("Phone number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it.take(150) },
                label = { Text("Street address") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = city, onValueChange = { city = it.take(40) },
                label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("  Payment", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it.filter { c -> c.isDigit() }.take(19) },
                label = { Text("Card number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (cardNumber.isNotEmpty()) Text(brand) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = expMonth,
                    onValueChange = { expMonth = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("MM") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expYear,
                    onValueChange = { expYear = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("YY") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("CVV") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f)
                )
            }
            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }

        Column(Modifier.background(Color.White).padding(16.dp)) {
            Button(
                onClick = {
                    error = null
                    busy = true
                    onPay(
                        ShippingInfo(name, phone, address, city),
                        CardInput(cardNumber, expMonth, expYear, cvv)
                    ) { message ->
                        busy = false
                        error = message
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Pay ${money(totalCents)} for $itemCount items")
                }
            }
        }
    }
}

@Composable
fun OrderDoneScreen(orderId: Long, onContinue: () -> Unit, onViewOrders: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp)
        )
        Text("Payment successful", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Order #$orderId has been placed.", color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
        Button(onClick = onViewOrders, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("View my orders") }
        TextButton(onClick = onContinue) { Text("Continue shopping") }
    }
}
