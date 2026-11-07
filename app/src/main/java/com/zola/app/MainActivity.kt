package com.zola.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var nativeList: NativeChatList
    private lateinit var nativeDock: NativeDock
    private var inConversation = false

    private val desktopUA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        setContentView(root)

        setupWebView()
        setupNativeUI()
        setupBackHandler()

        webView.loadUrl("https://chat.zalo.me/")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = desktopUA
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.addJavascriptInterface(
            JsBridge { json ->
                nativeList.setData(json)
                if (!inConversation && nativeList.hasData()) {
                    showNativeUI()
                }
            },
            "ZaloBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host ?: return true
                return !(host.contains("zalo.me") || host.contains("zalo.vn") || host.contains("zadn.vn"))
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectShim()
                if (url.contains("id.zalo.me") || url.contains("login")) {
                    hideNativeUIForLogin()
                }
            }
        }

        root.addView(webView)
    }

    private fun setupNativeUI() {
        val dp = resources.displayMetrics.density
        val dockHeight = (54 * dp).toInt()

        nativeList = NativeChatList(this) { conv ->
            openConversation(conv.id)
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                bottomMargin = dockHeight
            }
        }
        root.addView(nativeList)

        nativeDock = NativeDock(this) { tab ->
            if (tab == 0 && inConversation) {
                closeConversation()
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dockHeight,
                Gravity.BOTTOM
            )
        }
        root.addView(nativeDock)

        if (!nativeList.hasData()) {
            hideNativeUIForLogin()
        }
    }

    private fun hideNativeUIForLogin() {
        nativeList.visibility = View.GONE
        nativeDock.visibility = View.GONE
    }

    private fun showNativeUI() {
        nativeList.visibility = View.VISIBLE
        nativeDock.visibility = View.VISIBLE
    }

    private fun openConversation(convId: String) {
        inConversation = true
        webView.evaluateJavascript(
            "window.__zw && window.__zw.openConv && window.__zw.openConv('$convId');",
            null
        )
        nativeList.animate()
            .translationX(-root.width.toFloat())
            .setDuration(220)
            .withEndAction {
                nativeList.visibility = View.GONE
            }
            .start()
        nativeDock.visibility = View.GONE
    }

    private fun closeConversation() {
        inConversation = false
        nativeList.visibility = View.VISIBLE
        nativeList.animate()
            .translationX(0f)
            .setDuration(220)
            .start()
        nativeDock.visibility = View.VISIBLE
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (inConversation) {
                    closeConversation()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
    }

    private fun injectShim() {
        try {
            val script = assets.open("shim.js").bufferedReader().use { it.readText() }
            webView.evaluateJavascript(script, null)
        } catch (_: Exception) {}
    }
}
