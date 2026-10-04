# Build release APK / AAB

## Debug APK (sideload, fastest)
```powershell
cd D:\OpenCode\reel-pilot
.\gradlew.bat :app:assembleDebug
# output: app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Release APK (for GitHub / direct share)
1. Create keystore once:
```powershell
keytool -genkeypair -v -keystore reel-pilot.jks -alias reel -keyalg RSA -keysize 2048 -validity 10000
```
2. Add to `~/.gradle/gradle.properties` (NEVER commit):
```
REEL_STORE_FILE=D:\\keys\\reel-pilot.jks
REEL_STORE_PASSWORD=***
REEL_KEY_ALIAS=reel
REEL_KEY_PASSWORD=***
```
3. Signing config is intentionally NOT in this repo. For a quick unsigned test build:
```powershell
.\gradlew.bat :app:assembleRelease
```
Sign with apksigner before distributing.

## Release AAB (Play Store)
```powershell
.\gradlew.bat :app:bundleRelease
# output: app\build\outputs\bundle\release\app-release.aab
```

## Pre-release checklist
- [ ] versionCode/versionName bumped in `app/build.gradle.kts`
- [ ] Tested on Samsung + Xiaomi with 10s interval
- [ ] Accessibility disclosure video recorded (see PLAY_DISCLOSURE.md)
- [ ] Privacy policy URL live (see PRIVACY.md)
- [ ] Foreground-service + accessibility declarations filled in Play Console
