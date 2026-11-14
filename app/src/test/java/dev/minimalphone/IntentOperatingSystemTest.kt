package dev.minimalphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.minimalphone.blocker.GrayscaleController
import dev.minimalphone.session.AppTier
import dev.minimalphone.session.AppTierClassifier
import dev.minimalphone.session.MicroSessionManager
import dev.minimalphone.session.MinimalRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Tests the 4 Intent-Based Operating System features:
 * 1. Tiered App Tiers (Tier 0 Essentials, Tier 1 Utility with Limits, Tier 2 Dopamine Traps)
 * 2. Micro-Session Timers (5-minute session countdown, 60-second cooldown)
 * 3. 5-Second Intention Speed Bump routing
 * 4. Native Grayscale Daltonizer settings and permissions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class IntentOperatingSystemTest {

    private lateinit var context: Context
    private lateinit var repository: MinimalRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MinimalRepository.resetForTests()
        MicroSessionManager.clearAll()
        context.getSharedPreferences("minimal_bro_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repository = MinimalRepository.get(context)
    }

    @After
    fun tearDown() {
        repository.endSession(completed = false)
        MinimalRepository.resetForTests()
        MicroSessionManager.clearAll()
    }

    @Test
    fun appTierClassifierAccuratelySortsExplicitBuckets() {
        // Tier 0 (Essentials): UPI, Phone, Camera, Maps, Uber
        val upiGPay = AppTierClassifier.suggestTier("com.google.android.apps.nbu.paisa.user", "Google Pay")
        val upiPhonePe = AppTierClassifier.suggestTier("com.phonepe.app", "PhonePe")
        val phone = AppTierClassifier.suggestTier("com.google.android.dialer", "Phone")
        val camera = AppTierClassifier.suggestTier("com.google.android.GoogleCamera", "Camera")
        val maps = AppTierClassifier.suggestTier("com.google.android.apps.maps", "Google Maps")
        val uber = AppTierClassifier.suggestTier("com.ubercab", "Uber")

        assertEquals(AppTier.TIER_0, upiGPay)
        assertEquals(AppTier.TIER_0, upiPhonePe)
        assertEquals(AppTier.TIER_0, phone)
        assertEquals(AppTier.TIER_0, camera)
        assertEquals(AppTier.TIER_0, maps)
        assertEquals(AppTier.TIER_0, uber)

        // Tier 1 (Utility with Limits): Amazon, Myntra, LinkedIn, Swiggy, Chrome
        val amazon = AppTierClassifier.suggestTier("in.amazon.mShop.android.shopping", "Amazon")
        val myntra = AppTierClassifier.suggestTier("com.myntra.android", "Myntra")
        val linkedin = AppTierClassifier.suggestTier("com.linkedin.android", "LinkedIn")
        val swiggy = AppTierClassifier.suggestTier("in.swiggy.android", "Swiggy")
        val chrome = AppTierClassifier.suggestTier("com.android.chrome", "Chrome")

        assertEquals(AppTier.TIER_1, amazon)
        assertEquals(AppTier.TIER_1, myntra)
        assertEquals(AppTier.TIER_1, linkedin)
        assertEquals(AppTier.TIER_1, swiggy)
        assertEquals(AppTier.TIER_1, chrome)

        // Tier 2 (Dopamine Traps): Instagram, Reddit, YouTube
        val instagram = AppTierClassifier.suggestTier("com.instagram.android", "Instagram")
        val reddit = AppTierClassifier.suggestTier("com.reddit.frontpage", "Reddit")
        val youtube = AppTierClassifier.suggestTier("com.google.android.youtube", "YouTube")

        assertEquals(AppTier.TIER_2, instagram)
        assertEquals(AppTier.TIER_2, reddit)
        assertEquals(AppTier.TIER_2, youtube)
    }

    @Test
    fun microSessionTracksFiveMinutesAndSixtySecondCooldown() {
        val testPkg = "in.amazon.mShop.android.shopping"
        val t0 = 100_000L

        // 1. Session starts with 5-minute (300,000ms) countdown
        val session = MicroSessionManager.startOrGetSession(testPkg, now = t0)
        assertNotNull(session)
        assertEquals(t0, session!!.startedAtMillis)
        assertEquals(t0 + 5 * 60 * 1000L, session.expiresAtMillis)
        assertFalse(MicroSessionManager.isInCooldown(testPkg, now = t0))

        // Remaining session at t0 + 2 minutes
        val remainingMs = MicroSessionManager.getRemainingSessionMs(testPkg, now = t0 + 120_000L)
        assertEquals(180_000L, remainingMs)

        // 2. Session expires at 5 minutes and triggers 60-second cooldown
        val expiryTime = t0 + 300_000L
        val cooldownEnd = MicroSessionManager.expireSession(testPkg, now = expiryTime)
        assertEquals(expiryTime + 60_000L, cooldownEnd)

        // 3. During cooldown, isInCooldown is true and returns remaining seconds
        assertTrue(MicroSessionManager.isInCooldown(testPkg, now = expiryTime + 15_000L))
        assertEquals(45, MicroSessionManager.getRemainingCooldownSeconds(testPkg, now = expiryTime + 15_000L))

        // Attempting to start a session during cooldown is blocked (returns null)
        val blockedAttempt = MicroSessionManager.startOrGetSession(testPkg, now = expiryTime + 15_000L)
        assertNull(blockedAttempt)

        // 4. After 60 seconds have elapsed, cooldown ends and user can reopen
        val afterCooldown = cooldownEnd + 1_000L
        assertFalse(MicroSessionManager.isInCooldown(testPkg, now = afterCooldown))
        assertEquals(0, MicroSessionManager.getRemainingCooldownSeconds(testPkg, now = afterCooldown))

        val newSession = MicroSessionManager.startOrGetSession(testPkg, now = afterCooldown)
        assertNotNull(newSession)
        assertEquals(afterCooldown, newSession!!.startedAtMillis)
    }

    @Test
    fun repositoryPersistsAppTiersAndGrayscaleSettings() {
        repository.setAppTier("com.ubercab", AppTier.TIER_0)
        repository.setAppTier("in.swiggy.android", AppTier.TIER_1)
        repository.setAppTier("com.instagram.android", AppTier.TIER_2)

        val settings = repository.settings.value
        assertEquals(AppTier.TIER_0, settings.appTiers["com.ubercab"])
        assertEquals(AppTier.TIER_1, settings.appTiers["in.swiggy.android"])
        assertEquals(AppTier.TIER_2, settings.appTiers["com.instagram.android"])
        assertTrue(settings.allowedPackages.contains("com.ubercab"))
        assertTrue(settings.allowedPackages.contains("in.swiggy.android"))
        assertTrue(settings.allowedPackages.contains("com.instagram.android"))

        // Active session reflects tiers and grayscale
        val active = repository.start()
        assertEquals(AppTier.TIER_2, active.appTiers["com.instagram.android"])
        assertTrue(active.enableGrayscale)

        // Removing tier removes from allowed
        repository.setAppTier("com.instagram.android", null)
        assertFalse(repository.settings.value.allowedPackages.contains("com.instagram.android"))
        assertNull(repository.settings.value.appTiers["com.instagram.android"])
    }

    @Test
    fun grayscaleControllerConstantsAndPermissionsMatchAndroidSpecifications() {
        assertEquals("accessibility_display_daltonizer_enabled", GrayscaleController.SETTING_DALTONIZER_ENABLED)
        assertEquals("accessibility_display_daltonizer", GrayscaleController.SETTING_DALTONIZER)
        assertEquals(0, GrayscaleController.MONOCHROMACY_VALUE)
        assertTrue(GrayscaleController.ADB_PERMISSION_COMMAND.contains("WRITE_SECURE_SETTINGS"))
        assertTrue(GrayscaleController.ADB_PERMISSION_COMMAND.contains("com.focusbro.minimal"))
    }
}
