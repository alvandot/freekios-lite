package com.freekiosklite.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun samePinAndSaltProduceSameHash() {
        val salt = ByteArray(PinHasher.SALT_SIZE_BYTES) { it.toByte() }

        val first = PinHasher.hash("1234".toCharArray(), salt)
        val second = PinHasher.hash("1234".toCharArray(), salt)

        assertTrue(PinHasher.constantTimeEquals(first, second))
    }

    @Test
    fun differentPinsAndSaltsDoNotProduceSameHash() {
        val firstSalt = ByteArray(PinHasher.SALT_SIZE_BYTES) { it.toByte() }
        val secondSalt = ByteArray(PinHasher.SALT_SIZE_BYTES) { (it + 1).toByte() }

        val first = PinHasher.hash("1234".toCharArray(), firstSalt)
        val wrongPin = PinHasher.hash("9999".toCharArray(), firstSalt)
        val differentSalt = PinHasher.hash("1234".toCharArray(), secondSalt)

        assertFalse(PinHasher.constantTimeEquals(first, wrongPin))
        assertFalse(PinHasher.constantTimeEquals(first, differentSalt))
        assertNotEquals(first.toList(), wrongPin.toList())
    }

    @Test
    fun generatedSaltsAreSizedAndUnique() {
        val first = PinHasher.generateSalt()
        val second = PinHasher.generateSalt()

        assertTrue(first.size == PinHasher.SALT_SIZE_BYTES)
        assertTrue(second.size == PinHasher.SALT_SIZE_BYTES)
        assertFalse(first.contentEquals(second))
    }
}
