package uk.co.actuabs.longhand

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader

/**
 * Longhand — a WebView shell around a single self-contained HTML file.
 *
 * The page is served over https://appassets.androidplatform.net rather than a
 * file:// URL. That matters: file:// origins are opaque, and localStorage on an
 * opaque origin is unreliable across WebView versions. Serving it over a real
 * https origin means streaks and stats actually survive.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private lateinit var root: FrameLayout

    companion object {
        private const val ORIGIN = "https://appassets.androidplatform.net"
        private const val PAGE = "$ORIGIN/assets/longhand.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 36 removes the edge-to-edge opt-out, so handle insets properly
        // rather than fighting it. Padding is applied natively, which keeps the
        // HTML identical to the browser build.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#121116"))
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = false
                useWideViewPort = false
                builtInZoomControls = false
                displayZoomControls = false
                textZoom = 100              // ignore system font scaling; the grid is fixed
                mediaPlaybackRequiresUserGesture = true
                allowFileAccess = false
                allowContentAccess = false
            }
            addJavascriptInterface(Bridge(), "LonghandBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean {
                    val url = request.url.toString()
                    if (url.startsWith(ORIGIN)) return false
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) = inject(view)
            }
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#121116"))
            addView(web, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Close an open sheet first; only leave the app if nothing is open.
                web.evaluateJavascript(
                    "(function(){var s=document.getElementById('scrim');" +
                    "if(s&&s.classList.contains('open')){s.classList.remove('open');return 'closed';}" +
                    "return 'exit';})()"
                ) { result ->
                    if (result?.contains("exit") == true) finish()
                }
            }
        })

        if (savedInstanceState == null) web.loadUrl(PAGE) else web.restoreState(savedInstanceState)
    }

    /**
     * Two shims, so longhand.html needs no Android-specific code and the same
     * file keeps working in a desktop browser:
     *   1. navigator.share -> the native share sheet
     *   2. report the page's theme colour so the inset padding matches the UI
     *      instead of showing dark bands around a light-themed board
     */
    private fun inject(view: WebView) = view.evaluateJavascript(
        """
        (function () {
          if (window.__lhShim) return; window.__lhShim = true;
          if (window.LonghandBridge) {
            navigator.share = function (d) {
              window.LonghandBridge.share((d && d.text) || '');
              return Promise.resolve();
            };
            var meta = document.querySelector('meta[name=theme-color]');
            var send = function () {
              if (meta) window.LonghandBridge.setBackground(meta.getAttribute('content'));
            };
            if (meta) new MutationObserver(send).observe(meta, { attributes: true });
            send();
          }
        })();
        """.trimIndent(), null
    )

    inner class Bridge {
        @JavascriptInterface
        fun share(text: String) {
            if (text.isBlank()) return
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(send, getString(R.string.share_title)))
        }

        @JavascriptInterface
        fun setBackground(hex: String) {
            val colour = runCatching { Color.parseColor(hex.trim()) }.getOrNull() ?: return
            runOnUiThread {
                root.setBackgroundColor(colour)
                web.setBackgroundColor(colour)
                val light = ColorUtilsLite.isLight(colour)
                WindowInsetsControllerCompat(window, root).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }
}

internal object ColorUtilsLite {
    fun isLight(c: Int): Boolean {
        val r = Color.red(c) / 255.0; val g = Color.green(c) / 255.0; val b = Color.blue(c) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) > 0.5
    }
}
