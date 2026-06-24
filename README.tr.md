# NX-VPN

[English](README.md) | **Türkçe**

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
- 🔎 **Arama:** sunucu listesini ülke veya ada göre süz (hem yerelleştirilmiş hem İngilizce ülke adını
  eşler; ör. "Japonya" da "Japan" da Japonya'yı bulur)
- 🌐 **Çok dilli:** İngilizce ve Türkçe; uygulama sistem dilini izler, başka bir dilde İngilizce'ye düşer
- 🔌 Tek dokunuşla bağlan / kes (büyük güç butonu)
- 🌍 Ülke bayrağı tespiti (her ülke için emoji)
- 📥 Config içe aktarma: dosya seç **veya** metni yapıştır
- 📊 Canlı süre + indirme/yükleme trafiği sayaçları
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

> **Trafik sayaçları hakkında not:** WireGuard, backend'den tünel-başı kesin byte sayılarını verir.
> OpenVPN motoru dış API'sinde tünel-başı sayaç sunmadığı için OpenVPN bağlantılarında indirme/yükleme
> değerleri, bağlantıdan beri cihaz geneli toplamlardan *yaklaşık* olarak hesaplanır (bağlıyken
> neredeyse tüm trafik tünelden akar).

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

Adım adım kendi sunucu kurulumu: [`docs/SERVER_SETUP.tr.md`](docs/SERVER_SETUP.tr.md).

---

## Derleme (APK)

Yerel makinede Android SDK ile:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # birim testleri
```

CI tarafında **GitHub Actions** akışı yalnızca sürüm etiketlerinde (ör. `v1.0.1`) derler: birim
testlerini çalıştırır, APK'yı derler, **Artifacts** altına yükler ve bir **GitHub Release** yayınlar.
`main`'e düz push artık derleme tetiklemez — anlık derleme için **Run workflow** butonunu
(`workflow_dispatch`) kullan. Ayrıntılar: [`docs/BUILD.tr.md`](docs/BUILD.tr.md).

Sürüm yayınlamak için:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

> Release APK'ları, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` ve `KEY_PASSWORD` depo
> secret'ları tanımlı değilse imzasız çıkar.

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
app/src/main/res/values/, values-tr/       İngilizce (varsayılan) ve Türkçe metinler
```

## Yasal uyarı

Bu yazılım yalnızca yasal ve yetkili kullanım içindir. VPN kullanımına ilişkin yerel yasalara
uymak kullanıcının sorumluluğundadır.
