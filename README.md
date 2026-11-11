# MinimalBro (`com.focusbro.minimal`)

**Calm Minimalist Phone launcher & multi-day/ongoing digital detox home screen for Android — with zero limit on allowed apps.**

## Downloads (Ready to Install)

| Artifact | Purpose | Version | Size |
| --- | --- | --- | --- |
| [**Download `MinimalBro-1.0.0-release.apk`**](release/MinimalBro-1.0.0-release.apk) | Direct sideload on Android 8.0+ | `v1.0.0` (`versionCode 1`) | 5.4 MB (`5,611,322` B) |
| [**Play Store Bundle `MinimalBro-1.0.0-playstore.aab`**](release/MinimalBro-1.0.0-playstore.aab) | Signed Google Play App Bundle | `v1.0.0` (`versionCode 1`) | 5.4 MB (`5,614,042` B) |
| [**GitHub Releases (`v1.0.0`)**](https://github.com/sahuom24/MinimalBro/releases) | Official release assets & notes | `v1.0.0` | — |
| [**Checksums (`SHA256SUMS.txt`)**](release/SHA256SUMS.txt) | Verify APK & AAB integrity | `v1.0.0` | — |

> **Looking for strict timed focus timers, 5-app buckets, and Pomodoro cycles?**  
> Check out [**FocusBro (`sahuom24/focus-Bro`)**](https://github.com/sahuom24/focus-Bro).

---

## Automated Google Play Publishing

MinimalBro includes GitHub Actions CI & automated Google Play Console deployment ([`.github/workflows/release.yml`](.github/workflows/release.yml)).  
Follow the 5-minute one-time setup in [**`PLAY_AUTOMATION.md`**](PLAY_AUTOMATION.md) to push tags (`git tag v1.0.1 && git push origin v1.0.1`) and automatically publish `.aab` updates to Play Console.

---

## Features

- **Unlimited Essential Apps:** Curate as many essential apps as you need (`No limit`), pin favorites to the minimalist Home canvas, and find everything else via instant type-to-filter search.
- **Ongoing & Multi-Day Detox Commitments:** Stay minimal for `∞ Ongoing`, `24h`, `3 Days`, `7 Days`, `14 Days`, or `30 Days`, with friction-controlled hold-to-exit (plus optional screen-lock verification).
- **Selectable Default Home Launcher:** Registers as a standard Android `HOME` activity so every Home press keeps you inside your calm workspace.
- **Material You Bento Themes:** Includes `Minimal Paper` (Warm Paper), `Quiet Sage`, `Obsidian Calm`, `Dusk Clay`, and `Dynamic You`.
- **100% Offline & Private:** Zero `INTERNET` permission, zero analytics, `allowBackup="false"`, and local-only preferences.

---

## Build Locally & Verify

Requirements: JDK 17, Android SDK platform `36`, `minSdk = 26`, `targetSdk = 36`.

```bash
# Verify prebuilt release checksums
cd release && sha256sum -c SHA256SUMS.txt

# Run pure JVM logic unit tests & release lint
./gradlew testDebugUnitTest --tests "dev.minimalphone.MinimalLogicTest" lintVitalRelease

# Build debug / release artifacts
./gradlew assembleDebug
./gradlew assembleRelease bundleRelease
```

*(Full Robolectric UI & repository suite runs automatically in CI via [`.github/workflows/ci.yml`](.github/workflows/ci.yml).)*
