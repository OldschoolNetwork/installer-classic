# ON Classic Services Installer

An installer for Oldschool Network services, designed around Android's Holo interface and classic Android devices.

Choose the services you need, install ON Core Services and their dependencies, and use either ordinary APK installation or full system installation on a compatible rooted device.

**Development status:** alpha. The current work is source-only. The next installer APK will be built and published after the next ON Core update. System installation still needs device testing.

## Features

- A service selection screen with ON Core Services included as a required dependency.
- Ordinary installation through Android's package installer, without root.
- Full installation for compatible rooted devices: classic system application directories or a Magisk systemless module.
- Release selection based on the device's Android version and supported processor ABIs.
- Bundled APK integrity checks, package identity checks and checks against the certificate and version of an existing installation.
- Installation progress, useful failure messages and rollback information for system installation.
- A lightweight Holo interface with layouts editable in Android Studio.

The service pack is prepared from ON Core Services and ON Store. Additional services and compatible release variants can be declared in the local payload manifest.

## Requirements

- Android 4.0 / API 14 or newer for the installer application. Individual service APKs may have additional requirements.
- Enough free space for the installer, bundled APKs and installation backups.
- Permission to install unknown applications for ordinary installation, where required by Android.
- Root access and a supported system layout or Magisk environment for full installation.

Root access by itself does not make an installation complete. System installation must be activated by a restart and verified afterwards. Installing an app in a system directory does not grant platform-signature permissions or bypass Android's security model.

## Build

Open this directory in Android Studio. This project uses Gradle 5.4.1, Android Gradle Plugin 3.5.3, JDK 8 and Android SDK 28. APK files are not committed to the source repository. Prepare a signed service payload before building a distributable installer; see [SIGNING.md](SIGNING.md).

```text
gradlew.bat :app:testDebugUnitTest

# After preparing the signed Core and Store payload:
gradlew.bat :app:assembleRelease
```

Local service APKs live in `app/src/main/assets/apks/`. `tools/prepare-bundle.py` generates `app/src/main/assets/services.json` with their versions, checksums, compatibility and dependencies. Both the APK files and the generated manifest are ignored by Git. A release build rejects missing or debug-signed payloads. Sign the resulting installer with the same persistent ON key before publishing it.

## Installation and recovery

Ordinary installation opens Android's installer for each selected service. Full installation needs explicit root approval and a restart. Back up your device and make sure you can access recovery before changing system applications.

System installation retains rollback data and displays its recovery command. On a Magisk installation, the ON module can also be disabled through Magisk or recovery. Do not delete recovery data until the installation has been verified.

Compatibility and recovery instructions are being expanded as part of the current development cycle. No claim is made that the new system installation flow has been tested on every device or ROM.

## Links

- [Oldschool Network](https://oldschoolnetwork.online/)
- [Downloads](https://oldschoolnetwork.online/downloads/)
- [Installation documentation](https://oldschoolnetwork.online/downloads/docs/)

The updated download catalog and documentation are being prepared locally; these pages may not be published yet.

## Reporting problems

Please include your Android version, device model and a redacted installation log when reporting an issue. Never post passwords, ON Account tokens or private account information.

An open-source license has not been selected yet.

## Development notes

[CHANGELOG.md](CHANGELOG.md) describes the current changes. The dependency resolver has tests for Android/ABI selection, required services and dependency cycles. APK publication is intentionally deferred; there is no automatic upload during a source build.
