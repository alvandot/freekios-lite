package com.freekiosklite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freekiosklite.data.AppPreferencesRepository
import com.freekiosklite.data.InstalledApp
import com.freekiosklite.data.InstalledAppsRepository
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
)

class KioskViewModel(application: Application) : AndroidViewModel(application) {
    private val appPreferences = AppPreferencesRepository(application)
    private val installedApps = InstalledAppsRepository(application)
    private val pinSecurity = PinSecurity(application)
    private val _state = MutableStateFlow(KioskUiState())
    val state: StateFlow<KioskUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val selected = appPreferences.selectedPackages.first()
            _state.value = KioskUiState(
                installedApps = installedApps.findLaunchableApps(),
                selectedPackages = selected,
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
                message = "Configuration saved. Lock Task activation remains explicit.",
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    suspend fun verifyPin(pin: CharArray): PinVerification = pinSecurity.verifyPin(pin)
}
