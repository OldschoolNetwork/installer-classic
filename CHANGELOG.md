# Changes

## 0.32 — 2026-10-19

Installer 0.32 includes Core 0.55, which fixes the crash before the verification screen, and Store 0.34. Both embedded APKs use the persistent ON key.

## 0.31 — 2026-10-19

Installer 0.31 includes signed Core 0.54 and Store 0.34.

- Updated service payload, versions, checksums and certificate metadata.
- Core now declares the privileged installation permission needed for silent service updates. Full setup prepares the applicable permission allowlist.
- The signing helper reports tool diagnostics from either output stream when a tool fails.

All APKs use the persistent ON key. The bundled service files are checked against the signed releases. Installer unit checks passed. Root installation on physical devices still needs testing.

## 0.30 — 2026-10-19

Installer 0.30 bundles signed Core 0.53 and Store 0.33.

- Updated the service payload and its version, checksum and signing-certificate metadata.
- The Store payload declares Android's privileged installation permission; full installation prepares the applicable permission allowlist.
- Existing ordinary/full installation choices and the optional Google-component removal confirmation remain in place.

All three APKs use the persistent ON signing key. The bundled service APKs are checked against the signed release files before publication. Full installation and silent updates still need verification on rooted physical devices.

## 0.29 — 2026-10-19

ON Classic Installer 0.29 includes signed ON Core Services 0.52 and ON Store 0.32.

- Choose the ON services to install; required dependencies are included automatically.
- Ordinary installation uses Android's package installer. Full installation offers supported system-directory or Magisk paths with rollback data.
- Bundled packages are checked against their identities, versions, signing certificates and SHA-256 checksums.
- Android version and processor compatibility are checked before installation.
- Download/preparation and installation notifications, permission prompts and clearer progress.
- Optional removal of selected old Google components, disabled by default, with a review and confirmation step.
- Holo layouts, spacing, branding and About links updated for the project.

Requires Android 4.0 (API 14) or newer. Installer, Core and Store are signed with the same persistent ON key. The embedded APKs and checksums have been verified.

Full root installation, Google component removal, Magisk overlays and post-reboot behaviour have not been tested on a rooted physical device. Back up the device and retain recovery access before using those options. Ordinary installation does not remove Google components.

Old debug-signed installations cannot be updated in place with the ON release key.

## 0.28 — in development

- Replaced the old test payload flow with ON service selection and dependency resolution.
- Added APK identity, checksum, compatibility and signing-certificate checks.
- Reworked ordinary installation to install selected services in order.
- Added system installation code with owned ON directories, rollback data and a Magisk module path.
- Added privileged permission allowlists based on permissions actually requested by the selected APKs.
- Rebuilt the selection and About screens with Holo styling, consistent spacing and scrolling.
- Corrected the website and documentation links.
- Added scripts for persistent-key signing and preparing a signed installer payload.

The system installation flow still needs device testing. APK builds and publication are deferred until the next Core update.

## 0.22 — archived test build

This build contains the original Total Commander test payload. It does not install the current ON service pack and is not included in the source repository's payload.
