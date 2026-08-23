package com.rubenotepad.app

/**
 * ============================== OWNER CONFIGURATION ==============================
 *
 * This is the single place the application owner edits branding and promo assets.
 * No other file needs to change when replacing the video or channel link.
 */
object OwnerConfig {

    /** Display name used across the app UI and metadata. */
    const val APP_NAME: String = "Rube Note Pad"

    // ------------------------------------------------------------------
    // Promotional video shown on the onboarding screen.
    // ------------------------------------------------------------------

    /** Where the promo video comes from. */
    enum class PromoSource {
        /** Plays a bundled video from app/src/main/assets/promo/promo_video.mp4 (works offline). */
        LOCAL_ASSET,

        /** Streams an embedded YouTube video (requires internet on first view). */
        YOUTUBE
    }

    /**
     * Choose LOCAL_ASSET to bundle the edited promotional video with the APK,
     * or YOUTUBE to stream it. Recommended: LOCAL_ASSET so onboarding works offline.
     */
    val PROMO_SOURCE: PromoSource = PromoSource.YOUTUBE

    /** Path inside src/main/assets/ of the bundled promo video (used by LOCAL_ASSET). */
    const val PROMO_LOCAL_FILE: String = "promo/promo_video.mp4"

    /** The YouTube video ID of the promo short (used by YOUTUBE). */
    const val PROMO_YOUTUBE_ID: String = "zmgrXIhsBzU"

    /** Direct watch link, used as a fallback button if embedded playback fails. */
    const val PROMO_WATCH_URL: String = "https://www.youtube.com/shorts/zmgrXIhsBzU"

    // ------------------------------------------------------------------
    // Owner's YouTube channel.
    // ------------------------------------------------------------------

    /** Channel opened by the Subscribe button on the first-launch popup. */
    const val CHANNEL_URL: String = "https://www.youtube.com/@RubeCoder"
}
