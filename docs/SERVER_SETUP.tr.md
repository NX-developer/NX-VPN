# Sunucu kurulumu

[English](SERVER_SETUP.md) | **Türkçe**

NX-VPN bir istemcidir; çıkış sunucusunu sen sağlarsın. En kolay ve en hızlı yol **WireGuard**'dır
(uygulamada tam çalışır). Aşağıda iki seçenek var.

> Sunucu kurmak istemiyor musun? **Free sekmesi**nden tek dokunuşla halka açık VPN Gate sunucularına
> bağlanabilirsin (gönüllü işletilir ve güvenilmez kabul edilmelidir — bkz. README).

---

## Seçenek A — WireGuard, `wg-easy` ile (önerilen)

`wg-easy`, web arayüzlü, Docker tabanlı bir WireGuard sunucusudur. Birkaç dakikada ABD/Almanya vb.
bir VPS'te ayağa kalkar.

### 1. Bir VPS kirala
Örn. ABD lokasyonu için Hetzner, DigitalOcean, Vultr, AWS Lightsail. Ubuntu 22.04 yeterli.

### 2. Docker + wg-easy kur

```bash
# Docker
curl -fsSL https://get.docker.com | sh

# wg-easy (WG_HOST = sunucunun genel IP'si; PASSWORD = panel şifresi)
docker run -d --name wg-easy \
  -e WG_HOST=<SUNUCU_GENEL_IP> \
  -e PASSWORD=<PANEL_SIFRESI> \
  -e WG_DEFAULT_DNS=1.1.1.1 \
  -v ~/.wg-easy:/etc/wireguard \
  -p 51820:51820/udp \
  -p 51821:tcp \
  --cap-add NET_ADMIN --cap-add SYS_MODULE \
  --sysctl net.ipv4.ip_forward=1 \
  --sysctl net.ipv4.conf.all.src_valid_mark=1 \
  --restart unless-stopped \
  ghcr.io/wg-easy/wg-easy
```

> Güvenlik duvarında **51820/udp** (tünel) ve panele erişmek için **51821/tcp** açık olmalı.

### 3. İstemci oluştur ve `.conf` indir
`http://<SUNUCU_IP>:51821` adresinden panele gir → **New Client** → oluşan QR/`.conf` dosyasını indir.

### 4. NX-VPN'e aktar
Uygulama → **Servers** → **+** → `.conf` dosyasını seç (veya içeriğini yapıştır) → **Import** → bağlan.

---

## Seçenek B — OpenVPN (`.ovpn`)

OpenVPN sunucusu için `angristan/openvpn-install` script'i pratiktir:

```bash
curl -O https://raw.githubusercontent.com/angristan/openvpn-install/master/openvpn-install.sh
chmod +x openvpn-install.sh
sudo ./openvpn-install.sh   # sorulara cevap ver, bir istemci adı gir → <istemci>.ovpn üretir
```

Üretilen `.ovpn` dosyası NX-VPN'e içe aktarılıp saklanır ve arayüzde görünür.

> **OpenVPN tüneli nasıl kurulur:** NX-VPN, asıl OpenVPN tünelini açık kaynak **OpenVPN for Android**
> motoruna dış API üzerinden devreder. Bir OpenVPN profiline bağlanırken o uygulama kurulu değilse
> NX-VPN seni tek seferlik kurulum için F-Droid sayfasına yönlendirir. Bkz. README "Protokol durumu".

---

## Hızlı test ipucu
Önce ücretsiz/halka açık bir WireGuard config'i ile arayüzü test edebilirsin; ardından kendi
sunucunu kurup gerçek "ABD çıkışı" elde edersin.
