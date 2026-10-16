package com.securemart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.securemart.data.AuthResult
import com.securemart.data.CartLine
import com.securemart.data.CheckoutResult
import com.securemart.data.Order
import com.securemart.data.OrderWithItems
import com.securemart.data.Product
import com.securemart.data.ProductFormData
import com.securemart.data.Repository
import com.securemart.data.SaleLine
import com.securemart.data.User
import com.securemart.payment.CardInput
import com.securemart.payment.ShippingInfo
import com.securemart.security.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MartViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val session = SessionManager(app)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val products: StateFlow<List<Product>> = repo.activeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myProducts: StateFlow<List<Product>> = _user
        .flatMapLatest { u -> if (u == null) flowOf(emptyList<Product>()) else repo.productsBySeller(u.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cart: StateFlow<List<CartLine>> = _user
        .flatMapLatest { u -> if (u == null) flowOf(emptyList<CartLine>()) else repo.cartLines(u.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sales: StateFlow<List<SaleLine>> = _user
        .flatMapLatest { u -> if (u == null) flowOf(emptyList<SaleLine>()) else repo.salesStream(u.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val purchases: StateFlow<List<OrderWithItems>> = _user
        .flatMapLatest { u ->
            if (u == null) {
                flowOf(emptyList<OrderWithItems>())
            } else {
                combine(repo.orderStream(u.id), repo.orderItemStream(u.id)) { orders: List<Order>, items ->
                    val byOrder = items.groupBy { it.orderId }
                    orders.map { OrderWithItems(it, byOrder[it.id].orEmpty()) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repo.seedIfEmpty()
            repo.ensureDemoImages()
            val id = session.userId()
            val restored = if (id != null) repo.findUser(id) else null
            if (restored == null) session.clear()
            _user.value = restored
            _ready.value = true
        }
    }

    private fun toast(message: String) {
        _messages.trySend(message)
    }

    // ---------- session ----------
    /** Called when the app comes to the foreground: sign out if the user was away too long. */
    fun checkSession() {
        if (!_ready.value) return
        if (_user.value != null && session.userId() == null) {
            _user.value = null
            toast("You were signed out after 30 minutes of inactivity")
        }
    }

    /** Called when the app goes to the background. */
    fun markInactive() {
        session.touch()
    }

    fun register(name: String, email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repo.register(name, email, password)) {
                is AuthResult.Success -> {
                    session.start(result.user.id)
                    _user.value = result.user
                    onResult(null)
                }
                is AuthResult.Error -> onResult(result.message)
            }
        }
    }

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repo.login(email, password)) {
                is AuthResult.Success -> {
                    session.start(result.user.id)
                    _user.value = result.user
                    onResult(null)
                }
                is AuthResult.Error -> onResult(result.message)
            }
        }
    }

    fun logout() {
        session.clear()
        _user.value = null
    }

    // ---------- cart ----------
    fun addToCart(productId: Long, quantity: Int) {
        val u = _user.value ?: return
        viewModelScope.launch {
            toast(repo.addToCart(u.id, productId, quantity) ?: "Added to cart")
        }
    }

    fun setCartQuantity(line: CartLine, quantity: Int) {
        val u = _user.value ?: return
        viewModelScope.launch {
            repo.setCartQuantity(u.id, line.cartId, line.productId, quantity)?.let { toast(it) }
        }
    }

    fun removeFromCart(line: CartLine) {
        val u = _user.value ?: return
        viewModelScope.launch { repo.removeFromCart(u.id, line.cartId) }
    }

    // ---------- selling ----------
    fun saveProduct(productId: Long?, form: ProductFormData, onResult: (String?) -> Unit) {
        val u = _user.value ?: return
        viewModelScope.launch {
            val error = repo.saveProduct(u, productId, form)
            if (error == null) toast(if (productId == null) "Product listed" else "Listing updated")
            onResult(error)
        }
    }

    fun deleteProduct(productId: Long) {
        val u = _user.value ?: return
        viewModelScope.launch {
            toast(repo.deleteProduct(u, productId) ?: "Listing removed")
        }
    }

    // ---------- checkout ----------
    fun checkout(ship: ShippingInfo, card: CardInput, onResult: (CheckoutResult) -> Unit) {
        val u = _user.value ?: return
        viewModelScope.launch { onResult(repo.placeOrder(u, ship, card)) }
    }
}
