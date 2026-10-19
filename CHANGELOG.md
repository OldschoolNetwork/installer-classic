# Changes

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
