package com.rubenotepad.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.rubenotepad.app.data.AppPrefs
import java.io.IOException

/**
 * First screen of every launch.
 *
 * - First launch ever: promo video + subscribe popup + Continue.
 * - Later launches: same promo screen with a simple Continue (no subscribe popup).
 *
 * The video source and channel URL are configured in [OwnerConfig].
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs

    private lateinit var promoContainer: View
    private lateinit var videoView: VideoView
    private lateinit var promoWeb: WebView
    private lateinit var promoFallback: View
    private lateinit var btnOpenYoutube: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        title = OwnerConfig.APP_NAME

        prefs = AppPrefs(this)

        promoContainer = findViewById(R.id.promoContainer)
        videoView = findViewById(R.id.promoVideoView)
        promoWeb = findViewById(R.id.promoWebView)
        promoFallback = findViewById(R.id.promoFallback)
        btnOpenYoutube = findViewById(R.id.btnOpenYoutube)

        // Subscribe popup only on the very first launch of this installation.
        val subscribeCard = findViewById<MaterialCardView>(R.id.subscribeCard)
        subscribeCard.isVisible = !prefs.onboardingCompleted

        findViewById<MaterialButton>(R.id.btnSubscribe).setOnClickListener {
            openUrl(OwnerConfig.CHANNEL_URL)
        }

        btnOpenYoutube.setOnClickListener {
            openUrl(OwnerConfig.PROMO_WATCH_URL)
        }

        findViewById<MaterialButton>(R.id.btnContinue).setOnClickListener {
            onContinue()
        }

        setupPromo()
    }

    /** Persist onboarding completion, then open the note-taking app. */
    private fun onContinue() {
        prefs.onboardingCompleted = true
        startActivity(Intent(this, NotesActivity::class.java))
        finish()
    }

    // ------------------------------------------------------------------
    // Promo video setup (local asset or YouTube embed), with graceful fallback.
    // ------------------------------------------------------------------

    private fun setupPromo() {
        when (OwnerConfig.PROMO_SOURCE) {
            OwnerConfig.PromoSource.LOCAL_ASSET -> setupLocalVideo()
            OwnerConfig.PromoSource.YOUTUBE -> setupYoutubeEmbed()
        }
    }

    private fun setupLocalVideo() {
        val path = OwnerConfig.PROMO_LOCAL_FILE
        if (!assetExists(path)) {
            showFallback(getString(R.string.promo_missing_asset))
            return
        }
        promoFallback.isVisible = false
        videoView.isVisible = true
        videoView.setMediaController(MediaController(this))
        videoView.setVideoURI(Uri.parse("file:///android_asset/$path"))
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView.start()
        }
        videoView.setOnErrorListener { _, _, _ ->
            showFallback(getString(R.string.promo_playback_failed))
            true
        }
    }

    private fun setupYoutubeEmbed() {
        if (!isOnline()) {
            showFallback(getString(R.string.promo_offline))
            return
        }
        promoFallback.isVisible = false
        promoWeb.isVisible = true
        promoWeb.settings.javaScriptEnabled = true
        promoWeb.settings.mediaPlaybackRequiresUserGesture = false
        promoWeb.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.w(TAG, "Promo embed failed: ${error.description}")
                    // MIUI's WebView (Chromium build differs from AOSP) can invoke
                    // WebViewClient callbacks off the main thread. Touching views
                    // here directly causes CalledFromWrongThreadException on those
                    // devices, which is a hard crash right at launch. Hop back to
                    // the main thread before touching any UI.
                    runOnUiThread {
                        showFallback(getString(R.string.promo_offline))
                    }
                }
            }
        }
        promoWeb.loadUrl(
            "https://www.youtube-nocookie.com/embed/${OwnerConfig.PROMO_YOUTUBE_ID}?autoplay=1&loop=1&playlist=${OwnerConfig.PROMO_YOUTUBE_ID}&rel=0"
        )
    }

    private fun showFallback(message: String) {
        // Defensive guard: never touch views after the activity has started
        // finishing/destroying (e.g. a late/racing callback after the user
        // already backed out or rotated the screen).
        if (isFinishing || isDestroyed) return
        videoView.isVisible = false
        promoWeb.isVisible = false
        promoWeb.stopLoading()
        findViewById<android.widget.TextView>(R.id.tvPromoError).text = message
        promoFallback.isVisible = true
    }

    private fun assetExists(path: String): Boolean =
        try {
            assets.open(path).use { true }
        } catch (e: IOException) {
            false
        }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser available; nothing else we can do - never crash over a link.
            Log.w(TAG, "No activity to open URL", e)
        }
    }

    override fun onDestroy() {
        promoWeb.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
