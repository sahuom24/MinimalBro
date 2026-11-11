# Privacy Policy for MinimalBro

**Effective Date:** November 11, 2026  
**Developer:** Om Sahu (`imsahuom@gmail.com`)  
**App Package:** `com.focusbro.minimal`

MinimalBro is a 100% offline minimalist phone launcher and digital detox companion.

## 1. Data Collection & Transmission
- **No Personal Data Collected:** MinimalBro does not collect, upload, sell, or share personal information, usage telemetry, crash logs, analytics, or advertising IDs.
- **No Internet Access:** MinimalBro does not request `INTERNET` permission and cannot transmit any data off your device.
- **On-Device Storage Only:** Allowed app selections, pinned favorites, minimal duration commitments, theme preferences, and detox history remain exclusively in private on-device `SharedPreferences` and are deleted when you uninstall or clear app data.

## 2. Permissions & Special Access Used

### Default Home / Launcher (`CATEGORY_HOME`)
- **Purpose:** Allows you to set MinimalBro as your device's distraction-free Home screen so your essentials appear in a calm text/Material You interface.

### Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)
- **Purpose:** Optionally detects window state changes (`TYPE_WINDOW_STATE_CHANGED`) during an active Minimal detox session to redirect you away from hidden/unselected apps while keeping Phone, SMS, notifications, keyboards, biometrics, and essential settings accessible.
- **Privacy Guarantee:** `canRetrieveWindowContent="false"`. MinimalBro never reads screen text, keystrokes, passwords, messages, or personal content.

### Do Not Disturb Access (`ACCESS_NOTIFICATION_POLICY`)
- **Purpose:** Optionally enables Do Not Disturb during an active detox session if it was previously off, and restores it when finished.

### App Visibility (`QUERY` for Launcher Intents)
- MinimalBro queries only launchable apps via narrow `MAIN`/`LAUNCHER` `<queries>` so you can choose unlimited allowed essentials, pin favorites, and search/launch them.

## 3. Contact
For questions or support regarding this Privacy Policy, contact **imsahuom@gmail.com**.
