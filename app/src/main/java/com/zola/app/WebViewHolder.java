package com.zola.app;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebStorage;
import android.widget.Toast;
import org.json.JSONArray;

public class WebViewHolder {
    public static volatile ZaloWebView main;
    public static volatile String userAgent = "";

    public static MainActivity host() {
        if (main != null && main.getContext() instanceof MainActivity) {
            return (MainActivity) main.getContext();
        }
        return null;
    }

    public static ZaloWebView get(Context context) {
        return main;
    }

    public static void wake(String reason) {
        evalMain("window.__zw&&window.__zw.wake&&window.__zw.wake()");
    }

    public static void reload() {
        App.MAIN.post(() -> {
            if (main != null) {
                main.reload();
            }
        });
    }

    public static void applyDockButtons() {
        if (main == null) return;
        Context c = main.getContext();
        main.evaluateJavascript("window.__zw&&window.__zw.setDockButtons&&window.__zw.setDockButtons(" + Prefs.dockCloud(c) + "," + Prefs.dockTools(c) + ")", null);
    }

    public static void applyDockSettings() {
        if (main == null) return;
        Context c = main.getContext();
        main.evaluateJavascript("window.__zw&&window.__zw.setDock&&window.__zw.setDock(" + Prefs.dockOpacity(c) + "," + Prefs.dockAutoHide(c) + "," + Prefs.dockBottom(c) + ")", null);
    }

    public static void applySettings(boolean reload) {
        if (main == null) return;
        Context c = main.getContext();
        main.getSettings().setTextZoom(Prefs.textZoom(c));
        if (reload) {
            reload();
        }
    }

    public static void dumpLayout(ValueCallback<String> cb) {
        if (main == null) {
            cb.onReceiveValue(null);
            return;
        }
        main.evaluateJavascript("(function(){return document.documentElement.outerHTML;})()", cb);
    }

    public static void scriptStatus(ValueCallback<String> cb) {
        if (main == null) {
            cb.onReceiveValue(null);
            return;
        }
        main.evaluateJavascript("(function(){var z=window.__zw;if(!z)return 'none';return 'v'+(z.version||'?')+'|'+(z.mobileInfo?z.mobileInfo():'{}');})()", val -> {
            cb.onReceiveValue(decodeJsString(val));
        });
    }

    public static void debugState(ValueCallback<String> cb) {
        if (main == null) {
            cb.onReceiveValue(null);
            return;
        }
        main.evaluateJavascript("(function(){var z=window.__zw;if(!z)return 'script: not running';var d=document;return 'script v'+(z.version||'?')+' | layout '+(z.mobileInfo?z.mobileInfo():'?')+' | ws '+(z.wsInfo?z.wsInfo():'?')+' | logged in '+!!d.getElementById('sidebarNav')+' | online '+navigator.onLine+' | hidden '+d.hidden+' | html.'+d.documentElement.className+' | url '+location.host+location.pathname+' | viewport '+innerWidth+'x'+innerHeight;})()", val -> {
            cb.onReceiveValue(decodeJsString(val));
        });
    }

    private static String decodeJsString(String str) {
        if (str == null || "null".equals(str)) return null;
        try {
            return new JSONArray("[" + str + "]").getString(0);
        } catch (Exception unused) {
            return str;
        }
    }

    public static void resetAll(Context context, Runnable done) {
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookies(val -> {
            cm.flush();
            WebStorage.getInstance().deleteAllData();
            if (main != null) {
                main.clearCache(true);
                main.clearHistory();
                main.loadUrl("https://chat.zalo.me/");
            }
            if (done != null) done.run();
        });
    }

    public static void evalMain(String script) {
        App.MAIN.post(() -> {
            if (main != null) {
                main.evaluateJavascript(script, null);
            }
        });
    }

    public static void toast(String str) {
        App.MAIN.post(() -> {
            if (main != null) {
                Toast.makeText(main.getContext(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
