package com.freekiosklite.ui

import android.app.Application
import com.freekiosklite.data.InstalledApp
import com.freekiosklite.data.LaunchableAppsProvider
import com.freekiosklite.data.SelectedPackagesStore
import com.freekiosklite.security.PinAuthenticator
import com.freekiosklite.security.PinVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KioskViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadingAndSelectionStateArePersisted() = runTest {
        val store = FakePackageStore()
        val viewModel = KioskViewModel(
            Application(),
            appPreferences = store,
            installedApps = FakeAppsProvider(
                listOf(
                    InstalledApp("com.example.one", "One"),
                    InstalledApp("com.example.two", "Two"),
                ),
            ),
            pinSecurity = FakePinAuthenticator(),
        )

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.installedApps.size)

        viewModel.togglePackage("com.example.one")
        viewModel.setPin("12a34")
        viewModel.saveConfiguration()
        advanceUntilIdle()

        assertEquals(setOf("com.example.one"), store.selectedPackages.first())
        assertEquals("", viewModel.state.value.pin)
        assertTrue(viewModel.state.value.isPinConfigured)
    }

    @Test
    fun saveWithoutApprovedAppReportsValidationError() = runTest {
        val viewModel = KioskViewModel(
            Application(),
            appPreferences = FakePackageStore(),
            installedApps = FakeAppsProvider(emptyList()),
            pinSecurity = FakePinAuthenticator(),
        )

        advanceUntilIdle()
        viewModel.setPin("1234")
        viewModel.saveConfiguration()

        assertEquals("Select at least one approved application.", viewModel.state.value.message)
    }

    private class FakePackageStore : SelectedPackagesStore {
        private val values = MutableStateFlow<Set<String>>(emptySet())
        override val selectedPackages: Flow<Set<String>> = values
        override suspend fun saveSelectedPackages(packages: Set<String>) {
            values.value = packages
        }
    }

    private class FakeAppsProvider(
        private val apps: List<InstalledApp>,
    ) : LaunchableAppsProvider {
        override fun findLaunchableApps(): List<InstalledApp> = apps
    }

    private class FakePinAuthenticator : PinAuthenticator {
        override suspend fun isConfigured(): Boolean = false
        override suspend fun configurePin(pin: CharArray) {
            pin.fill('\u0000')
        }
        override suspend fun verifyPin(pin: CharArray): PinVerification {
            pin.fill('\u0000')
            return PinVerification.Success
        }
    }
}
