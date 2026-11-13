package com.freekiosklite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freekiosklite.data.AppPreferencesRepository
import com.freekiosklite.data.InstalledApp
import com.freekiosklite.data.InstalledAppsRepository
import com.freekiosklite.data.LaunchableAppsProvider
import com.freekiosklite.data.SelectedPackagesStore
import com.freekiosklite.security.PinAuthenticator
import com.freekiosklite.security.PinSecurity
import com.freekiosklite.security.PinVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class KioskUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val pin: String = "",
    val message: String? = null,
    val isLoading: Boolean = true,
    val isPinConfigured: Boolean = false,
    val isAdminUnlocked: Boolean = false,
)

class KioskViewModel(
    application: Application,
    private val appPreferences: SelectedPackagesStore = AppPreferencesRepository(application),
    private val installedApps: LaunchableAppsProvider = InstalledAppsRepository(application),
    private val pinSecurity: PinAuthenticator = PinSecurity(application),
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(KioskUiState())
    val state: StateFlow<KioskUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val selected = appPreferences.selectedPackages.first()
            _state.value = KioskUiState(
                installedApps = installedApps.findLaunchableApps(),
                selectedPackages = selected,
                isPinConfigured = pinSecurity.isConfigured(),
                isLoading = false,
            )
        }
    }

    fun setPin(pin: String) {
        _state.value = _state.value.copy(pin = pin.filter(Char::isDigit).take(12))
    }

    fun togglePackage(packageName: String) {
        val selected = _state.value.selectedPackages.toMutableSet()
        if (!selected.add(packageName)) selected.remove(packageName)
        _state.value = _state.value.copy(selectedPackages = selected)
    }

    fun saveConfiguration() {
        val current = _state.value
        if (current.selectedPackages.isEmpty()) {
            _state.value = current.copy(message = "Select at least one approved application.")
            return
        }
        if (current.pin.length < 4) {
            _state.value = current.copy(message = "Set an administrator PIN with at least 4 digits.")
            return
        }

        viewModelScope.launch {
            pinSecurity.configurePin(current.pin.toCharArray())
            appPreferences.saveSelectedPackages(current.selectedPackages)
            _state.value = _state.value.copy(
                pin = "",
                isPinConfigured = true,
                isAdminUnlocked = true,
                message = "Configuration saved. Lock Task activation remains explicit.",
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    suspend fun verifyPin(pin: CharArray): PinVerification = pinSecurity.verifyPin(pin)

    fun unlockAdmin(pin: String) {
        viewModelScope.launch {
            when (val result = pinSecurity.verifyPin(pin.toCharArray())) {
                PinVerification.Success -> {
                    _state.value = _state.value.copy(isAdminUnlocked = true, message = null)
                }
                is PinVerification.Failure -> {
                    _state.value = _state.value.copy(message = "Invalid PIN.")
                }
                PinVerification.Locked -> {
                    _state.value = _state.value.copy(message = "PIN locked temporarily.")
                }
                PinVerification.NotConfigured -> {
                    _state.value = _state.value.copy(message = "Administrator PIN is not configured.")
                }
            }
        }
    }

    fun lockAdmin() {
        _state.value = _state.value.copy(isAdminUnlocked = false)
    }
}
