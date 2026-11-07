package com.zola.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

/* JADX INFO: loaded from: classes.dex */
final class Prefs {
    static final String AUTO_RECORD = "call_auto_record";
    static final String BAR_BOTTOM = "bar_bottom";
    static final String BAR_TOP = "bar_top";
    static final String BATTERY_ASKED = "battery_asked";
    static final String BUBBLES = "bubbles";
    static final String BUBBLE_AUTO_EXPAND = "bubble_auto_expand";
    static final String BUBBLE_HIDE_NOTIF = "bubble_hide_notif";
    static final String BUBBLE_ON_HOME = "bubble_on_home";
    static final String BUBBLE_SCOPE = "bubble_scope";
    static final String CALL_ALERTS = "call_alerts";
    static final String CALL_STATS = "call_stats";
    static final String DOCK_AUTO_HIDE = "dock_auto_hide";
    static final String DOCK_BOTTOM = "dock_bottom";
    static final String DOCK_CLOUD = "dock_cloud";
    static final String DOCK_OPACITY = "dock_opacity";
    static final String DOCK_TOOLS = "dock_tools";
    static final String INCOMING_CALLS = "incoming_calls";
    static final String KEEP_ALIVE = "keep_alive";
    static final String KEEP_ON_LOCK = "keep_on_lock";
    static final String MOBILE_UI = "mobile_ui";
    static final String NATIVE_LIST = "native_list";
    static final String NOTIF_ASKED = "notif_asked";
    static final String RINGBACK = "call_ringback";
    static final String RINGTONE = "call_ringtone";
    static final String START_ON_BOOT = "start_on_boot";
    static final String TEXT_ZOOM = "text_zoom";
    static final String VIEWPORT_WIDTH = "viewport_width";

    static boolean callAlerts(Context context) {
        return false;
    }

    private Prefs() {
    }

    static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences("zola", 0);
    }

    static boolean bool(Context context, String str, boolean z) {
        return sp(context).getBoolean(str, z);
    }

    static int integer(Context context, String str, int i) {
        return sp(context).getInt(str, i);
    }

    static void put(Context context, String str, boolean z) {
        sp(context).edit().putBoolean(str, z).apply();
    }

    static void put(Context context, String str, int i) {
        sp(context).edit().putInt(str, i).apply();
    }

    static boolean keepAlive(Context context) {
        return bool(context, KEEP_ALIVE, true);
    }

    static boolean startOnBoot(Context context) {
        return bool(context, START_ON_BOOT, true);
    }

    static boolean incomingCalls(Context context) {
        return bool(context, INCOMING_CALLS, true);
    }

    static boolean callStats(Context context) {
        return bool(context, CALL_STATS, true);
    }

    static boolean keepOnLock(Context context) {
        return bool(context, KEEP_ON_LOCK, false);
    }

    static Uri ringtone(Context context) {
        String string = sp(context).getString(RINGTONE, null);
        if (string == null) {
            return Settings.System.DEFAULT_RINGTONE_URI;
        }
        if (string.isEmpty()) {
            return null;
        }
        return Uri.parse(string);
    }

    static boolean hasCustomRingtone(Context context) {
        String string = sp(context).getString(RINGTONE, null);
        return (string == null || string.isEmpty() || Settings.System.DEFAULT_RINGTONE_URI.toString().equals(string)) ? false : true;
    }

    static void setRingtone(Context context, Uri uri) {
        if (uri == null || !Settings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
            sp(context).edit().putString(RINGTONE, uri == null ? "" : uri.toString()).apply();
        } else {
            sp(context).edit().remove(RINGTONE).apply();
        }
    }

    static int autoRecord(Context context) {
        return integer(context, AUTO_RECORD, 0);
    }

    static Uri defaultRingback(Context context) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ringback);
    }

    static Uri ringback(Context context) {
        String string = sp(context).getString(RINGBACK, null);
        if (string == null) {
            return defaultRingback(context);
        }
        if (string.isEmpty()) {
            return null;
        }
        return Uri.parse(string);
    }

    static boolean hasCustomRingback(Context context) {
        String string = sp(context).getString(RINGBACK, null);
        return (string == null || string.isEmpty() || defaultRingback(context).toString().equals(string)) ? false : true;
    }

    static void setRingback(Context context, Uri uri) {
        if (uri == null || !defaultRingback(context).equals(uri)) {
            sp(context).edit().putString(RINGBACK, uri == null ? "" : uri.toString()).apply();
        } else {
            sp(context).edit().remove(RINGBACK).apply();
        }
    }

    static boolean dockCloud(Context context) {
        return bool(context, DOCK_CLOUD, false);
    }

    static boolean dockTools(Context context) {
        return bool(context, DOCK_TOOLS, false);
    }

    static boolean bubbles(Context context) {
        return bool(context, BUBBLES, true);
    }

    static boolean bubbleAutoExpand(Context context) {
        return bool(context, BUBBLE_AUTO_EXPAND, false);
    }

    static boolean bubbleHideNotif(Context context) {
        return bool(context, BUBBLE_HIDE_NOTIF, false);
    }

    static boolean bubbleOnHome(Context context) {
        return bool(context, BUBBLE_ON_HOME, true);
    }

    static int bubbleScope(Context context) {
        return integer(context, BUBBLE_SCOPE, 0);
    }

    static boolean mobileUi(Context context) {
        return bool(context, MOBILE_UI, true);
    }

    static int viewportWidth(Context context) {
        return integer(context, VIEWPORT_WIDTH, 980);
    }

    static int textZoom(Context context) {
        int iTextZoomSetting = textZoomSetting(context);
        return iTextZoomSetting != 0 ? iTextZoomSetting : Math.max(50, Math.min(200, Math.round(context.getResources().getConfiguration().fontScale * 100.0f)));
    }

    static int textZoomSetting(Context context) {
        return integer(context, TEXT_ZOOM, 100);
    }

    static int dockOpacity(Context context) {
        return integer(context, DOCK_OPACITY, 60);
    }

    static boolean dockAutoHide(Context context) {
        return bool(context, DOCK_AUTO_HIDE, true);
    }

    static boolean nativeList(Context context) {
        return bool(context, NATIVE_LIST, true);
    }

    static int dockBottom(Context context) {
        return integer(context, DOCK_BOTTOM, 24);
    }

    static int barTop(Context context) {
        return integer(context, BAR_TOP, -1);
    }

    static int barBottom(Context context) {
        return integer(context, BAR_BOTTOM, -1);
    }
}
