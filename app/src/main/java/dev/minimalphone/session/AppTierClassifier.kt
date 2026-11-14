package dev.minimalphone.session

/**
 * Intelligent categorization of installed applications into three explicit tiers:
 * - Tier 0 (Essentials): UPI, Phone, Camera, Maps, Uber. Unlimited access, zero friction.
 * - Tier 1 (Utility with Limits): Amazon, Myntra, LinkedIn, Swiggy, Chrome. 5-min micro-sessions.
 * - Tier 2 (Dopamine Traps): Instagram, Reddit, YouTube. Intention speed bump & monochrome grayscale.
 */
object AppTierClassifier {

    private val EXACT_TIER_0 = setOf(
        // Phone / Dialer & Messages
        "com.google.android.dialer",
        "com.android.phone",
        "com.samsung.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        // UPI & Banking
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp", // BHIM
        "com.dreamplug.androidapp", // CRED
        // Maps & Mobility
        "com.google.android.apps.maps",
        "com.ubercab",
        "com.olacabs.customer",
        "com.rapido.passenger",
        // Camera & Essential Utilities
        "com.google.android.GoogleCamera",
        "com.android.camera",
        "com.sec.android.app.camera",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.google.android.calendar",
        "com.google.android.calculator",
        "com.android.calculator2",
    )

    private val EXACT_TIER_1 = setOf(
        // Shopping
        "in.amazon.mShop.android.shopping",
        "com.amazon.mShop.android.shopping",
        "com.flipkart.android",
        "com.myntra.android",
        "com.meesho.supply",
        "com.ril.ajio",
        // Food & Quick Commerce
        "in.swiggy.android",
        "com.application.zomato",
        "com.grofers.customerapp", // Blinkit
        "com.zeptoconsumerapp",
        "com.bigbasket.mobileapp",
        // Browsers & Professional Tools
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.linkedin.android",
        "com.slack",
        "com.microsoft.teams",
    )

    private val EXACT_TIER_2 = setOf(
        // Social Media & Infinite Scroll
        "com.instagram.android",
        "com.reddit.frontpage",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.twitter.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.snapchat.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.pinterest",
        "com.instagram.barcelona", // Threads
        // Streaming Entertainment
        "com.netflix.ninja",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient", // Prime Video
        "in.startv.hotstar",
        "tv.twitch.android.app",
    )

    private val TIER_0_SUBSTRINGS = listOf(
        "dialer", "phone", "messaging", "camera", "maps", "navigation",
        "uber", "olacabs", "rapido", "paytm", "phonepe", "paisa", "bhim",
        "banking", "clock", "calculator", "calendar", "notes", "keep",
        "authenticator", "password", "security",
    )

    private val TIER_1_SUBSTRINGS = listOf(
        "amazon", "shopping", "flipkart", "myntra", "meesho", "ajio",
        "swiggy", "zomato", "blinkit", "zepto", "grocery", "food",
        "chrome", "browser", "firefox", "linkedin", "slack", "teams",
        "trello", "jira", "drive", "cloud", "office",
    )

    private val TIER_2_SUBSTRINGS = listOf(
        "instagram", "reddit", "youtube", "tiktok", "twitter", "facebook",
        "snapchat", "pinterest", "threads", "shorts", "reels", "netflix",
        "primevideo", "hotstar", "twitch", "discord", "dating", "tinder", "bumble",
    )

    /**
     * Recommends the appropriate tier for an application based on package name and display label.
     */
    fun suggestTier(packageName: String, label: String = ""): AppTier {
        if (packageName in EXACT_TIER_2) return AppTier.TIER_2
        if (packageName in EXACT_TIER_0) return AppTier.TIER_0
        if (packageName in EXACT_TIER_1) return AppTier.TIER_1

        val combined = "${packageName.lowercase()} ${label.lowercase()}"

        if (TIER_2_SUBSTRINGS.any { combined.contains(it) }) return AppTier.TIER_2
        if (TIER_0_SUBSTRINGS.any { combined.contains(it) }) return AppTier.TIER_0
        if (TIER_1_SUBSTRINGS.any { combined.contains(it) }) return AppTier.TIER_1

        return AppTier.TIER_0
    }
}
