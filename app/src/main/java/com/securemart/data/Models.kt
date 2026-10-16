package com.securemart.data

import android.net.Uri

/** A cart row joined with its product (read-only view). */
data class CartLine(
    val cartId: Long,
    val productId: Long,
    val title: String,
    val priceCents: Long,
    val quantity: Int,
    val stock: Int,
    val imagePath: String?,
    val category: String,
    val sellerId: Long
)

/** One sold line shown to the seller. The buyer's identity is deliberately not exposed. */
data class SaleLine(
    val id: Long,
    val orderId: Long,
    val title: String,
    val priceCents: Long,
    val quantity: Int,
    val orderedAt: Long
)

data class OrderWithItems(val order: Order, val items: List<OrderItem>)

data class ProductFormData(
    val title: String,
    val description: String,
    val price: String,
    val stock: String,
    val category: String,
    val imageUri: Uri?
)

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class CheckoutResult {
    data class Success(val orderId: Long) : CheckoutResult()
    data class Error(val message: String) : CheckoutResult()
}
