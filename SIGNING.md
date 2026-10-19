# APK signing

Use Android SDK `apksigner` to sign and verify APKs. Run `zipalign` before signing; changing an APK after signing invalidates its signature.

ON uses a persistent signing key shared by its service APKs. The private keystore and its recovery information live outside this repository. They must never be committed or uploaded as release assets.

The helper scripts target the Windows development setup. `tools/sign-apk.py` reads the password from Windows Credential Manager and passes it to the signing tool through the subprocess environment, not a command-line argument.

Example in PowerShell:

```powershell
python tools/sign-apk.py sign `
  --input path/to/app-release-unsigned.apk `
  --output releases/ON-Classic-Installer.apk `
  --keystore path/to/private/on-release.jks `
  --jdk path/to/jdk8 `
  --sdk path/to/Android/Sdk
```

The script aligns the APK, enables signatures compatible with Android API 14 and verifies the result. It does not upload anything.

To prepare the installer payload from already signed service APKs:

```powershell
python tools/prepare-bundle.py `
  --core path/to/ON-Core-Services.apk `
  --store path/to/ON-Store.apk `
  --jdk path/to/jdk8 `
  --sdk path/to/Android/Sdk
```

The preparation step checks package identities and signatures, requires the same signing certificate for Core and Store, copies the APKs into the local assets directory and generates `services.json`. Release builds reject a missing payload, a debug-signed payload or a checksum mismatch.

The public certificate is in `docs/on-release-public.pem`. It can be used to compare the signing identity of future APKs; it contains no private key.

An app signed with the new ON key cannot update an older development build signed with the Android debug key. That development installation needs a one-time migration. Keep the persistent ON key for all subsequent updates.

APK publication is currently deferred until the next Core update. No APK is published by the source build or signing scripts.

Reference: [Android apksigner documentation](https://developer.android.com/tools/apksigner).
