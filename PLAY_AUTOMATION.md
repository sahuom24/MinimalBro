# MinimalBro — Google Play Store Automation Setup

MinimalBro (`com.focusbro.minimal`) is configured with [`.github/workflows/release.yml`](.github/workflows/release.yml) so that pushing a version tag (`v1.0.0`, `v1.0.1`, …) or manually running **Build, Release & Play Console Deploy** builds, tests, publishes a GitHub Release, and uploads the signed `.aab` to Google Play Console automatically via the official Google Play Developer API — without ever sharing your Google account password.

## App Signing Details

- **Package ID:** `com.focusbro.minimal`
- **Upload Key Alias:** `minimalbro-upload`
- **Upload Certificate SHA-256:** `3B:4F:4A:50:0D:12:AC:19:42:E9:02:0D:C5:F1:5C:E2:E2:CE:65:94:C3:90:EF:B7:1B:94:8D:EB:32:FC:DD:05`

---

## Important First-Time Play Console Rule (New App)

Because `com.focusbro.minimal` is a brand-new app ID, Google Play requires **one initial manual `.aab` upload** inside Play Console when creating the app (`release/MinimalBro-1.0.0-playstore.aab`). Once that first draft/internal release exists in Play Console, every subsequent upload works 100% automatically via GitHub Actions.

---

## One-Time Setup (Takes ~5 Minutes)

### Step 1 — Reuse or Create Your Play Service Account

If you already created a Google Play Service Account for **FocusBro** (`focus-Bro`), you can reuse the exact same `.json` key here — just make sure the service account has **App permissions -> MinimalBro -> Release to testing tracks & production** in **Play Console -> Users and permissions**.

Otherwise:
1. Open **Google Play Console -> Setup -> API access**.
2. Link a Google Cloud Project -> create a Service Account -> download its **JSON key**.
3. Grant **Release to testing tracks** and **Release apps to production** in **Play Console -> Users and permissions**.

### Step 2 — Add GitHub Repository Secrets

Open **GitHub -> `sahuom24/MinimalBro` -> Settings -> Secrets and variables -> Actions -> New repository secret** and add:

| Secret Name | Value |
| --- | --- |
| `PLAY_SERVICE_ACCOUNT_JSON` | Full contents of the downloaded `.json` service account key |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore (`keystore/minimalbro-release.jks`) |
| `KEYSTORE_PASSWORD` | Your keystore password (from local `keystore.properties`) |
| `KEY_ALIAS` | `minimalbro-upload` |
| `KEY_PASSWORD` | Your key password (from local `keystore.properties`) |

Generate `ANDROID_KEYSTORE_BASE64` locally in PowerShell (copies straight to clipboard without printing to terminal history):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore/minimalbro-release.jks")) | Set-Clipboard
```

---

## How to Release Future Updates

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` (e.g. `versionCode = 2`, `versionName = "1.0.1"`).
2. Commit and push to `main`:
   ```bash
   git add app/build.gradle.kts
   git commit -m "chore: bump version to 1.0.1"
   git push origin main
   ```
3. Push a version tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. GitHub Actions automatically runs tests & lint, builds the signed `.apk` & `.aab`, publishes GitHub Releases, and uploads the `.aab` to Google Play (`internal` track by default; or trigger manually for `alpha` / `beta` / `production`).
