package dev.minimalphone.session

/** Domain bounds and defaults for MinimalBro. */
object MinimalConstants {
    const val INDEFINITE_HOURS = 0
    const val MIN_TIMED_HOURS = 1
    const val MAX_TIMED_HOURS = 90 * 24 // 90 days
}

/** Material You and calm minimalist themes, stored purely locally. */
enum class Wallpaper(val title: String) {
    PAPER("Warm Paper"),
    AMOLED("Pure Ink"),
    DYNAMIC("Material You"),
    FOREST("Calm Sage"),
    DUSK("Twilight"),
    NIGHT("Espresso"),
}

/** Intent-based tiered app classification to break the unconscious doom-scrolling loop. */
enum class AppTier(val level: Int, val title: String, val subtitle: String) {
    TIER_0(0, "Tier 0 (Essentials)", "UPI, Phone, Camera, Maps, Uber · Unlimited access"),
    TIER_1(1, "Tier 1 (Utility with Limits)", "Amazon, Myntra, Swiggy, Chrome · 5m session & 60s cooldown"),
    TIER_2(2, "Tier 2 (Dopamine Traps)", "Instagram, Reddit, YouTube · Speed bump & monochrome"),
}

/** User preferences for MinimalBro. Zero app count caps: keep as many essentials as needed. */
data class MinimalSettings(
    /** 0 = Ongoing (permanent minimal phone until exited), otherwise hours up to 90 days. */
    val commitmentHours: Int = 0,
    val allowedPackages: Set<String> = emptySet(),
    val favoritePackages: Set<String> = emptySet(),
    val appTiers: Map<String, AppTier> = emptyMap(),
    val showAppIcons: Boolean = false,
    val pauseInterruptions: Boolean = false,
    val requireUnlockToEnd: Boolean = false,
    val wallpaper: Wallpaper = Wallpaper.PAPER,
    val enableGrayscale: Boolean = true,
)

/** An active minimal phone detox session, derived from wall-clock timestamps. */
data class ActiveMinimalSession(
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val isIndefinite: Boolean,
    val allowedPackages: Set<String>,
    val appTiers: Map<String, AppTier> = emptyMap(),
    val enableGrayscale: Boolean = true,
    val requireUnlockToEnd: Boolean,
    val interruptionsPaused: Boolean,
) {
    val totalMillis: Long get() = (endsAtMillis - startedAtMillis).coerceAtLeast(1L)

    fun remainingMillis(now: Long = System.currentTimeMillis()): Long =
        if (isIndefinite) Long.MAX_VALUE else (endsAtMillis - now).coerceAtLeast(0L)

    fun elapsedMillis(now: Long = System.currentTimeMillis()): Long =
        (now - startedAtMillis).coerceAtLeast(0L)

    fun isOver(now: Long = System.currentTimeMillis()): Boolean =
        !isIndefinite && now >= endsAtMillis

    fun fractionElapsed(now: Long = System.currentTimeMillis()): Float =
        if (isIndefinite) 0f else ((now - startedAtMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    fun fractionRemaining(now: Long = System.currentTimeMillis()): Float =
        if (isIndefinite) 1f else (1f - fractionElapsed(now)).coerceIn(0f, 1f)
}

/** One completed or ended minimal session kept for on-device reflection. */
data class MinimalRecord(
    val endedAtMillis: Long,
    val minutes: Int,
    val completed: Boolean,
)
