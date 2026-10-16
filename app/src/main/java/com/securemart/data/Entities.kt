package com.securemart.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A registered account. Only a salted PBKDF2 hash of the password is stored. */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** A product listed for sale by a user. Prices are stored in cents to avoid rounding errors. */
@Entity(tableName = "products", indices = [Index(value = ["sellerId"])])
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sellerId: Long,
    val sellerName: String,
    val title: String,
    val description: String,
    val priceCents: Long,
    val category: String,
    val stock: Int,
    val imagePath: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cart_items",
    indices = [Index(value = ["userId", "productId"], unique = true)]
)
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val quantity: Int
)

/** A paid order. Card number and CVV are never stored - only brand, last 4 digits and a payment reference. */
@Entity(tableName = "orders", indices = [Index(value = ["buyerId"])])
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buyerId: Long,
    val totalCents: Long,
    val shipName: String,
    val shipPhone: String,
    val shipAddress: String,
    val shipCity: String,
    val cardBrand: String,
    val cardLast4: String,
    val paymentRef: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Snapshot of a purchased product (title and price at the time of purchase). */
@Entity(
    tableName = "order_items",
    indices = [Index(value = ["orderId"]), Index(value = ["sellerId"])]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val sellerId: Long,
    val title: String,
    val priceCents: Long,
    val quantity: Int
)
