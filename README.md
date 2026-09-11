# Dual Audio Sync

A tiny iOS-friendly web player for one local movie file on an iPad and two Bluetooth headphone pairs using a second phone/tablet as the remote audio receiver.

## How it works

- The movie file stays only on the HOST iPad.
- The iPad opens the local file in the browser player.
- Web Audio sends the same audio both to the iPad output and to a WebRTC MediaStream.
- The LISTEN device receives only the live audio stream and plays it through its own Bluetooth headphones.
- PeerJS Cloud is used only for signaling; the media path is WebRTC peer-to-peer when the network allows it.

## Fast setup

1. Connect Sony headphones to the iPad.
2. Connect Bose headphones to the second device. Disconnect the Bose from the iPad if they are paired there.
3. In Telegram on the iPad, save the downloaded movie to Files.
4. Open the GitHub Pages site in Safari on the iPad.
5. Stay in HOST mode, choose the local movie file, then press `START HOST`.
6. Note the six-digit room code or share the generated join link.
7. Open the same site on the second device, select `LISTEN`, enter the six-digit code and press `JOIN AUDIO`.
8. When `ВКЛЮЧИТЬ ЗВУК В BOSE` appears, tap it. iOS requires a user gesture before remote audio playback.
9. Start the movie on the iPad.
10. Adjust `Задержка Sony` on the HOST until the local Sony audio matches the remote Bose audio. Start around 220 ms and use the five-click sync test.
11. Keep the HOST page in the foreground and keep the iPad screen awake during playback.

## Important limitations

- Internet is required to establish the PeerJS signaling connection. The audio itself is WebRTC P2P when possible.
- If the connection drops after changing Wi-Fi/airplane-mode/network interfaces, reconnect both devices.
- iOS/Safari does not allow this page to capture arbitrary system audio from Telegram. The movie must be selected and played inside this page.
- Browser codec support still applies. MP4/H.264/AAC is the safest combination for Safari.
- Different Bluetooth headphones add different hardware latency; use the HOST delay slider to compensate.

## Deployment

The repository contains a GitHub Actions Pages workflow in `.github/workflows/pages.yml`. In repository Settings → Pages, the publishing source should be set to **GitHub Actions** if GitHub has not enabled it automatically.
