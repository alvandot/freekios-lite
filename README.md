# FreeKiosk Lite

FreeKiosk Lite is a native Android multi-app kiosk foundation for shared
devices. It targets Android 12 through Android 16 and uses Kotlin, Jetpack
Compose, Material 3, AndroidX, DataStore, and Android Keystore.

## Architecture

- **UI:** Jetpack Compose and Material 3
- **Presentation:** `KioskViewModel` with a small MVVM state model
- **Data:** DataStore stores approved application package names
- **Security:** PBKDF2-HMAC-SHA256 hashes the administrator PIN with a random
  salt; the hash is encrypted with an Android Keystore AES-GCM key. Failed PIN
  attempts are rate-limited with a temporary lockout.
- **Device management:** `DevicePolicyController` and
  `KioskDeviceAdminReceiver` provide an explicit foundation for Device Owner and
  Lock Task Mode. The app never activates Device Owner or Lock Task Mode
  automatically.

The current UI provides a launcher for administrator-approved, installed
applications and an administrator configuration screen. Missing or uninstalled
packages are ignored safely.

## Requirements

- Android Studio with Android SDK 36
- JDK 17
- Gradle is provided by the repository wrapper

## Local development

```bash
./gradlew --no-daemon clean assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

`.github/workflows/android.yml` runs on pushes to `main`, pull requests to
`main`, and manual dispatch. It uses JDK 17, installs only the required Android
SDK packages, validates the Gradle wrapper, builds the debug APK, and uploads
`FreeKiosk-Lite-debug` as a workflow artifact.

## Optional Device Owner provisioning

Device Owner activation is an administrator operation and is not performed by
the application. On a freshly provisioned device, after installing the APK,
an administrator may use Android Debug Bridge:

```bash
adb shell dpm set-device-owner com.freekiosklite/.admin.KioskDeviceAdminReceiver
```

This command can fail if the device has existing accounts or has already been
provisioned. Follow the device manufacturer's provisioning procedure and keep
an out-of-band recovery path. Lock Task Mode is only started by an explicit
future administrator action after configuration validation.

## Limitations and next steps

Implemented now:

- Native Kotlin/Compose application shell
- Approved application selection and launcher
- DataStore persistence
- Keystore-backed PIN storage and attempt limiting
- Device admin and Lock Task architecture without automatic activation
- Android build workflow and debug artifact upload

Planned:

- A protected administrator authentication screen
- Explicit, user-confirmed Lock Task activation and exit flow
- Device policy capability checks and manufacturer-specific messaging
- Encrypted settings migration and UI tests
- Release signing configuration supplied outside the repository
