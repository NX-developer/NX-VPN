# Server setup

**English** | [Türkçe](SERVER_SETUP.tr.md)

NX-VPN is a client; you provide the exit server. The easiest and fastest path is **WireGuard**
(fully supported in the app). Two options are described below.

> Prefer not to run a server? Use the **Free tab** to connect to public VPN Gate servers with one
> tap (volunteer-run and untrusted — see the README).

---

## Option A — WireGuard, with `wg-easy` (recommended)

`wg-easy` is a Docker-based WireGuard server with a web UI. It comes up on a US/Germany/etc. VPS in
a few minutes.

### 1. Rent a VPS
E.g. Hetzner, DigitalOcean, Vultr, AWS Lightsail for a US location. Ubuntu 22.04 is enough.

### 2. Install Docker + wg-easy

```bash
# Docker
curl -fsSL https://get.docker.com | sh

# wg-easy (WG_HOST = the server's public IP; PASSWORD = panel password)
docker run -d --name wg-easy \
  -e WG_HOST=<SERVER_PUBLIC_IP> \
  -e PASSWORD=<PANEL_PASSWORD> \
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

> The firewall must allow **51820/udp** (tunnel) and **51821/tcp** to reach the panel.

### 3. Create a client and download the `.conf`
Open the panel at `http://<SERVER_IP>:51821` → **New Client** → download the generated QR/`.conf`.

### 4. Import into NX-VPN
App → **Servers** → **+** → pick the `.conf` file (or paste its contents) → **Import** → connect.

---

## Option B — OpenVPN (`.ovpn`)

For an OpenVPN server, the `angristan/openvpn-install` script is convenient:

```bash
curl -O https://raw.githubusercontent.com/angristan/openvpn-install/master/openvpn-install.sh
chmod +x openvpn-install.sh
sudo ./openvpn-install.sh   # answer the prompts, enter a client name → produces <client>.ovpn
```

The generated `.ovpn` file can be imported into NX-VPN, where it is stored and shown in the UI.

> **How OpenVPN tunnels:** NX-VPN delegates the actual OpenVPN tunnel to the open-source
> **OpenVPN for Android** engine over its external API. When you connect to an OpenVPN profile, if
> that app isn't installed NX-VPN sends you to its F-Droid page for a one-time install. See the
> README "Protocol status" section.

---

## Quick test tip
You can first test the UI with a free/public WireGuard config; then set up your own server to get a
real "US exit".
