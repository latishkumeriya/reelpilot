## ReelPilot v1.1.0 — hands-free Reels/Shorts/TikTok auto-scroll

Adjustable timer: presets 35s / 40s / 60s + Custom 5–300s. No root, no login, 100% on-device.

### Features
- Auto-swipe via Accessibility + countdown overlay bubble (countdown + Pause/+10s/Stop)
- Target apps: Instagram Reels (default), YouTube Shorts, TikTok (opt-in in Settings)
- Auto-pause when leaving target app or screen off; manual swipe resets timer
- Auto-stop after N reels (0 = unlimited, up to 200); service stops itself at the limit
- Lifetime stats (total reels + sessions, DataStore-persisted) with reset
- Vibrate (40ms) and/or beep (ToneGenerator) per scroll
- Bubble size 80–130% + opacity 40–100%, live-applied
- Battery-optimization + Xiaomi/Oppo/Vivo/Samsung/Huawei autostart wizards
- Restart-on-boot resume (or tap-to-resume notification on Android 12+)
- Foreground notification with Pause/Resume/+10s/Stop

### Install
1. Download `ReelPilot-v1.1.0.apk` from Assets below (or build per `docs/BUILD_APK.md`).
2. Sideload: `adb install -r ReelPilot-v1.1.0.apk`
3. Open app → grant Accessibility + Display-over-other-apps + Notifications.
4. Pick 35s (or Custom 10s to test) → Start → open Instagram Reels → bubble counts down → auto-swipe.

### Permissions — why each is needed
- Accessibility: detect selected short-video app in foreground + perform swipe-up after countdown. Never reads passwords/messages for profiling.
- Display over other apps: countdown bubble on top of Instagram/YouTube/TikTok.
- Notifications: keep timer alive as foreground service with Pause/Resume/Stop.
- Ignore battery optimizations: prevents Xiaomi/Oppo/Vivo from killing the timer.
- No data leaves the phone — see `docs/PRIVACY.md`.

### Known limits
- Play Store may reject Accessibility API use for non-disability purposes — sideload via GitHub recommended (see `docs/PLAY_DISCLOSURE.md` for declaration + video script).
- TikTok/Shorts detection is package-level (best-effort across OEM skins).
- Android 12+ blocks background FGS starts after reboot — you get a tap-to-resume notification instead.
- minSdk 26 (Android 8.0), targetSdk 34. Tested flow: Samsung + Xiaomi, 10s interval.

### Verify
```
SHA256: <paste sha256 of APK here>
versionCode: 2 / versionName: 1.1.0 / applicationId: com.reelpilot.app
```

### Source
Built from `D:\OpenCode\reel-pilot` Phase 5. Full test steps in `README.md`.
