# NX-VPN

Açık kaynaklı, Android için sade bir VPN istemcisi. İki şekilde kullanılır:

1. **Hazır sunucular (Free sekmesi):** Tek dokunuşla, ücretsiz halka açık **VPN Gate** (Tsukuba
   Üniversitesi'nin akademik gönüllü ağı) OpenVPN sunucularına bağlan. Kendi sunucunu kurmana gerek yok.
2. **Kendi sunucun:** Kendi **WireGuard (`.conf`)** veya **OpenVPN (`.ovpn`)** konfigürasyonunu içe
   aktarıp bağlan.

APK, GitHub Actions üzerinde otomatik olarak derlenir.

> **Lisans:** GPL-3.0 — bkz. [`LICENSE`](LICENSE).

---

## Özellikler

- 🟢 **Free sekmesi:** internetten canlı çekilen ücretsiz VPN Gate sunucuları (ülke, ping, hız)
- 🔌 Tek dokunuşla bağlan / kes (büyük güç butonu)
- 🌍 Ülke bayrağı tespiti (her ülke için emoji)
- 📥 Config içe aktarma: dosya seç **veya** metni yapıştır
- 📊 Canlı süre + indirme/yükleme trafiği sayaçları (WireGuard)
- 🎨 Material 3 arayüz, koyu tema, dinamik renk (Android 12+)
- 🔒 Kendi konfigürasyonların cihazda DataStore ile saklanır; hiçbir veri dışarı gönderilmez

## Protokol durumu

| Protokol  | Durum | Notlar |
|-----------|-------|--------|
| WireGuard | ✅ Tamamen gömülü | Resmi `com.wireguard.android:tunnel` (gömülü Go backend) ile uygulama içinde çalışır. |
| OpenVPN   | ✅ Çalışır (motor uygulamasıyla) | Tünel, açık kaynak **OpenVPN for Android** motoruna resmi dış API (AIDL) ile devredilir. NX-VPN, sunucu listesini/config'i tutar; bağlantıyı motor uygulaması kurar. |

> **OpenVPN nasıl çalışır?** NX-VPN'in tek başına OpenVPN'in native tünel kodunu gömmesi büyük bir
> NDK derlemesi ve `ics-openvpn` fork bakımı gerektirir. Bunun yerine, açık kaynak **OpenVPN for
> Android** uygulamasının resmi dış API'sini kullanıyoruz: bağlanırken cihazda o uygulama kurulu
> değilse NX-VPN seni F-Droid sayfasına yönlendirir (tek seferlik kurulum). Böylece varsayılan
> derleme native dert olmadan her zaman yeşil kalır.
>
> OpenVPN for Android: <https://f-droid.org/packages/de.blinkt.openvpn/>

## Hazır sunucular (VPN Gate) hakkında

Free sekmesindeki sunucular **gönüllüler** tarafından işletilir (VPN Gate, Tsukuba Üniversitesi).
Gelip gidebilir, yavaş olabilir ve güvenilmez kabul edilmelidir; hassas trafik için kendi sunucunu
kurman önerilir. Kaynak: <https://www.vpngate.net/>.

---

## Sunucular (önemli)

İki yol var:

- **Hızlı/ücretsiz:** Free sekmesinden VPN Gate sunucularına bağlan (gönüllü, halka açık — yukarıdaki
  uyarıya bak).
- **Kendi çıkış noktan:** Tam kontrol ve gizlilik için açık kaynaklı bir VPN sunucusu kur (örn. bir ABD
  VPS'inde **WireGuard / `wg-easy`**), üreteceğin `.conf` dosyasını içe aktar ve bağlan.

Adım adım kendi sunucu kurulumu: [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md).

---

## Derleme (APK)

Yerel makinede Android SDK ile:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # birim testleri
```

GitHub'a push ettiğinde **GitHub Actions** otomatik olarak testleri çalıştırır, debug APK derler
ve **Artifacts** altına yükler. `v1.0.0` gibi bir etiket (tag) gönderirsen imzalı/imzasız release
APK üretir ve bir **GitHub Release** yayınlar. Ayrıntılar: [`docs/BUILD.md`](docs/BUILD.md).

---

## GitHub'a yükleme (senin yapacağın adımlar)

> ⚠️ Anahtarını (token) bana veya herkese açık bir yere **yazma**. Aşağıdaki adımları kendin çalıştır.

```bash
# 1) Bu klasörde:
cd NX-VPN
git init && git add . && git commit -m "NX-VPN: ilk sürüm"
git branch -M main

# 2) GitHub'da boş bir repo aç (örn. github.com/<kullanıcı>/NX-VPN), sonra:
git remote add origin https://github.com/<kullanıcı>/NX-VPN.git

# 3) Push (token'ı şifre alanına yapıştırırsın; ekranda görünmez)
git push -u origin main
```

Push biter bitmez **Actions** sekmesinde derleme başlar; bitince APK'yı **Artifacts**'tan indirebilirsin.

---

## Proje yapısı

```
app/src/main/java/com/nxvpn/app/
├── MainActivity.kt          VPN izni + OpenVPN motor izinleri + dosya seçme akışları
├── NxVpnApplication.kt      Singleton'lar (repo + VpnManager)
├── data/                    Modeller, config içe aktarma, DataStore deposu, VPN Gate çekme
├── vpn/                     VpnManager + WireGuard tüneli + OpenVpnConnector (dış API)
└── ui/                      Compose ekranları (Home, Free, Servers, Import) + tema

app/src/main/aidl/de/blinkt/openvpn/api/   OpenVPN for Android dış API (AIDL, GPL'den muaf)
```

## Yasal uyarı

Bu yazılım yalnızca yasal ve yetkili kullanım içindir. VPN kullanımına ilişkin yerel yasalara
uymak kullanıcının sorumluluğundadır.
