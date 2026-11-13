package com.freekiosklite.security

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinSecurityInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var now = 1_000_000L

    @Before
    fun clearStoredSecurityState() {
        context.preferencesDataStoreFile("security_preferences.preferences_pb").delete()
    }

    @After
    fun clearAfterTest() {
        context.preferencesDataStoreFile("security_preferences.preferences_pb").delete()
    }

    @Test
    fun pinIsNotStoredAsPlaintextAndCanBeVerified() = runTest {
        val security = PinSecurity(context) { now }

        security.configurePin("1234".toCharArray())

        val stored = context.preferencesDataStoreFile("security_preferences.preferences_pb").readBytes()
        assertTrue(stored.isNotEmpty())
        assertTrue("1234".toByteArray().none(stored::contains))
        assertEquals(PinVerification.Success, security.verifyPin("1234".toCharArray()))
        assertTrue(security.isConfigured())
    }

    @Test
    fun incorrectPinsLockThenRecoverAfterLockoutWindow() = runTest {
        val security = PinSecurity(context) { now }
        security.configurePin("1234".toCharArray())

        repeat(4) {
            assertTrue(security.verifyPin("0000".toCharArray()) is PinVerification.Failure)
        }
        assertEquals(PinVerification.Locked, security.verifyPin("0000".toCharArray()))
        assertEquals(PinVerification.Locked, security.verifyPin("1234".toCharArray()))

        now += 5 * 60 * 1000L + 1
        assertEquals(PinVerification.Success, security.verifyPin("1234".toCharArray()))
    }
}
