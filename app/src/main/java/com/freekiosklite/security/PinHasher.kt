package com.freekiosklite.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    const val SALT_SIZE_BYTES = 16
    const val HASH_ITERATIONS = 120_000
    const val HASH_SIZE_BITS = 256

    fun generateSalt(random: SecureRandom = SecureRandom()): ByteArray =
        ByteArray(SALT_SIZE_BYTES).also(random::nextBytes)

    fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, HASH_ITERATIONS, HASH_SIZE_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        if (left.size != right.size) return false
        var result = 0
        left.indices.forEach { index ->
            result = result or (left[index].toInt() xor right[index].toInt())
        }
        return result == 0
    }
}
