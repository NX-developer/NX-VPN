# Building and releasing

**English** | [Türkçe](BUILD.tr.md)

## What does CI do? (`.github/workflows/build.yml`)

The workflow builds **only on version tags (`v*`)** and via the manual **Run workflow** button
(`workflow_dispatch`). Plain pushes to `main` do **not** trigger a build (this used to fire a
duplicate run whenever a tag pointed at the same commit).

On a `v*` tag:
1. JDK 17 + Android SDK are set up
2. `./gradlew testDebugUnitTest` — unit tests
3. `./gradlew assembleDebug` — debug APK
4. The debug APK is uploaded under **Actions → the run → Artifacts → `nx-vpn-debug`**
5. `./gradlew assembleRelease` — release APK
6. A **GitHub Release** is created with the APKs attached

```bash
git tag v1.0.1
git push origin v1.0.1
```

## Signed release (optional)

An unsigned APK is produced by default (fine for testing). For the Play Store / permanent
distribution, sign it:

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore nxvpn.keystore -alias nxvpn \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64-encode it: `base64 -w0 nxvpn.keystore` (copy the output)
3. Add the following under GitHub repo → **Settings → Secrets and variables → Actions**:
   - `KEYSTORE_BASE64` → the base64 from above
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
4. Push a `v*` tag — the release APK is signed automatically.

> **Never** commit `nxvpn.keystore` or its passwords. `.gitignore` already excludes them.

## Local build

With the Android SDK installed:
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```
Install it on a device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
