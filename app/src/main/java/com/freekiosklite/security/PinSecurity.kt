package com.freekiosklite.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.flow.first

private val Context.securityDataStore by preferencesDataStore(name = "security_preferences")

private const val KEY_ALIAS = "freekiosk_pin_key"
private const val HASH_ITERATIONS = 120_000
private const val LOCKOUT_MILLIS = 5 * 60 * 1000L
private const val MAX_ATTEMPTS = 5

sealed interface PinVerification {
    data object NotConfigured : PinVerification
    data object Success : PinVerification
    data class Failure(val remainingAttempts: Int) : PinVerification
    data object Locked : PinVerification
}

class PinSecurity(private val context: Context) {
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val saltKey = stringPreferencesKey("pin_salt")
    private val attemptsKey = intPreferencesKey("pin_attempts")
    private val lockedUntilKey = longPreferencesKey("pin_locked_until")

    suspend fun configurePin(pin: CharArray) {
        require(pin.size >= 4) { "PIN must contain at least four digits" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = hash(pin, salt)
        context.securityDataStore.edit { preferences ->
            preferences[saltKey] = encode(salt)
            preferences[pinHashKey] = encrypt(hash)
            preferences[attemptsKey] = 0
            preferences[lockedUntilKey] = 0L
        }
        pin.fill('\u0000')
    }

    suspend fun verifyPin(pin: CharArray): PinVerification {
        val preferences = context.securityDataStore.data.first()
        val encryptedHash = preferences[pinHashKey] ?: return PinVerification.NotConfigured
        val salt = preferences[saltKey]?.let(::decode) ?: return PinVerification.NotConfigured
        val now = System.currentTimeMillis()
        if ((preferences[lockedUntilKey] ?: 0L) > now) {
            pin.fill('\u0000')
            return PinVerification.Locked
        }

        val matches = constantTimeEquals(hash(pin, salt), decrypt(encryptedHash))
        pin.fill('\u0000')
        if (matches) {
            context.securityDataStore.edit {
                it[attemptsKey] = 0
                it[lockedUntilKey] = 0L
            }
            return PinVerification.Success
        }

        val attempts = (preferences[attemptsKey] ?: 0) + 1
        context.securityDataStore.edit {
            it[attemptsKey] = attempts
            if (attempts >= MAX_ATTEMPTS) {
                it[attemptsKey] = 0
                it[lockedUntilKey] = now + LOCKOUT_MILLIS
            }
        }
        return if (attempts >= MAX_ATTEMPTS) {
            PinVerification.Locked
        } else {
            PinVerification.Failure(MAX_ATTEMPTS - attempts)
        }
    }

    private fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, HASH_ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private fun encrypt(value: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        return encode(cipher.iv + cipher.doFinal(value))
    }

    private fun decrypt(value: String): ByteArray {
        val encrypted = decode(value)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, encrypted.copyOfRange(0, 12)),
            )
        }
        return cipher.doFinal(encrypted.copyOfRange(12, encrypted.size))
    }

    private fun encode(value: ByteArray): String =
        Base64.encodeToString(value, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        if (left.size != right.size) return false
        var result = 0
        left.indices.forEach { index -> result = result or (left[index].toInt() xor right[index].toInt()) }
        return result == 0
    }
}
