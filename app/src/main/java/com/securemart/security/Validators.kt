package com.securemart.security

import android.util.Patterns
import com.securemart.payment.CardInput
import com.securemart.payment.ShippingInfo
import java.math.BigDecimal
import java.util.Calendar

/** Server-style validation: every value is checked again in the repository, not just in the UI. */
object InputValidator {

    fun cleanLine(s: String, max: Int): String =
        s.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ").take(max)

    fun cleanText(s: String, max: Int): String =
        s.filter { !it.isISOControl() || it == '\n' }.trim().take(max)

    fun isValidEmail(email: String): Boolean =
        email.length <= 100 && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun nameProblem(name: String): String? =
        if (Regex("^[\\p{L} .'-]{2,60}$").matches(name)) null
        else "Enter your name (2-60 letters)"

    fun passwordProblem(p: String): String? = when {
        p.length < 8 -> "Password must be at least 8 characters"
        p.length > 64 -> "Password is too long (max 64 characters)"
        !p.any { it.isUpperCase() } -> "Password needs an upper-case letter"
        !p.any { it.isLowerCase() } -> "Password needs a lower-case letter"
        !p.any { it.isDigit() } -> "Password needs a number"
        else -> null
    }

    /** "1250" or "1250.50" -> cents. Returns null if the text is not a valid positive price. */
    fun parsePriceToCents(text: String): Long? {
        val t = text.trim()
        if (!Regex("^\\d{1,7}(\\.\\d{1,2})?$").matches(t)) return null
        val cents = BigDecimal(t).multiply(BigDecimal(100)).toLong()
        return if (cents in 1..999_999_999L) cents else null
    }

    fun shippingProblem(s: ShippingInfo): String? {
        if (s.name.length < 2) return "Enter the recipient's name"
        val phone = s.phone.replace(Regex("[ -]"), "")
        if (!Regex("^\\+?\\d{10,14}$").matches(phone)) return "Enter a valid phone number"
        if (s.address.length < 8) return "Enter a full street address"
        if (s.city.length < 2) return "Enter your city"
        return null
    }
}

object CardValidator {

    fun digitsOnly(s: String): String = s.filter { it.isDigit() }

    fun brand(digits: String): String = when {
        digits.startsWith("4") -> "Visa"
        digits.length >= 2 && digits.substring(0, 2).toInt() in 51..55 -> "Mastercard"
        digits.length >= 4 && digits.substring(0, 4).toInt() in 2221..2720 -> "Mastercard"
        digits.startsWith("34") || digits.startsWith("37") -> "Amex"
        else -> "Card"
    }

    /** Luhn checksum: catches typing mistakes in card numbers. */
    fun luhnValid(number: String): Boolean {
        if (number.length < 13 || number.length > 19) return false
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var d = number[i] - '0'
            if (alternate) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun problem(card: CardInput): String? {
        val digits = digitsOnly(card.number)
        if (!luhnValid(digits)) return "Card number is not valid"
        val month = card.expMonth.toIntOrNull()
        val year = card.expYear.toIntOrNull()
        if (month == null || month !in 1..12 || card.expYear.length != 2 || year == null) {
            return "Enter the expiry as MM and YY"
        }
        val now = Calendar.getInstance()
        val nowYear = now.get(Calendar.YEAR) % 100
        val nowMonth = now.get(Calendar.MONTH) + 1
        if (year < nowYear || (year == nowYear && month < nowMonth)) return "This card has expired"
        if (year > nowYear + 20) return "Expiry year is not valid"
        val cvvLength = if (brand(digits) == "Amex") 4 else 3
        if (card.cvv.length != cvvLength || !card.cvv.all { it.isDigit() }) {
            return "Security code (CVV) must be $cvvLength digits"
        }
        return null
    }
}
