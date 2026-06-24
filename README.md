# NX-VPN

**English** | [Türkçe](README.tr.md)

A simple, open-source VPN client for Android. It can be used in two ways:

1. **Ready-to-use servers (Free tab):** Connect with a single tap to free, public **VPN Gate**
   (the academic volunteer network of the University of Tsukuba) OpenVPN servers. No need to set up
   your own server.
2. **Your own server:** Import and connect with your own **WireGuard (`.conf`)** or
   **OpenVPN (`.ovpn`)** configuration.

The APK is built automatically on GitHub Actions.

> **License:** GPL-3.0 — see [`LICENSE`](LICENSE).

---

## Features

- 🟢 **Free tab:** free VPN Gate servers fetched live from the internet (country, ping, speed)
- 🔎 **Search:** filter the server list by country or name (matches both the localized and the
  English country name, so e.g. "Japonya" and "Japan" both find Japan)
- 🌐 **Localized:** English and Turkish; the app follows the system language and falls back to
  English for any other locale
- 🔌 One-tap connect / disconnect (large power button)
- 🌍 Country flag detection (an emoji per country)
- 📥 Config import: pick a file **or** paste the text
- 📊 Live duration + download/upload traffic counters
- 🎨 Material 3 UI, dark theme, dynamic color (Android 12+)
- 🔒 Your own configs are stored on-device with DataStore; no data is sent anywhere

## Protocol status

| Protocol  | Status | Notes |
|-----------|--------|-------|
| WireGuard | ✅ Fully embedded | Runs in-process via the official `com.wireguard.android:tunnel` (bundled Go backend). |
| OpenVPN   | ✅ Works (via engine app) | The tunnel is delegated to the open-source **OpenVPN for Android** engine over its official external API (AIDL). NX-VPN holds the server list/config; the engine app brings up the connection. |

> **How does OpenVPN work?** Embedding OpenVPN's native tunnel code directly would require a large
> NDK build and maintaining an `ics-openvpn` fork. Instead, we use the official external API of the
> open-source **OpenVPN for Android** app: when connecting, if that app isn't installed NX-VPN sends
> you to its F-Droid page (a one-time install). This keeps the default build green without native
> headaches.
>
> OpenVPN for Android: <https://f-droid.org/packages/de.blinkt.openvpn/>

> **Note on traffic counters:** WireGuard reports exact per-tunnel byte counts from the backend.
> The OpenVPN engine does not expose per-tunnel counters over its external API, so on OpenVPN
> connections the download/upload figures are *approximated* from device-wide totals since connect
> (while connected, virtually all traffic flows through the tunnel).

## About the ready-to-use servers (VPN Gate)

The servers in the Free tab are operated by **volunteers** (VPN Gate, University of Tsukuba).
They come and go, may be slow, and should be treated as untrusted; for sensitive traffic, set up
your own server. Source: <https://www.vpngate.net/>.

---

## Servers (important)

There are two paths:

- **Fast/free:** connect to VPN Gate servers from the Free tab (volunteer, public — see the warning
  above).
- **Your own exit node:** for full control and privacy, set up an open-source VPN server (e.g.
  **WireGuard / `wg-easy`** on a US VPS), import the `.conf` it generates, and connect.

Step-by-step self-hosting guide: [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md).

---

## Building (APK)

Locally, with the Android SDK:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests
```

On CI, the **GitHub Actions** workflow builds only on version tags (e.g. `v1.0.1`): it runs the
unit tests, builds the APK, uploads it under **Artifacts**, and publishes a **GitHub Release**.
Plain pushes to `main` no longer trigger a build — use the **Run workflow** button
(`workflow_dispatch`) for an ad-hoc build. Details: [`docs/BUILD.md`](docs/BUILD.md).

To cut a release:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

> Release APKs are unsigned unless the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
> `KEY_PASSWORD` repository secrets are configured.

---

## Project structure

```
app/src/main/java/com/nxvpn/app/
├── MainActivity.kt          VPN consent + OpenVPN engine consent + file-picker flows
├── NxVpnApplication.kt      Singletons (repo + VpnManager)
├── data/                    Models, config import, DataStore repo, VPN Gate fetch
├── vpn/                     VpnManager + WireGuard tunnel + OpenVpnConnector (external API)
└── ui/                      Compose screens (Home, Free, Servers, Import) + theme

app/src/main/aidl/de/blinkt/openvpn/api/   OpenVPN for Android external API (AIDL, exempt from GPL)
app/src/main/res/values/, values-tr/       English (default) and Turkish strings
```

## Legal notice

This software is intended only for lawful and authorized use. It is the user's responsibility to
comply with local laws regarding VPN usage.
