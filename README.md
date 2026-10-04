# ReelPilot — Phase 5 (Release polish: boot + sound + overlay tuning)

Kotlin + Jetpack Compose + Hilt + DataStore.

## Open in Android Studio
1. Open folder `reel-pilot/` in Android Studio Hedgehog+.
2. Let Gradle sync (needs internet first time).
3. Run on real device (emulator has no Instagram): `Run > app`.
4. Grant Accessibility + Overlay when Onboarding appears.

## What works in Phase 2
- Home screen: 35 / 40 / 60 / Custom (5-300s) selector, toggles, Start/Stop UI
- Onboarding, Settings, Help screens + bottom nav
- `PrefsRepository` persists interval
- `ScrollTimerManager` countdown loop + manual-swipe reset
- `ReelScrollService` swipe gesture skeleton
- `ScrollForegroundService` persistent notification
- `FloatingBubbleManager` draggable countdown bubble

## Phase 3 DONE
- `utils/PermissionUtils.kt` — live accessibility/overlay/battery checks
- `utils/BatteryUtils.kt` — request ignore-optimizations with fallback
- `utils/AutostartHelper.kt` — Xiaomi/Oppo/Vivo/Samsung/Huawei deep-links
- `manager/SessionCoordinator.kt` — runs timer only when Instagram FG (if autoStart) + screen ON; system-pause auto-resumes
- `manager/ScrollTimerManager.kt` — fixed resume-reset bug, pauseBySystem/resumeInternal, reels counter, max-reels stop
- `services/ReelScrollService.kt` — foreground tracking, gated swipe, 2.5s debounce, vibrate (40ms) on scroll
- `services/ScrollForegroundService.kt` — overlay show/hide tied to lifecycle, screen OFF/ON receiver, Pause/Resume/Stop notif actions, live title
- `overlay/FloatingBubbleManager.kt` — drag vs tap disambiguation, shows ⏳ system-paused / ⏸ user-paused
- `ui/onboarding` — live green checks, battery + autostart cards, blocked Done until core OK, auto-skip on re-launch
- `ui/home` — warning banners, pause reason, Instagram detected dot, Start disabled until accessibility ON
- `AndroidManifest.xml` — VIBRATE + queries instagram
- `MainActivity.kt` — auto-skip onboarding, guarded start, clean STOP via action

## Phase 3 TEST (real device)
1. Set Custom 10s, Start, open Instagram Reels → bubble counts 10..0, swipe happens.
2. Press Home (leave Instagram) → notif shows "Waiting for Instagram…", no swipe.
3. Return → resumes with remaining time preserved.
4. Screen off → paused; screen on → resumes.
5. Manual swipe → timer resets to full.
6. Bubble tap → pause/resume. Notif Pause/Resume/Stop all work.
7. Xiaomi device → Settings shows Autostart card, battery Fix opens ignore-optimizations.

## Phase 4 DONE (overlay reworked in CI fix: classic Views, not Compose — ComposeView in a Service overlay needs ViewTreeLifecycleOwner artifacts)
- `overlay/FloatingBubbleManager.kt` — countdown + status + Pause/+10s/Stop buttons, drag-to-move, live collect of timer/prefs/coordinator
- `services/ScrollForegroundService.kt` — new +10s notif action, auto-shutdown on max-reels via onMaxReached
- `manager/ScrollTimerManager.kt` — addSeconds(10) snooze, recordScroll/recordSession, onMaxReached callback
- `data/PrefsRepository.kt` — enableInstagram/Youtube/Tiktok, totalReels/totalSessions, recordSession/recordScroll/resetStats
- `manager/AppDetector.kt` — Reels/Shorts/TikTok package set + labels
- `manager/SessionCoordinator.kt` — setForegroundPackage(), enabled-set gating, "Waiting — in YouTube" reason
- `services/ReelScrollService.kt` — multi-app foreground + gated swipe
- `ui/settings` — target apps switches, auto-stop slider 0-200 + chips, lifetime stats + reset
- `ui/home` — session x/y + lifetime display, per-app foreground label
- Deps: lifecycle-service added; manifest queries youtube + tiktok

## Phase 4 TEST
1. Enable Shorts in Settings, open YouTube Shorts with 10s timer → auto-scrolls.
2. Bubble shows ring depleting; +10s postpones; ⏹ stops service + hides bubble.
3. Set max 5 → after 5 scrolls service stops itself.
4. Stats survive restart (DataStore). Reset works.
5. Disable all apps → Settings warns, timer waits.

## Phase 5 DONE
- `receiver/BootReceiver` wired (RECEIVE_BOOT_COMPLETED): auto-resumes if restart-on-boot ON + session was active; falls back to tap-to-resume notification on Android 12+ denial
- `utils/SoundHelper` ToneGenerator beep + Settings toggle
- `overlay` live size (80–130%) + opacity (40–100%) sliders, applied instantly via scaleX/Y + alpha collectors
- Adaptive launcher icon (no more system icon), versionCode 2 / 1.1.0
- `docs/BUILD_APK.md` + `docs/PLAY_DISCLOSURE.md` + `docs/PRIVACY.md`
- HelpScreen expanded FAQ; sessionWasActive flag set/cleared by foreground service

## Phase 5 TEST
1. Start session → reboot (or force-stop + BOOT broadcast) → service resumes or resume notification appears.
2. Settings beep ON → each auto-scroll beeps; OFF → silent.
3. Bubble sliders move/resize live during active session.
4. `./gradlew :app:assembleDebug` APK installs; release checklist in docs.

## Done — v1.1.0 feature-complete
