package com.securemart.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password storage: PBKDF2-HMAC-SHA256, 120,000 iterations, 16-byte random salt per user.
 * The plain password is never stored or logged.
 */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

    /** Constant-time comparison so timing does not reveal how many bytes matched. */
    fun verify(password: CharArray, saltB64: String, expectedB64: String): Boolean {
        val actual = hash(password, decode(saltB64))
        return MessageDigest.isEqual(actual, decode(expectedB64))
    }
}
