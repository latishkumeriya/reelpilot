# Play Store — Accessibility & Foreground-Service declarations

## Short description (Play listing)
ReelPilot auto-advances short videos hands-free with an adjustable timer (35/40/60s or custom 5–300s).

## Accessibility declaration (copy into Play Console)
> ReelPilot uses the AccessibilityService API solely to (1) detect when a user-selected
> short-video app (Instagram Reels, YouTube Shorts, TikTok) is in the foreground and
> (2) perform a swipe-up gesture after the user-chosen countdown to advance to the next
> video. The service never reads passwords, messages, or screen text for profiling;
> it only observes foreground package + scroll events to reset the timer on manual
> swipes. No data is collected, transmitted, or shared. A persistent in-app disclosure
> + onboarding screen explains this before the user enables the service.

Video script (30s, required by Google):
1. Open ReelPilot → show disclosure text.
2. Tap Enable → system Accessibility screen → enable ReelPilot.
3. Pick 35s → Start → open Instagram → bubble counts down → auto-swipe.
4. Show Stop.

## Foreground-service declaration
Type: Special Use (`specialUse`). Subtype: "Hands-free auto-scroll countdown".
Justification: a visible countdown + bubble must survive while the user watches
videos in another app; notification shows seconds left + Pause/Resume/Stop.

## Common rejection reasons
- Missing in-app disclosure video → record the script above.
- Requesting accessibility without need → keep ToS: no auto-like/comment, swipe only.
- If rejected, distribute via GitHub APK (no declaration needed for sideload).
