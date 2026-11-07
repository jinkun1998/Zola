package com.zola.app

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class JsBridge(
    private val onConversations: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onConvList(json: String) {
        handler.post {
            onConversations(json)
        }
    }
}
