@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securemart.data.Product
import com.securemart.data.ProductCategory
import com.securemart.data.ProductFormData

/** Used for both "List a product" (existing == null) and "Edit listing". */
@Composable
fun ProductFormScreen(
    existing: Product?,
    onBack: () -> Unit,
    onSave: (ProductFormData, done: (String?) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var price by remember {
        mutableStateOf(
            if (existing == null) "" else String.format(java.util.Locale.US, "%d.%02d", existing.priceCents / 100, existing.priceCents % 100)
        )
    }
    var stock by remember { mutableStateOf(existing?.stock?.toString() ?: "1") }
    var category by remember { mutableStateOf(ProductCategory.fromName(existing?.category ?: "OTHER")) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(if (existing == null) "List a product" else "Edit listing", onBack = { if (!busy) onBack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photoModifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            val uri = pickedUri
            when {
                uri != null -> PickedImage(uri, photoModifier)
                existing?.imagePath != null -> ProductImage(existing?.imagePath, category, photoModifier)
                else -> Box(
                    modifier = photoModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Tap to add a photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Photos are resized and location data is removed before they are saved.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            OutlinedTextField(
                value = title, onValueChange = { title = it.take(80) },
                label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it.take(600) },
                label = { Text("Description") }, minLines = 3,
                supportingText = { Text("${description.length}/600") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it.take(10) },
                    label = { Text("Price (Rs)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("In stock") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Category", fontWeight = FontWeight.Medium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                }
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
                    onSave(
                        ProductFormData(title, description, price, stock, category.name, pickedUri)
                    ) { message ->
                        busy = false
                        error = message
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (existing == null) "Publish listing" else "Save changes")
                }
            }
        }
    }
}
