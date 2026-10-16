package com.securemart.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.securemart.R
import com.securemart.payment.CardInput
import com.securemart.payment.MockPaymentGateway
import com.securemart.payment.PaymentGateway
import com.securemart.payment.ShippingInfo
import com.securemart.security.CardValidator
import com.securemart.security.InputValidator
import com.securemart.security.PasswordHasher
import com.securemart.security.SecurityConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.Locale

private class OrderException(message: String) : Exception(message)

/**
 * Single place where all business rules and security checks live.
 * The UI can ask for anything, but this class decides what is allowed.
 */
class Repository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val users = db.userDao()
    private val products = db.productDao()
    private val cart = db.cartDao()
    private val orders = db.orderDao()
    private val gateway: PaymentGateway = MockPaymentGateway()

    // ---------- streams ----------
    fun activeProducts(): Flow<List<Product>> = products.observeActive()
    fun productsBySeller(sellerId: Long): Flow<List<Product>> = products.observeBySeller(sellerId)
    fun cartLines(userId: Long): Flow<List<CartLine>> = cart.observeLines(userId)
    fun orderStream(buyerId: Long): Flow<List<Order>> = orders.observeOrders(buyerId)
    fun orderItemStream(buyerId: Long): Flow<List<OrderItem>> = orders.observeItemsForBuyer(buyerId)
    fun salesStream(sellerId: Long): Flow<List<SaleLine>> = orders.observeSales(sellerId)

    suspend fun findUser(id: Long): User? = users.findById(id)

    // ---------- accounts ----------
    suspend fun register(name: String, email: String, password: String): AuthResult {
        val cleanName = InputValidator.cleanLine(name, 60)
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        InputValidator.nameProblem(cleanName)?.let { return AuthResult.Error(it) }
        if (!InputValidator.isValidEmail(cleanEmail)) return AuthResult.Error("Enter a valid email address")
        InputValidator.passwordProblem(password)?.let { return AuthResult.Error(it) }
        if (users.findByEmail(cleanEmail) != null) {
            return AuthResult.Error("An account with this email already exists")
        }

        val salt = PasswordHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password.toCharArray(), salt) }
        val user = User(
            name = cleanName,
            email = cleanEmail,
            passwordHash = PasswordHasher.encode(hash),
            salt = PasswordHasher.encode(salt)
        )
        return try {
            AuthResult.Success(user.copy(id = users.insert(user)))
        } catch (e: SQLiteConstraintException) {
            AuthResult.Error("An account with this email already exists")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val user = users.findByEmail(cleanEmail)
        if (user == null) {
            // Do the same amount of work as a real login so response time does not reveal
            // whether an account exists.
            withContext(Dispatchers.Default) { PasswordHasher.hash(password.toCharArray(), ByteArray(16)) }
            return AuthResult.Error(GENERIC_LOGIN_ERROR)
        }

        val now = System.currentTimeMillis()
        if (user.lockedUntil > now) {
            val minutes = (user.lockedUntil - now) / 60_000 + 1
            return AuthResult.Error("Too many failed attempts. Try again in $minutes min.")
        }

        val ok = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password.toCharArray(), user.salt, user.passwordHash)
        }
        if (ok) {
            if (user.failedAttempts != 0 || user.lockedUntil != 0L) users.setLockState(user.id, 0, 0)
            return AuthResult.Success(user)
        }

        val failed = user.failedAttempts + 1
        return if (failed >= SecurityConfig.MAX_LOGIN_ATTEMPTS) {
            users.setLockState(user.id, 0, now + SecurityConfig.LOCK_MINUTES * 60_000)
            AuthResult.Error("Too many failed attempts. Account locked for ${SecurityConfig.LOCK_MINUTES} minutes.")
        } else {
            users.setLockState(user.id, failed, 0)
            AuthResult.Error(GENERIC_LOGIN_ERROR)
        }
    }

    // ---------- cart ----------
    suspend fun addToCart(userId: Long, productId: Long, qty: Int): String? {
        val product = products.findById(productId) ?: return "Product not found"
        if (!product.active) return "This product is no longer available"
        if (product.sellerId == userId) return "You can't buy your own product"
        if (product.stock <= 0) return "Sold out"
        val existing = cart.find(userId, productId)
        val newQty = (existing?.quantity ?: 0) + qty.coerceAtLeast(1)
        if (newQty > product.stock) return "Only ${product.stock} in stock"
        if (newQty > MAX_PER_ITEM) return "Maximum $MAX_PER_ITEM per item"
        cart.upsert(
            CartItem(id = existing?.id ?: 0, userId = userId, productId = productId, quantity = newQty)
        )
        return null
    }

    suspend fun setCartQuantity(userId: Long, cartId: Long, productId: Long, qty: Int): String? {
        if (qty < 1) return null
        val product = products.findById(productId) ?: return "Product not found"
        if (qty > product.stock) return "Only ${product.stock} in stock"
        if (qty > MAX_PER_ITEM) return "Maximum $MAX_PER_ITEM per item"
        cart.setQuantity(cartId, userId, qty)
        return null
    }

    suspend fun removeFromCart(userId: Long, cartId: Long) {
        cart.remove(cartId, userId)
    }

    // ---------- selling ----------
    suspend fun saveProduct(seller: User, productId: Long?, form: ProductFormData): String? {
        val title = InputValidator.cleanLine(form.title, 80)
        if (title.length < 3) return "Title must be at least 3 characters"
        val description = InputValidator.cleanText(form.description, 600)
        if (description.length < 10) return "Description must be at least 10 characters"
        val cents = InputValidator.parsePriceToCents(form.price)
            ?: return "Enter a valid price (for example 1250 or 1250.50)"
        val stock = form.stock.trim().toIntOrNull()?.takeIf { it in 0..999 }
            ?: return "Stock must be a number from 0 to 999"
        val category = ProductCategory.fromName(form.category).name

        val existing = if (productId != null) products.findById(productId) else null
        if (productId != null && (existing == null || existing.sellerId != seller.id)) {
            return "You can only edit your own listings"
        }

        var imagePath = existing?.imagePath
        if (form.imageUri != null) {
            val saved = withContext(Dispatchers.IO) { ImageStore.saveProductImage(appContext, form.imageUri) }
                ?: return "Could not read that image. Please pick another one."
            withContext(Dispatchers.IO) { ImageStore.delete(imagePath) }
            imagePath = saved
        }

        if (existing == null) {
            products.insert(
                Product(
                    sellerId = seller.id,
                    sellerName = seller.name,
                    title = title,
                    description = description,
                    priceCents = cents,
                    category = category,
                    stock = stock,
                    imagePath = imagePath
                )
            )
        } else {
            products.update(
                existing.copy(
                    title = title,
                    description = description,
                    priceCents = cents,
                    category = category,
                    stock = stock,
                    imagePath = imagePath
                )
            )
        }
        return null
    }

    suspend fun deleteProduct(seller: User, productId: Long): String? {
        val product = products.findById(productId) ?: return "Product not found"
        if (product.sellerId != seller.id) return "You can only delete your own listings"
        withContext(Dispatchers.IO) { ImageStore.delete(product.imagePath) }
        // Soft delete: old orders keep their history, the item just disappears from the shop.
        products.update(product.copy(active = false, imagePath = null))
        return null
    }

    // ---------- checkout ----------
    suspend fun placeOrder(buyer: User, ship: ShippingInfo, card: CardInput): CheckoutResult {
        val shipping = ShippingInfo(
            name = InputValidator.cleanLine(ship.name, 60),
            phone = InputValidator.cleanLine(ship.phone, 20),
            address = InputValidator.cleanLine(ship.address, 150),
            city = InputValidator.cleanLine(ship.city, 40)
        )
        InputValidator.shippingProblem(shipping)?.let { return CheckoutResult.Error(it) }
        CardValidator.problem(card)?.let { return CheckoutResult.Error(it) }

        val lines = cart.lines(buyer.id)
        if (lines.isEmpty()) return CheckoutResult.Error("Your cart is empty")
        for (line in lines) {
            if (line.sellerId == buyer.id) return CheckoutResult.Error("Remove your own product from the cart")
            if (line.quantity > line.stock) {
                return CheckoutResult.Error("Only ${line.stock} of \"${line.title}\" left in stock")
            }
        }
        // The amount is always calculated from database prices, never taken from the screen.
        val total = lines.sumOf { it.priceCents * it.quantity }

        val payment = gateway.authorize(total, card)
        if (!payment.approved) return CheckoutResult.Error(payment.message)

        return try {
            val orderId = db.withTransaction {
                val fresh = cart.lines(buyer.id)
                if (fresh.isEmpty()) throw OrderException("Your cart changed. Please review it.")
                var sum = 0L
                for (line in fresh) {
                    if (products.decrementStock(line.productId, line.quantity) == 0) {
                        throw OrderException("\"${line.title}\" just sold out. Please review your cart.")
                    }
                    sum += line.priceCents * line.quantity
                }
                if (sum != total) throw OrderException("Prices changed. Please review your cart.")

                val id = orders.insertOrder(
                    Order(
                        buyerId = buyer.id,
                        totalCents = sum,
                        shipName = shipping.name,
                        shipPhone = shipping.phone,
                        shipAddress = shipping.address,
                        shipCity = shipping.city,
                        cardBrand = payment.brand,
                        cardLast4 = payment.last4,
                        paymentRef = payment.reference,
                        status = "PAID"
                    )
                )
                orders.insertItems(
                    fresh.map {
                        OrderItem(
                            orderId = id,
                            productId = it.productId,
                            sellerId = it.sellerId,
                            title = it.title,
                            priceCents = it.priceCents,
                            quantity = it.quantity
                        )
                    }
                )
                cart.clear(buyer.id)
                id
            }
            CheckoutResult.Success(orderId)
        } catch (e: OrderException) {
            gateway.void(payment.reference) // release the payment if the order could not be saved
            CheckoutResult.Error(e.message ?: "Could not place the order")
        }
    }

    // ---------- sample data ----------
    /** On first launch: one demo seller with a few products so the shop is not empty. */
    suspend fun seedIfEmpty() {
        if (users.count() > 0) return
        val salt = PasswordHasher.newSalt()
        val randomPassword = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val hash = withContext(Dispatchers.Default) {
            PasswordHasher.hash(PasswordHasher.encode(randomPassword).toCharArray(), salt)
        }
        // Nobody knows this password, so the demo seller account cannot be logged into.
        val sellerName = "Campus Marketplace"
        val sellerId = users.insert(
            User(
                name = sellerName,
                email = DEMO_SELLER_EMAIL,
                passwordHash = PasswordHasher.encode(hash),
                salt = PasswordHasher.encode(salt)
            )
        )

        fun demo(title: String, description: String, rupees: Long, category: ProductCategory, stock: Int) =
            Product(
                sellerId = sellerId,
                sellerName = sellerName,
                title = title,
                description = description,
                priceCents = rupees * 100,
                category = category.name,
                stock = stock
            )

        listOf(
            demo("Wireless Earbuds", "Bluetooth 5.3 earbuds with a charging case and up to 20 hours of battery life.", 4500, ProductCategory.ELECTRONICS, 8),
            demo("Mechanical Keyboard", "Compact 75% keyboard with tactile switches and white backlight. Great for coding.", 7500, ProductCategory.ELECTRONICS, 4),
            demo("Cotton Hoodie", "Soft fleece-lined hoodie, medium size, charcoal colour. Worn only a few times.", 2800, ProductCategory.FASHION, 3),
            demo("Canvas Backpack", "Water-resistant backpack with a padded laptop sleeve that fits up to 15.6 inches.", 3200, ProductCategory.FASHION, 6),
            demo("LED Desk Lamp", "Dimmable desk lamp with three colour modes and a USB charging port.", 1900, ProductCategory.HOME, 10),
            demo("Steel Water Bottle", "Insulated 750 ml bottle that keeps drinks cold for 24 hours.", 1200, ProductCategory.HOME, 12),
            demo("Programming Handbook", "Used Kotlin programming handbook in very good condition, no markings inside.", 1500, ProductCategory.BOOKS, 2),
            demo("Scientific Calculator", "Exam-approved scientific calculator with 240 functions and a protective cover.", 2400, ProductCategory.OTHER, 5),
            demo("Football (Size 5)", "Training football, size 5, stitched panels. Barely used.", 1600, ProductCategory.SPORTS, 7)
        ).forEach { products.insert(it) }
    }

    /**
     * Gives the sample products their pictures. Runs on every start, so it also fixes products
     * that were created earlier without a picture. Does nothing once every picture is in place.
     */
    suspend fun ensureDemoImages() {
        val seller = users.findByEmail(DEMO_SELLER_EMAIL) ?: return
        for (product in products.allBySeller(seller.id)) {
            val resId = DEMO_IMAGES[product.title] ?: continue
            val current = product.imagePath
            val missing = current == null || !File(current).exists()
            if (!product.active || !missing) continue
            val path = withContext(Dispatchers.IO) { ImageStore.copyFromResource(appContext, resId) } ?: continue
            products.update(product.copy(imagePath = path))
        }
    }

    companion object {
        private const val DEMO_SELLER_EMAIL = "seller@securemart.local"
        private val DEMO_IMAGES = mapOf(
            "Wireless Earbuds" to R.drawable.p_earbuds,
            "Mechanical Keyboard" to R.drawable.p_keyboard,
            "Cotton Hoodie" to R.drawable.p_hoodie,
            "Canvas Backpack" to R.drawable.p_backpack,
            "LED Desk Lamp" to R.drawable.p_lamp,
            "Steel Water Bottle" to R.drawable.p_bottle,
            "Programming Handbook" to R.drawable.p_book,
            "Scientific Calculator" to R.drawable.p_calculator,
            "Football (Size 5)" to R.drawable.p_football
        )
        private const val GENERIC_LOGIN_ERROR = "Invalid email or password"
        const val MAX_PER_ITEM = 20
    }
}
