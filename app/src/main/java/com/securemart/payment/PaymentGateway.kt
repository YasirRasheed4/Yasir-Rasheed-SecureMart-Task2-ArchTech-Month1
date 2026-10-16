package com.securemart.payment

import com.securemart.security.CardValidator
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID

/** Card details exist only in memory while paying. They are never written to disk or the database. */
data class CardInput(
    val number: String,
    val expMonth: String,
    val expYear: String,
    val cvv: String
)

data class ShippingInfo(
    val name: String,
    val phone: String,
    val address: String,
    val city: String
)

data class PaymentResult(
    val approved: Boolean,
    val reference: String,
    val brand: String,
    val last4: String,
    val message: String
)

/**
 * The app talks to payments only through this interface. A real gateway (Stripe, JazzCash,
 * Easypaisa...) would implement it, so raw card data goes straight to the processor and the app
 * only keeps the returned token/reference.
 */
interface PaymentGateway {
    suspend fun authorize(amountCents: Long, card: CardInput): PaymentResult
    suspend fun void(reference: String)
}

/** Demo gateway: no real money moves. */
class MockPaymentGateway : PaymentGateway {

    override suspend fun authorize(amountCents: Long, card: CardInput): PaymentResult {
        delay(1200) // pretend to talk to the bank
        val digits = CardValidator.digitsOnly(card.number)
        val brand = CardValidator.brand(digits)
        val last4 = digits.takeLast(4)
        if (digits == DECLINED_TEST_CARD) {
            return PaymentResult(false, "", brand, last4, "Your card was declined. Try another card.")
        }
        val ref = "PAY-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)
        return PaymentResult(true, ref, brand, last4, "Approved")
    }

    override suspend fun void(reference: String) {
        delay(300) // a real gateway would release the held funds here
    }

    companion object {
        const val DECLINED_TEST_CARD = "4000000000000002"
    }
}
