package dev.minimalphone

import dev.minimalphone.session.ActiveMinimalSession
import dev.minimalphone.session.MinimalConstants
import dev.minimalphone.session.MinimalSettings
import dev.minimalphone.ui.formatCommitmentHours
import dev.minimalphone.ui.formatCountdown
import dev.minimalphone.ui.formatElapsedShort
import dev.minimalphone.ui.formatLoggedMinutes
import dev.minimalphone.ui.minimalStartButtonLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for MinimalBro domain bounds, continuous ring depletion, ongoing/multi-day
 * formatting, and unlimited app configuration.
 */
class MinimalLogicTest {

    @Test
    fun timedMinimalSessionRingDepletesContinuouslyAndIndefiniteStaysFull() {
        val start = 2_000_000L
        val end = start + 80_000L
        val timed = ActiveMinimalSession(
            startedAtMillis = start,
            endsAtMillis = end,
            isIndefinite = false,
            allowedPackages = emptySet(),
            requireUnlockToEnd = false,
            interruptionsPaused = false,
        )

        assertEquals(1.0f, timed.fractionRemaining(start), 0.0001f)
        assertEquals(0.50f, timed.fractionRemaining(start + 40_000L), 0.0001f)
        assertEquals(0.0f, timed.fractionRemaining(end), 0.0001f)
        assertFalse(timed.isOver(start + 40_000L))
        assertTrue(timed.isOver(end))

        val ongoing = timed.copy(isIndefinite = true)
        assertEquals(1.0f, ongoing.fractionRemaining(end + 999_999L), 0.0001f)
        assertFalse(ongoing.isOver(end + 999_999L))
    }

    @Test
    fun commitmentsAndElapsedLabelsFormatAcrossHoursAndMultiDayDetox() {
        assertEquals("Ongoing", formatCommitmentHours(MinimalConstants.INDEFINITE_HOURS))
        assertEquals("12h", formatCommitmentHours(12))
        assertEquals("1 day", formatCommitmentHours(24))
        assertEquals("7 days", formatCommitmentHours(7 * 24))
        assertEquals("90 days", formatCommitmentHours(MinimalConstants.MAX_TIMED_HOURS))
        assertEquals("7d 00:00:00", formatCountdown(7L * 86_400_000L))
        assertEquals("2d 4h", formatElapsedShort((2L * 24 + 4) * 60 * 60_000L))
        assertEquals("3 h 15 min", formatLoggedMinutes(195))
    }

    @Test
    fun unlimitedAllowedAppsAndCtaLabelsWorkWithoutFiveAppCap() {
        val twentyApps = (1..20).map { "com.example.tool$it" }.toSet()
        val ongoingSettings = MinimalSettings(
            commitmentHours = 0,
            allowedPackages = twentyApps,
            favoritePackages = setOf("com.example.tool1", "com.example.tool2"),
        )
        assertEquals(20, ongoingSettings.allowedPackages.size)
        assertEquals("Enter Ongoing Minimal", minimalStartButtonLabel(ongoingSettings))

        val timedSettings = ongoingSettings.copy(commitmentHours = 7 * 24)
        assertEquals("Enter 7 days Minimal", minimalStartButtonLabel(timedSettings))
    }
}
