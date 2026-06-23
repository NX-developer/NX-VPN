# NX-VPN

Açık kaynaklı, Android için sade bir VPN istemcisi. Kullanıcı kendi **WireGuard (`.conf`)** veya
**OpenVPN (`.ovpn`)** sunucu konfigürasyonunu içe aktarır ve tek dokunuşla bağlanır. APK,
GitHub Actions üzerinde otomatik olarak derlenir.

> **Lisans:** GPL-3.0 — bkz. [`LICENSE`](LICENSE).

---

## Özellikler

- 🔌 Tek dokunuşla bağlan / kes (büyük güç butonu)
- 🌍 Sunucu listesi, ülke bayrağı tespiti (US, DE, TR, NL, …)
- 📥 Config içe aktarma: dosya seç **veya** metni yapıştır
- 📊 Canlı süre + indirme/yükleme trafiği sayaçları
- 🎨 Material 3 arayüz, koyu tema, dinamik renk (Android 12+)
- 🔒 Konfigürasyonlar cihazda DataStore ile saklanır; hiçbir veri dışarı gönderilmez

## Protokol durumu

| Protokol  | Durum | Notlar |
|-----------|-------|--------|
| WireGuard | ✅ Çalışır | Resmi `com.wireguard.android:tunnel` (gömülü Go backend) ile tam entegre. |
| OpenVPN   | 🚧 Hazırlanıyor | Config içe aktarılır/saklanır ve arayüzde görünür. Tünel kurma için `ics-openvpn` backend modülünün eklenmesi gerekir — bkz. [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md). |

> **Neden böyle?** WireGuard kütüphanesi APK'ya hazır gömülür ve CI'da sorunsuz derlenir.
> OpenVPN'in yerel (native) tünel kodu ayrı bir GPL modülü (`ics-openvpn`) ister ve CI'da
> ek NDK derlemesi gerektirir; bunu açık ve ayrı bir adım olarak ekledik ki varsayılan derleme
> her zaman yeşil kalsın.

---

## Sunucular (önemli)

NX-VPN bir **istemcidir** — kendi başına "Amerika sunucusu" sağlamaz. Gerçek bir çıkış noktası için
açık kaynaklı bir VPN sunucusu kurman gerekir (örn. bir ABD VPS'inde **WireGuard / `wg-easy`**).
Kurduğunda üreteceğin `.conf` dosyasını uygulamaya içe aktarırsın ve bağlanırsın.

Adım adım sunucu kurulumu: [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md).

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
├── MainActivity.kt          VPN izni + dosya seçme akışları
├── NxVpnApplication.kt      Singleton'lar (repo + VpnManager)
├── data/                    Modeller, config içe aktarma, DataStore deposu
├── vpn/                     VpnManager + WireGuard tünel sarmalayıcı
└── ui/                      Compose ekranları (Home, Servers, Import) + tema
```

## Yasal uyarı

Bu yazılım yalnızca yasal ve yetkili kullanım içindir. VPN kullanımına ilişkin yerel yasalara
uymak kullanıcının sorumluluğundadır.
