# ON Classic Installer

Installs ON services together, with a Holo interface and a choice of ordinary or full installation.

## Download

Current build: **alpha 0.29**, version code **4**.

- [Download ON Classic Installer](https://github.com/OldschoolNetwork/installer-classic/releases/download/alpha-0.29/ON-Classic-Installer-0.29.apk)
- [Release notes and checksums](https://github.com/OldschoolNetwork/installer-classic/releases/tag/alpha-0.29)
- [All ON downloads](https://oldschoolnetwork.online/downloads/)

Android **4.0 / API 14 or newer**. This is a universal APK. It uses the persistent Oldschool Network signing key.

## What it does

Select the services you need before installation. Core is included when required by another service. Ordinary installation opens Android's installer for each package. Full installation uses supported system directories or a Magisk module and retains rollback information.

Removing old Google components is optional and off by default. The installer shows the selected components and requires confirmation before proceeding. Root access alone does not grant Android platform-signature permissions.

## Included services

| Service | Version |
| --- | --- |
| ON Core Services | 0.52 |
| ON Store | 0.32 |

## Build

Use JDK 8, Android SDK 28, Android Gradle Plugin 3.5.3 and Gradle 5.4.1. Open the project in Android Studio or use the Gradle wrapper.

Prepare the signed Core and Store APKs with `tools/prepare-bundle.py` before building a release. The APKs and generated `services.json` are local payload files and are excluded from Git. See [SIGNING.md](SIGNING.md) for signing and bundle preparation.

```text
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleRelease
```

The release build checks the payload files and their checksums. Building does not publish a release.

## Alpha status

These builds are under development. Android 5.1 checks have passed, but that does not establish compatibility with every device or ROM. Full root installation, Google component removal, Magisk overlays and silent updates still need testing on physical devices. Keep a backup and recovery access before using system installation.

The ON release key differs from the old Android debug key. Android cannot update those old development packages in place; plan a one-time migration and keep any data you need.

## Help and problems

- [Oldschool Network](https://oldschoolnetwork.online/)
- [Documentation](https://oldschoolnetwork.online/downloads/docs/)
- [Changelog](CHANGELOG.md)

Include your device model, Android version, application version and the steps that led to a problem. Remove account tokens and private information from logs before posting them.
