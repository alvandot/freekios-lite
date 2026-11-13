package com.freekiosklite.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.freekiosklite.data.InstalledApp
import com.freekiosklite.data.AppLaunchResolver
import com.freekiosklite.ui.theme.FreeKioskTheme

@Composable
fun KioskApp(viewModel: KioskViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    FreeKioskTheme {
        Scaffold { padding ->
            if (state.isLoading) {
                Text("Loading kiosk configuration...", Modifier.padding(padding).padding(24.dp))
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(padding),
                ) {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            onOpenAdmin = { navController.navigate("admin") },
                        )
                    }
                    composable("admin") {
                        if (state.isPinConfigured && !state.isAdminUnlocked) {
                            AdminAuthScreen(
                                onBack = { navController.popBackStack() },
                                onUnlock = viewModel::unlockAdmin,
                            )
                        } else {
                            AdminScreen(
                                state = state,
                                onBack = {
                                    viewModel.lockAdmin()
                                    navController.popBackStack()
                                },
                                onPinChanged = viewModel::setPin,
                                onTogglePackage = viewModel::togglePackage,
                                onSave = viewModel::saveConfiguration,
                            )
                        }
                    }
                }
            }
        }
    }

    state.message?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun HomeScreen(
    state: KioskUiState,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedApps = state.installedApps.filter { it.packageName in state.selectedPackages }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FreeKiosk Lite", style = MaterialTheme.typography.headlineLarge)
        Text("Multi-app kiosk launcher", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kiosk status", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (selectedApps.isEmpty()) {
                        "Not configured"
                    } else {
                        "${selectedApps.size} approved application(s)"
                    },
                )
            }
        }
        if (selectedApps.isEmpty()) {
            Text("Configure approved applications before using this shared-device launcher.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(selectedApps, key = InstalledApp::packageName) { app ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launchApp(context, app.packageName) },
                    ) {
                        Text(app.label)
                    }
                }
            }
        }
        TextButton(onClick = onOpenAdmin) {
            Text("Administrator configuration")
        }
    }
}

@Composable
private fun AdminAuthScreen(
    onBack: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Administrator authentication", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(12) },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onUnlock(pin) }, modifier = Modifier.fillMaxWidth()) {
            Text("Unlock")
        }
    }
}

@Composable
private fun AdminScreen(
    state: KioskUiState,
    onBack: () -> Unit,
    onPinChanged: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Administrator setup", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Done") }
        }
        Text("Only selected launchable applications are shown on the kiosk home screen.")
        OutlinedTextField(
            value = state.pin,
            onValueChange = onPinChanged,
            label = { Text("Administrator PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f)) {
            items(state.installedApps, key = InstalledApp::packageName) { app ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = app.packageName in state.selectedPackages,
                        onCheckedChange = { onTogglePackage(app.packageName) },
                    )
                    Text(app.label, Modifier.padding(start = 8.dp))
                }
            }
        }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save configuration")
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = AppLaunchResolver(context.packageManager).launchIntent(packageName)
    if (intent == null) {
        Toast.makeText(context, "Application is unavailable.", Toast.LENGTH_SHORT).show()
    } else {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
