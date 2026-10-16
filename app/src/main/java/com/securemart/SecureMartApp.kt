@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securemart.data.CheckoutResult
import com.securemart.data.User
import com.securemart.ui.AuthScreen
import com.securemart.ui.CartScreen
import com.securemart.ui.CheckoutScreen
import com.securemart.ui.HomeScreen
import com.securemart.ui.OrderDoneScreen
import com.securemart.ui.OrdersScreen
import com.securemart.ui.ProductDetailScreen
import com.securemart.ui.ProductFormScreen
import com.securemart.ui.ProfileScreen
import com.securemart.ui.SellScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    SHOP("Shop", Icons.Filled.Storefront),
    CART("Cart", Icons.Filled.ShoppingCart),
    SELL("Sell", Icons.Filled.Sell),
    ORDERS("Orders", Icons.Filled.Receipt),
    PROFILE("Profile", Icons.Filled.Person)
}

/** Full-screen pages that open on top of the tabs. */
private sealed interface Overlay {
    data class Detail(val productId: Long) : Overlay
    data class Form(val productId: Long?) : Overlay
    data object Checkout : Overlay
    data class Done(val orderId: Long) : Overlay
}

@Composable
fun SecureMartApp(vm: MartViewModel = viewModel()) {
    val ready by vm.ready.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val currentUser = user
    when {
        !ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        currentUser == null -> AuthScreen(
            onLogin = { email, password, done -> vm.login(email, password, done) },
            onRegister = { name, email, password, done -> vm.register(name, email, password, done) }
        )
        else -> MainShell(vm, currentUser)
    }
}

@Composable
private fun MainShell(vm: MartViewModel, user: User) {
    val products by vm.products.collectAsStateWithLifecycle()
    val myProducts by vm.myProducts.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val sales by vm.sales.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()

    val stack = remember { mutableStateListOf<Overlay>() }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    when (val top = stack.lastOrNull()) {
        is Overlay.Detail -> {
            val product = products.firstOrNull { it.id == top.productId }
            if (product == null) {
                LaunchedEffect(Unit) { pop() }
            } else {
                ProductDetailScreen(
                    product = product,
                    isOwner = product.sellerId == user.id,
                    onBack = { pop() },
                    onAddToCart = { qty -> vm.addToCart(product.id, qty) },
                    onEdit = { stack.add(Overlay.Form(product.id)) }
                )
            }
        }

        is Overlay.Form -> ProductFormScreen(
            existing = top.productId?.let { id -> products.firstOrNull { it.id == id } },
            onBack = { pop() },
            onSave = { form, done ->
                vm.saveProduct(top.productId, form) { error ->
                    done(error)
                    if (error == null) pop()
                }
            }
        )

        is Overlay.Checkout -> CheckoutScreen(
            totalCents = cart.sumOf { it.priceCents * it.quantity },
            itemCount = cart.sumOf { it.quantity },
            onBack = { pop() },
            onPay = { ship, card, onError ->
                vm.checkout(ship, card) { result ->
                    when (result) {
                        is CheckoutResult.Success -> {
                            pop()
                            stack.add(Overlay.Done(result.orderId))
                        }
                        is CheckoutResult.Error -> onError(result.message)
                    }
                }
            }
        )

        is Overlay.Done -> OrderDoneScreen(
            orderId = top.orderId,
            onContinue = {
                stack.clear()
                tabIndex = MainTab.SHOP.ordinal
            },
            onViewOrders = {
                stack.clear()
                tabIndex = MainTab.ORDERS.ordinal
            }
        )

        null -> Scaffold(
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tabIndex == tab.ordinal,
                            onClick = { tabIndex = tab.ordinal },
                            icon = {
                                val cartCount = cart.sumOf { it.quantity }
                                if (tab == MainTab.CART && cartCount > 0) {
                                    BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (MainTab.entries[tabIndex]) {
                    MainTab.SHOP -> HomeScreen(
                        products = products,
                        myId = user.id,
                        onOpen = { stack.add(Overlay.Detail(it.id)) }
                    )
                    MainTab.CART -> CartScreen(
                        lines = cart,
                        onQuantity = { line, qty -> vm.setCartQuantity(line, qty) },
                        onRemove = { vm.removeFromCart(it) },
                        onCheckout = { stack.add(Overlay.Checkout) }
                    )
                    MainTab.SELL -> SellScreen(
                        myProducts = myProducts,
                        sales = sales,
                        onNew = { stack.add(Overlay.Form(null)) },
                        onEdit = { stack.add(Overlay.Form(it.id)) },
                        onDelete = { vm.deleteProduct(it.id) }
                    )
                    MainTab.ORDERS -> OrdersScreen(purchases)
                    MainTab.PROFILE -> ProfileScreen(
                        user = user,
                        listingCount = myProducts.size,
                        orderCount = purchases.size,
                        onLogout = { vm.logout() }
                    )
                }
            }
        }
    }
}
