package com.freekiosklite.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDataInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearStoredPackages() {
        context.preferencesDataStoreFile("kiosk_preferences.preferences_pb").delete()
    }

    @After
    fun clearAfterTest() {
        context.preferencesDataStoreFile("kiosk_preferences.preferences_pb").delete()
    }

    @Test
    fun approvedPackagesPersistAcrossRepositoryInstances() = runTest {
        AppPreferencesRepository(context).saveSelectedPackages(setOf("com.example.approved"))

        assertTrue(
            AppPreferencesRepository(context).selectedPackages.first()
                .contains("com.example.approved"),
        )
    }

    @Test
    fun launchableAppsExcludeFreeKioskItselfAndMissingPackagesHaveNoIntent() {
        val apps = InstalledAppsRepository(context).findLaunchableApps()
        assertFalse(apps.any { it.packageName == context.packageName })
        assertTrue(AppLaunchResolver(context.packageManager)
            .launchIntent("com.freekiosklite.package.that.is.not.installed") == null)
    }
}
