package dev.minimalphone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.session.ActiveMinimalSession
import dev.minimalphone.session.MinimalSettings
import dev.minimalphone.session.Wallpaper
import dev.minimalphone.ui.AppsCurateTab
import dev.minimalphone.ui.MinimalHomeScreen
import dev.minimalphone.ui.MinimalSetupScreen
import dev.minimalphone.ui.theme.MinimalBroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class MinimalScreensTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun minimalHomeShowsUnlimitedAppsFavoritesAndHoldToExit() {
        val now = System.currentTimeMillis()
        val apps = (1..8).map { LaunchableApp("com.example.app$it", "Minimal Tool $it") }
        compose.setContent {
            MinimalBroTheme(wallpaper = Wallpaper.PAPER) {
                MinimalHomeScreen(
                    active = ActiveMinimalSession(
                        startedAtMillis = now - 60_000L,
                        endsAtMillis = now + 7L * 86_400_000L,
                        isIndefinite = true,
                        allowedPackages = apps.map { it.packageName }.toSet(),
                        requireUnlockToEnd = false,
                        interruptionsPaused = false,
                    ),
                    wallpaper = Wallpaper.PAPER,
                    allowedApps = apps,
                    favoriteApps = apps.take(2),
                    showAppIcons = false,
                    onExpired = {},
                    onRequestExit = {},
                    onExtendHours = {},
                )
            }
        }

        compose.onNodeWithText("Minimal Tool 1").assertIsDisplayed()
        compose.onNodeWithText("Minimal Tool 8").assertIsDisplayed()
        compose.onNodeWithText("Hold to exit Minimal").assertIsDisplayed()
    }

    @Test
    fun appsCurateTabDisplaysUnlimitedCount() {
        compose.setContent {
            MinimalBroTheme {
                AppsCurateTab(
                    apps = (1..7).map { LaunchableApp("com.example.app$it", "App $it") },
                    appsLoaded = true,
                    allowedPackages = (1..7).map { "com.example.app$it" }.toSet(),
                    favoritePackages = setOf("com.example.app1"),
                    onToggleAllowed = {},
                    onToggleFavorite = {},
                    onAllowEssentials = {},
                    onAllowAll = {},
                    onClearAllowed = {},
                )
            }
        }

        compose.onNodeWithText("7 allowed · No limit · 1 pinned on Home").assertIsDisplayed()
    }

    @Test
    fun setupScreenRendersOngoingDefaultCta() {
        compose.setContent {
            MinimalBroTheme {
                MinimalSetupScreen(
                    settings = MinimalSettings(),
                    history = emptyList(),
                    apps = emptyList(),
                    appsLoaded = true,
                    isDefaultHome = false,
                    accessibilityAccess = false,
                    dndAccess = false,
                    notificationsEnabled = true,
                    justFinishedMinutes = null,
                    onDismissFinished = {},
                    onStartMinimal = {},
                    onCommitmentHours = {},
                    onToggleAllowed = {},
                    onToggleFavorite = {},
                    onAllowEssentials = {},
                    onAllowAll = {},
                    onClearAllowed = {},
                    onShowAppIcons = {},
                    onPauseInterruptions = {},
                    onRequireUnlock = {},
                    onWallpaper = {},
                    onOpenHomeSettings = {},
                    onRequestAccessibilityAccess = {},
                    onRequestDndAccess = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        compose.onNodeWithText("MinimalBro").assertIsDisplayed()
        compose.onNodeWithText("Enter Ongoing Minimal").assertIsDisplayed()
    }
}
