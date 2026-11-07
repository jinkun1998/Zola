package com.zola.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface

class JsBridge(
    private val expectedToken: String,
    private val mainActivity: MainActivity,
    private val webView: ZaloWebView
) {
    private val handler = Handler(Looper.getMainLooper())

    private fun ok(token: String?): Boolean = token == expectedToken

    @JavascriptInterface
    fun needLogin(token: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onLoginPage() }
    }

    @JavascriptInterface
    fun pageReady(token: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onPageReady() }
    }

    @JavascriptInterface
    fun convList(token: String?, json: String?) {
        if (!ok(token) || json == null) return
        handler.post { mainActivity.onConvList(json) }
    }

    @JavascriptInterface
    fun nativeLayout(token: String?, json: String?) {
        if (!ok(token) || json == null) return
        handler.post { mainActivity.onNativeLayout(json, webView) }
    }

    @JavascriptInterface
    fun systemBars(token: String?, top: String?, bottom: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onSystemBars(top, bottom) }
    }

    @JavascriptInterface
    fun dockState(token: String?, state: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onDockState(state) }
    }

    @JavascriptInterface
    fun connState(token: String?, state: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onConnState(state) }
    }

    @JavascriptInterface
    fun convOpened(token: String?, id: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.onConvOpened(id) }
    }

    @JavascriptInterface
    fun mobileState(token: String?, open: Boolean) {
        if (!ok(token)) return
        handler.post { mainActivity.onMobileState(open) }
    }

    @JavascriptInterface
    fun isHidden(token: String?): Boolean = !MainActivity.visible

    @JavascriptInterface
    fun permission(token: String?): String {
        return if (!ok(token)) "default" else Notifier.permissionState(mainActivity)
    }

    @JavascriptInterface
    fun log(token: String?, msg: String?) {
        if (ok(token)) Log.d("ZolaJS", msg ?: "")
    }

    @JavascriptInterface
    fun diag(token: String?, msg: String?) {
        if (ok(token)) DiagLog.add("web: " + (msg ?: ""))
    }

    @JavascriptInterface
    fun connLog(token: String?, msg: String?) {
        if (ok(token)) DiagLog.conn("web: " + (msg ?: ""))
    }

    @JavascriptInterface
    fun showNotification(token: String?, id: String?, title: String?, body: String?, icon: String?, tag: String?, silent: Boolean) {
        if (!ok(token)) return
        Notifier.show(mainActivity, id, title, body, tag, icon, silent)
    }

    @JavascriptInterface
    fun cancelNotification(token: String?, id: String?, tag: String?) {
        if (!ok(token)) return
        Notifier.cancel(mainActivity, id)
    }

    @JavascriptInterface
    fun requestPermission(token: String?, callbackId: String?) {
        if (!ok(token)) return
        handler.post { mainActivity.requestNotificationPermission(callbackId) }
    }

    @JavascriptInterface
    fun mediaActive(token: String?, active: Boolean, isCall: Boolean) {
        if (!ok(token)) return
        KeepAliveService.setMediaActive(mainActivity, active, isCall)
    }

    @JavascriptInterface
    fun ringing(token: String?, ring: Boolean) {
        if (!ok(token)) return
        if (ring) Notifier.showRingAlert(mainActivity) else Notifier.cancelCallAlert(mainActivity)
    }

    @JavascriptInterface
    fun saveFile(token: String?, name: String?, mime: String?, base64: String?) {
        if (!ok(token)) return
        Downloads.saveBase64(mainActivity, name, mime, base64)
    }

    @JavascriptInterface
    fun shareFile(token: String?, name: String?, mime: String?, base64: String?) {
        if (!ok(token) || base64 == null) return
        handler.post { ShareFiles.share(mainActivity, name, mime, base64) }
    }

    @JavascriptInterface
    fun prefetch(token: String?, json: String?) {}

    @JavascriptInterface
    fun showKeyboard(token: String?) {}

    @JavascriptInterface
    fun callEvent(token: String?, dir: String?, ws: Int, cmd: Int, sub: Int, body: String?) {}

    @JavascriptInterface
    fun callSigResult(token: String?, reqId: String?, ok: Boolean, result: String?) {}

    @JavascriptInterface
    fun startCall(token: String?, uid: String?, name: String?, avatar: String?, video: Boolean) {}

    @JavascriptInterface
    fun startGroupCall(token: String?, id: String?, name: String?, avatar: String?, extra: String?, count: String?) {}
}
