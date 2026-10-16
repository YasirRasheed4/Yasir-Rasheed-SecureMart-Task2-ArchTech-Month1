package com.securemart.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** All SQL uses bound parameters (:name) - user input is never concatenated into a query. */

const val CART_LINES_SQL = """
    SELECT c.id AS cartId, p.id AS productId, p.title AS title, p.priceCents AS priceCents,
           c.quantity AS quantity, p.stock AS stock, p.imagePath AS imagePath,
           p.category AS category, p.sellerId AS sellerId
    FROM cart_items c INNER JOIN products p ON p.id = c.productId
    WHERE c.userId = :userId AND p.active = 1
    ORDER BY c.id
"""

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): User?

    @Query("UPDATE users SET failedAttempts = :failed, lockedUntil = :lockedUntil WHERE id = :id")
    suspend fun setLockState(id: Long, failed: Int, lockedUntil: Long)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId AND active = 1 ORDER BY createdAt DESC")
    fun observeBySeller(sellerId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Product?

    @Query("SELECT * FROM products WHERE sellerId = :sellerId")
    suspend fun allBySeller(sellerId: Long): List<Product>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    /** Atomic stock check + decrement: returns 0 rows if there is not enough stock. */
    @Query("UPDATE products SET stock = stock - :qty WHERE id = :id AND active = 1 AND stock >= :qty")
    suspend fun decrementStock(id: Long, qty: Int): Int
}

@Dao
interface CartDao {
    @Query(CART_LINES_SQL)
    fun observeLines(userId: Long): Flow<List<CartLine>>

    @Query(CART_LINES_SQL)
    suspend fun lines(userId: Long): List<CartLine>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun find(userId: Long, productId: Long): CartItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItem): Long

    @Query("UPDATE cart_items SET quantity = :qty WHERE id = :id AND userId = :userId")
    suspend fun setQuantity(id: Long, userId: Long, qty: Int): Int

    @Query("DELETE FROM cart_items WHERE id = :id AND userId = :userId")
    suspend fun remove(id: Long, userId: Long): Int

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: Order): Long

    @Insert
    suspend fun insertItems(items: List<OrderItem>)

    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY createdAt DESC")
    fun observeOrders(buyerId: Long): Flow<List<Order>>

    @Query(
        "SELECT oi.* FROM order_items oi INNER JOIN orders o ON o.id = oi.orderId WHERE o.buyerId = :buyerId"
    )
    fun observeItemsForBuyer(buyerId: Long): Flow<List<OrderItem>>

    @Query(
        """SELECT oi.id AS id, oi.orderId AS orderId, oi.title AS title, oi.priceCents AS priceCents,
                  oi.quantity AS quantity, o.createdAt AS orderedAt
           FROM order_items oi INNER JOIN orders o ON o.id = oi.orderId
           WHERE oi.sellerId = :sellerId ORDER BY o.createdAt DESC"""
    )
    fun observeSales(sellerId: Long): Flow<List<SaleLine>>
}
