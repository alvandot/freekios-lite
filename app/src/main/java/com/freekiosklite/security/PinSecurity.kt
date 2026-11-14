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
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private val Context.securityDataStore by preferencesDataStore(name = "security_preferences")

private const val KEY_ALIAS = "freekiosk_pin_key"
private const val LOCKOUT_MILLIS = 5 * 60 * 1000L
private const val MAX_ATTEMPTS = 5

sealed interface PinVerification {
    data object NotConfigured : PinVerification
    data object Success : PinVerification
    data class Failure(val remainingAttempts: Int) : PinVerification
    data object Locked : PinVerification
}

interface PinAuthenticator {
    suspend fun isConfigured(): Boolean
    suspend fun configurePin(pin: CharArray)
    suspend fun verifyPin(pin: CharArray): PinVerification
}

class PinSecurity(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : PinAuthenticator {
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val saltKey = stringPreferencesKey("pin_salt")
    private val attemptsKey = intPreferencesKey("pin_attempts")
    private val lockedUntilKey = longPreferencesKey("pin_locked_until")

    override suspend fun isConfigured(): Boolean =
        context.securityDataStore.data.first()[pinHashKey] != null

    override suspend fun configurePin(pin: CharArray) {
        require(pin.size >= 4) { "PIN must contain at least four digits" }
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        context.securityDataStore.edit { preferences ->
            preferences[saltKey] = encode(salt)
            preferences[pinHashKey] = encrypt(hash)
            preferences[attemptsKey] = 0
            preferences[lockedUntilKey] = 0L
        }
        pin.fill('\u0000')
    }

    override suspend fun verifyPin(pin: CharArray): PinVerification {
        val preferences = context.securityDataStore.data.first()
        val encryptedHash = preferences[pinHashKey] ?: return PinVerification.NotConfigured
        val salt = preferences[saltKey]?.let(::decode) ?: return PinVerification.NotConfigured
        val now = clock()
        if ((preferences[lockedUntilKey] ?: 0L) > now) {
            pin.fill('\u0000')
            return PinVerification.Locked
        }

        val matches = PinHasher.constantTimeEquals(
            PinHasher.hash(pin, salt),
            decrypt(encryptedHash),
        )
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

}
