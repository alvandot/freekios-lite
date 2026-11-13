package com.freekiosklite.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "kiosk_preferences")

class AppPreferencesRepository(private val context: Context) {
    private val selectedPackagesKey = stringSetPreferencesKey("selected_packages")

    val selectedPackages: Flow<Set<String>> =
        context.appPreferencesDataStore.data.map { preferences ->
            preferences[selectedPackagesKey].orEmpty()
        }

    suspend fun saveSelectedPackages(packages: Set<String>) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[selectedPackagesKey] = packages
        }
    }
}
