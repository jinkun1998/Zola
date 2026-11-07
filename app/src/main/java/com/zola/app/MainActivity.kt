package com.zola.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.content.res.Configuration
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.UUID
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var popupContainer: FrameLayout
    private lateinit var statusScrim: View
    private lateinit var webView: ZaloWebView
    private lateinit var nativeList: NativeChatList
    private lateinit var nativeDock: NativeDock

    private val bridgeToken = UUID.randomUUID().toString()
    private lateinit var fullScript: String

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { res ->
        val uris = WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data)
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    private var pendingWebPermission: PermissionRequest? = null

    private val scriptOrigins = setOf(
        "https://zalo.me", "https://*.zalo.me",
        "https://zaloapp.com", "https://*.zaloapp.com",
        "https://zalo.vn", "https://*.zalo.vn"
    )

    companion object {
        const val ACTION_RELOAD = "com.zola.app.RELOAD"
        @JvmField
        var visible: Boolean = false

        private var barsLoaded: Boolean = false
        private var barTop: Int = -1
        private var barBottom: Int = -1

        @JvmStatic
        fun pageBottomColor(): Int = barBottom
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        webContainer = FrameLayout(this)
        popupContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }
        statusScrim = View(this)

        root.addView(webContainer, match())
        root.addView(statusScrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP))
        root.addView(popupContainer, match())
        setContentView(root)

        setupWindow()
        applyInsets()

        val night = isNightMode()
        if (!barsLoaded) {
            val savedTop = Prefs.barTop(this)
            val savedBottom = Prefs.barBottom(this)
            val isSavedLight = Color.luminance(savedTop) > 0.5f
            if (night == isSavedLight) {
                barTop = if (night) Color.parseColor("#121212") else Color.WHITE
                barBottom = savedBottom
            } else {
                barTop = savedTop
                barBottom = savedBottom
            }
            barsLoaded = true
        }
        applyBarColors()

        DiagLog.init(this)
        loadScript()
        setupNativeUI()
        setupWebView()
        applyNightMode(night)
        setupBackHandler()
        handleIntent(intent)

        if (!askNotificationPermissionOnFirstRun()) {
            maybeAskBattery()
        }

        Updater.check(this, false)

        webView.loadUrl("https://chat.zalo.me/")
    }

    private fun setupWindow() {
        val w = window
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false)
            if (Build.VERSION.SDK_INT < 35) {
                w.statusBarColor = 0
                w.navigationBarColor = 0
            }
        } else {
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun applyInsets() {
        if (Build.VERSION.SDK_INT < 30) return
        root.setOnApplyWindowInsetsListener { _, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsets.Type.ime())
            val systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars())
            val cutout = windowInsets.getInsets(WindowInsets.Type.displayCutout())
            val topInset = maxOf(systemBars.top, cutout.top)
            val leftInset = maxOf(systemBars.left, cutout.left)
            val rightInset = maxOf(systemBars.right, cutout.right)
            val bottomInset = ime.bottom

            webContainer.setPadding(leftInset, topInset, rightInset, bottomInset)
            popupContainer.setPadding(leftInset, topInset, rightInset, maxOf(systemBars.bottom, ime.bottom))
            val lp = statusScrim.layoutParams
            if (lp.height != topInset) {
                lp.height = topInset
                statusScrim.layoutParams = lp
            }
            windowInsets
        }
    }

    private fun applyBarColors() {
        statusScrim.setBackgroundColor(barTop)
        root.setBackgroundColor(barBottom)
        val isLightTop = Color.luminance(barTop) > 0.5f
        val isLightBottom = Color.luminance(barBottom) > 0.5f

        if (Build.VERSION.SDK_INT >= 30) {
            val controller = insetsController() ?: return
            val appearance = (if (isLightTop) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0) or
                    (if (isLightBottom) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0)
            controller.setSystemBarsAppearance(
                appearance,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (isLightTop) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26 && isLightBottom) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun insetsController(): WindowInsetsController? {
        if (Build.VERSION.SDK_INT < 30) return null
        return try {
            window.decorView.windowInsetsController ?: window.insetsController
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun isNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyNightMode(night: Boolean) {
        if (::webView.isInitialized) {
            try {
                if (Build.VERSION.SDK_INT >= 33 && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, night)
                } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    @Suppress("DEPRECATION")
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        if (night) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            } catch (_: Exception) {}
        }

        val isTopLight = Color.luminance(barTop) > 0.5f
        if (night == isTopLight) {
            barTop = if (night) Color.parseColor("#121212") else Color.WHITE
            Prefs.put(this, "bar_top", barTop)
        }

        applyBarColors()
        WebViewHolder.evalMain("window.__zw && window.__zw.refreshBars && window.__zw.refreshBars()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val night = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        applyNightMode(night)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val night = isNightMode()
            val isTopLight = Color.luminance(barTop) > 0.5f
            if (night == isTopLight) {
                applyNightMode(night)
            } else {
                applyBarColors()
            }
        }
    }

    private fun loadScript() {
        val shimSource = assets.open("shim.js").bufferedReader().use { it.readText() }
        fullScript = "if(!window.__zw)window.__ZW_CFG={bt:\"$bridgeToken\",viewportWidth:0,ringDetect:false,mobile:true,dockOpacity:100,dockAutoHide:false,dockBottom:0,dockCloud:false,dockTools:false,nativeList:true};\n$shimSource"
    }

    private fun setupNativeUI() {
        nativeList = NativeChatList(this)
        nativeDock = NativeDock(this).apply {
            setGlass(nativeList.dockGlass())
        }
        nativeList.onOpen = Runnable {
            nativeDock.hideForOpen()
        }
        nativeList.boot(true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(true)

        val defaultUa = WebSettings.getDefaultUserAgent(this)
        val chromeVer = Pattern.compile("Chrome/(\\d+)").matcher(defaultUa).takeIf { it.find() }?.group(1) ?: "130"
        val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVer.0.0.0 Safari/537.36"

        webView = ZaloWebView(this, false)
        WebViewHolder.main = webView
        WebViewHolder.userAgent = desktopUA
        webView.setDownloadListener(Downloads.Listener(this, webView))

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            useWideViewPort = false
            loadWithOverviewMode = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = true
            userAgentString = desktopUA
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, fullScript, scriptOrigins)
        }

        webView.addJavascriptInterface(JsBridge(bridgeToken, this, webView), "ZaloBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val uri = request.url
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme == "http" || scheme == "https") {
                    val host = uri.host ?: return false
                    if (isTrustedHost(host)) return false
                }
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (url.startsWith("https://chat.zalo.me")) {
                    nativeList.postDelayed({ nativeList.boot(false) }, 500)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                if (resultMsg == null) return false
                val popup = ZaloWebView(this@MainActivity, true).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = desktopUA
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                            val host = req.url.host ?: return false
                            if (isTrustedHost(host)) return false
                            return try {
                                startActivity(Intent(Intent.ACTION_VIEW, req.url))
                                true
                            } catch (_: Exception) { false }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onCloseWindow(window: WebView?) {
                            closePopup(window)
                        }
                    }
                }
                popupContainer.addView(popup, match())
                popupContainer.visibility = View.VISIBLE
                (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                closePopup(window)
            }

            override fun onShowFileChooser(wv: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                return try {
                    fileLauncher.launch(intent)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val missing = mutableListOf<String>()
                for (res in request.resources) {
                    if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(android.Manifest.permission.RECORD_AUDIO)
                    }
                    if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(android.Manifest.permission.CAMERA)
                    }
                }
                if (missing.isEmpty()) {
                    request.grant(request.resources)
                } else {
                    pendingWebPermission = request
                    requestPermissions(missing.toTypedArray(), 102)
                }
            }
        }

        webContainer.removeAllViews()
        webContainer.addView(nativeList, match())
        webContainer.addView(webView, match())
        webContainer.addView(nativeDock, nativeDock.params())
    }

    fun onLoginPage() {
        nativeList.cancelBoot()
        nativeDock.hideNow()
        webView.setBackgroundColor(Color.WHITE)
        webView.passTop = -1
    }

    fun onPageReady() {
        Log.d("Zola", "Zalo web page ready")
        nativeList.boot(false)
        webView.evaluateJavascript("window.__zw && window.__zw.nativeReady && window.__zw.nativeReady(true); window.__zw && window.__zw.requestConvList && window.__zw.requestConvList(); window.__zw && window.__zw.nativeDock && window.__zw.nativeDock(true);", null)
    }

    fun onConvList(json: String) {
        nativeList.setData(json)
    }

    fun onNativeLayout(json: String, wv: ZaloWebView) {
        nativeList.setLayout(json, wv)
    }

    fun onSystemBars(top: String?, bottom: String?) {
        try {
            val night = isNightMode()
            val defaultColor = if (night) Color.parseColor("#121212") else Color.WHITE
            val parsedTop = if (top != null) Color.parseColor(top) else defaultColor
            val parsedBottom = if (bottom != null) Color.parseColor(bottom) else defaultColor
            val topColor = if (night && Color.luminance(parsedTop) > 0.5f) Color.parseColor("#121212") else parsedTop
            val bottomColor = parsedBottom
            setSystemBarColors(topColor, bottomColor)
        } catch (_: Exception) {}
    }

    private fun setSystemBarColors(top: Int, bottom: Int) {
        if (barsLoaded && top == barTop && bottom == barBottom) return
        barTop = top
        barBottom = bottom
        barsLoaded = true
        Prefs.put(this, "bar_top", top)
        Prefs.put(this, "bar_bottom", bottom)
        if (nativeList.visibility != View.VISIBLE) {
            webView.setBackgroundColor(bottom)
        }
        applyBarColors()
    }

    fun onDockState(state: String?) {
        nativeDock.setState(state)
    }

    fun onConnState(state: String?) {
        nativeList.setConn(state)
    }

    fun onConvOpened(id: String?) {}

    fun onMobileState(open: Boolean) {}

    private fun closePopup(view: View?) {
        if (view == null) return
        popupContainer.removeView(view)
        if (popupContainer.childCount == 0) {
            popupContainer.visibility = View.GONE
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (popupContainer.childCount > 0) {
                    val topPopup = popupContainer.getChildAt(popupContainer.childCount - 1) as? WebView
                    if (topPopup?.canGoBack() == true) {
                        topPopup.goBack()
                    } else {
                        closePopup(topPopup)
                    }
                    return
                }
                webView.evaluateJavascript("!!(window.__zw && window.__zw.back && window.__zw.back())") { res ->
                    if (res != "true") {
                        moveTaskToBack(true)
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        visible = true
        KeepAliveService.start(this)
        Notifier.cancelCallAlert(this)
    }

    override fun onStop() {
        visible = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            val act = intent.action
            if (act == ACTION_RELOAD || act == "com.zola.app.RELOAD") {
                WebViewHolder.reload()
            }
        }
        val clickId = intent.getStringExtra("zw_click_id")
        if (clickId != null) {
            intent.removeExtra("zw_click_id")
            WebViewHolder.evalMain("window.__zw&&window.__zw.click(" + org.json.JSONObject.quote(clickId) + ")")
        }
        val convId = intent.getStringExtra("zw_conv")
        if (convId != null && convId.isNotEmpty()) {
            intent.removeExtra("zw_conv")
            WebViewHolder.evalMain("window.__zw&&window.__zw.openConv&&window.__zw.openConv(" + org.json.JSONObject.quote(convId) + ")")
        }
    }

    private fun askNotificationPermissionOnFirstRun(): Boolean {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Prefs.bool(this, "notif_asked", false)) {
            return false
        }
        requestNotificationPermission()
        return true
    }

    private fun maybeAskBattery() {
        if (!Prefs.keepAlive(this) || Prefs.bool(this, "battery_asked", false)) return
        val pm = getSystemService(PowerManager::class.java)
        if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) return
        Prefs.put(this, "battery_asked", true)
        AlertDialog.Builder(this)
            .setTitle(R.string.battery_title)
            .setMessage(R.string.battery_message)
            .setPositiveButton(R.string.battery_ok) { _, _ ->
                SettingsActivity.requestIgnoreBattery(this)
            }
            .setNegativeButton(R.string.later, null)
            .show()
    }

    fun requestStoragePermission() {}

    fun onMediaActiveChanged(active: Boolean) {}

    fun requestNotificationPermission(callbackId: String? = null) {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            Prefs.put(this, "notif_asked", true)
            maybeAskBattery()
        } else if (requestCode == 102) {
            val req = pendingWebPermission
            pendingWebPermission = null
            if (req != null) {
                val available = mutableListOf<String>()
                for (res in req.resources) {
                    if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        available.add(res)
                    } else if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        available.add(res)
                    }
                }
                if (available.isNotEmpty()) req.grant(available.toTypedArray()) else req.deny()
            }
        }
    }

    private fun isTrustedHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "zalo.me" || h.endsWith(".zalo.me") ||
                h == "zaloapp.com" || h.endsWith(".zaloapp.com") ||
                h == "zalo.vn" || h.endsWith(".zalo.vn") ||
                h == "zadn.vn" || h.endsWith(".zadn.vn")
    }

    private fun match() = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
}
