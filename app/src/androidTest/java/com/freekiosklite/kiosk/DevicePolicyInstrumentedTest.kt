package com.freekiosklite.kiosk

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePolicyInstrumentedTest {
    @Test
    fun deviceOwnerAllowsExplicitLockTaskCapability() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val controller = DevicePolicyController(context)

        assumeTrue(
            "Device Owner was not provisioned; policy probe is not applicable.",
            controller.isDeviceOwner(),
        )
        assertTrue(controller.canStartLockTask())
    }
}
