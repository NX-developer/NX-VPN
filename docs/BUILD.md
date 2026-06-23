# Derleme ve sürüm yayınlama

## CI ne yapar? (`.github/workflows/build.yml`)

Her `main` push'unda / PR'da:
1. JDK 17 + Android SDK kurulur
2. `./gradlew testDebugUnitTest` — birim testleri
3. `./gradlew assembleDebug` — debug APK
4. APK, **Actions → ilgili çalışma → Artifacts → `nx-vpn-debug`** altına yüklenir

Bir sürüm etiketi (`v*`) push edildiğinde ayrıca:
5. `./gradlew assembleRelease` — release APK
6. **GitHub Release** oluşturulur ve APK'lar eklenir

```bash
git tag v1.0.0
git push origin v1.0.0
```

## İmzalı release (opsiyonel)

İmzasız da APK üretilir (test için). Play Store / kalıcı dağıtım için imzala:

1. Bir keystore üret:
   ```bash
   keytool -genkey -v -keystore nxvpn.keystore -alias nxvpn \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64'e çevir: `base64 -w0 nxvpn.keystore` (çıktıyı kopyala)
3. GitHub repo → **Settings → Secrets and variables → Actions** altına ekle:
   - `KEYSTORE_BASE64` → yukarıdaki base64
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
4. Bir `v*` etiketi push et — release APK otomatik imzalanır.

> `nxvpn.keystore` ve şifreleri **asla** repoya koyma. `.gitignore` bunları zaten dışlar.

## Yerel derleme

Android SDK kuruluysa:
```bash
./gradlew assembleDebug
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
```
`adb install -r app/build/outputs/apk/debug/app-debug.apk` ile cihaza kurabilirsin.
