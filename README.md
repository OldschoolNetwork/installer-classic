# ON Classic Installer

Installs ON services and their dependencies on classic Android devices.

Current alpha: **0.35** (version code 10). Android 4.0 / API 14 or later.

## Features

- Choose Core and optional ON services.
- Install as ordinary apps or use full system setup on a compatible rooted device.
- Validate bundled package identities, versions, checksums and signing certificates.

## Downloads

Get signed APKs from [GitHub Releases](https://github.com/OldschoolNetwork/installer-classic/releases). Use [All downloads](https://oldschoolnetwork.online/downloads/) for individual services and archived versions. The main Download button provides the Classic Installer.

## Installation

Read the [installation guides](https://oldschoolnetwork.online/downloads/docs/categories/installation/). Full system setup requires a compatible rooted device; ordinary installation uses Android's package installer. Update an existing installation with APKs signed by the same certificate.

This is alpha software. Physical-device and firmware compatibility can differ from emulator results.

## Build

Use JDK 8, Android SDK 28 and the included Gradle 5.4.1 wrapper. Prepare the signed service APKs with `tools/prepare-bundle.py`, then run `gradlew.bat :app:testDebugUnitTest :app:assembleRelease`. The release build verifies package checksums and certificate metadata.

The 0.35 payload contains Core 0.57 and Store 0.38. APK payloads and the generated `services.json` stay outside Git. See [SIGNING.md](SIGNING.md) for signing and bundle preparation.

Full system installation and silent updates still need physical-device testing for this release. Builds signed with the ON release key cannot update older debug-signed packages in place.

## Links

[Website](https://oldschoolnetwork.online/) · [Help](https://oldschoolnetwork.online/downloads/docs/) · [Platform Rules](https://api.oldschoolnetwork.online/v1/on-account/web/terms.html)

[Bluesky](https://bsky.app/profile/oldnet.bsky.social) · [YouTube](https://www.youtube.com/@oldschoolnetwork) · [X](https://x.com/oldschool_net) · [Telegram](https://t.me/OldschoolNetwork)
