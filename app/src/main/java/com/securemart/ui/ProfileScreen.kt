@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.User

@Composable
fun ProfileScreen(user: User, listingCount: Int, orderCount: Int, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("Profile")
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.name.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(user.email, color = Color.Gray)
                    Text("Member since ${formatDate(user.createdAt)}", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Listings", listingCount.toString(), Modifier.weight(1f))
                StatCard("Orders", orderCount.toString(), Modifier.weight(1f))
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("  How your data is protected", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    InfoRow(Icons.Filled.Lock, "Password stored as a salted PBKDF2 hash, never in plain text.")
                    InfoRow(Icons.Filled.Lock, "Account locks for 5 minutes after 5 wrong passwords.")
                    InfoRow(Icons.Filled.Lock, "Signed out automatically after 30 minutes away from the app.")
                    InfoRow(Icons.Filled.Lock, "Card number and CVV are never saved. Only brand and last 4 digits.")
                    InfoRow(Icons.Filled.Lock, "No internet permission and backups disabled: your data stays on this phone.")
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Text("  Log out")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 13.sp)
        }
    }
}
