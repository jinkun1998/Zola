package com.zola.app;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class DiagLog {

    /* JADX INFO: renamed from: app, reason: collision with root package name */
    private static Context f5app;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
    private static final Ring MAIN = new Ring("diag.log", 600);
    private static final Ring CONN = new Ring("conn.log", 1500);

    private DiagLog() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Ring {
        boolean dirty;
        final String file;
        final ArrayDeque<String> lines = new ArrayDeque<>();
        final int max;

        Ring(String str, int i) {
            this.file = str;
            this.max = i;
        }

        void load() {
            File file = new File(DiagLog.f5app.getFilesDir(), this.file);
            if (file.exists()) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        push(line);
                    }
                } catch (IOException e) {
                    Log.w("Zola", "read " + this.file, e);
                }
            }
        }

        void push(String str) {
            this.lines.addLast(str);
            while (this.lines.size() > this.max) {
                this.lines.removeFirst();
            }
        }

        void add(String str) {
            synchronized (DiagLog.TIME) {
                push(DiagLog.TIME.format(new Date()) + "  " + str);
            }
            scheduleSave();
        }

        void scheduleSave() {
            if (this.dirty) {
                return;
            }
            this.dirty = true;
            App.IO.execute(Ring.this::save);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void save() {
            String str;
            synchronized (DiagLog.class) {
                this.dirty = false;
                str = this.lines.isEmpty() ? "" : String.join("\n", this.lines) + "\n";
            }
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(DiagLog.f5app.getFilesDir(), this.file)), StandardCharsets.UTF_8);
                try {
                    outputStreamWriter.write(str);
                    outputStreamWriter.close();
                } catch (Throwable th) {
                    try {
                        outputStreamWriter.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                Log.w("Zola", "save " + this.file, e);
            }
        }
    }

    static synchronized void init(Context context) {
        if (f5app != null) {
            return;
        }
        f5app = context.getApplicationContext();
        MAIN.load();
        CONN.load();
    }

    static void add(String str) {
        Log.d("Zola", "diag: " + str);
        synchronized (DiagLog.class) {
            if (f5app != null) {
                MAIN.add(str);
            }
        }
    }

    static void conn(String str) {
        synchronized (DiagLog.class) {
            if (f5app != null) {
                CONN.add(str);
            }
        }
    }

    static void sys(String str) {
        Log.d("Zola", "diag: " + str);
        synchronized (DiagLog.class) {
            if (f5app == null) {
                return;
            }
            MAIN.add(str);
            CONN.add(str);
        }
    }

    static String powerState(Context context) {
        String str;
        StringBuilder sb = new StringBuilder();
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
            if (powerManager != null) {
                sb.append("screen=").append(powerManager.isInteractive() ? "on" : "off").append(" saver=").append(powerManager.isPowerSaveMode()).append(" doze=").append(powerManager.isDeviceIdleMode()).append(" batteryUnrestricted=").append(powerManager.isIgnoringBatteryOptimizations(context.getPackageName()));
            }
            if (Build.VERSION.SDK_INT >= 28) {
                UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(UsageStatsManager.class);
                if (usageStatsManager != null) {
                    sb.append(" bucket=").append(usageStatsManager.getAppStandbyBucket());
                }
                ActivityManager activityManager = (ActivityManager) context.getSystemService(ActivityManager.class);
                if (activityManager != null) {
                    sb.append(" bgRestricted=").append(activityManager.isBackgroundRestricted());
                }
            }
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
            if (connectivityManager != null) {
                sb.append(" dataSaver=").append(connectivityManager.getRestrictBackgroundStatus());
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                StringBuilder sbAppend = sb.append(" net=");
                if (networkCapabilities == null) {
                    str = "none";
                } else if (networkCapabilities.hasTransport(1)) {
                    str = "wifi";
                } else {
                    str = networkCapabilities.hasTransport(0) ? "cell" : "other";
                }
                sbAppend.append(str);
            }
        } catch (RuntimeException e) {
            sb.append(" err=").append(e.getClass().getSimpleName());
        }
        return sb.toString().trim();
    }

    static synchronized String dump(Context context) {
        StringBuilder sb;
        sb = new StringBuilder();
        sb.append("Zola ").append(versionName(context)).append(" | ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" | Android ").append(Build.VERSION.RELEASE).append(" | WebView ").append(webViewVersion()).append('\n');
        Iterator<String> it = MAIN.lines.iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append('\n');
        }
        appendExitReasons(context, sb);
        return sb.toString();
    }

    static synchronized String dumpConn(Context context) {
        StringBuilder sb;
        sb = new StringBuilder("=== Zola connection log ===\n");
        sb.append("Zola ").append(versionName(context)).append(" | ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" | Android ").append(Build.VERSION.RELEASE).append(" | WebView ").append(webViewVersion()).append('\n');
        sb.append("now: ").append(powerState(context)).append(" keepAlive=").append(Prefs.keepAlive(context)).append('\n');
        Iterator<String> it = CONN.lines.iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append('\n');
        }
        appendExitReasons(context, sb);
        return sb.toString();
    }

    private static void appendExitReasons(Context context, StringBuilder sb) {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        try {
            List<ApplicationExitInfo> historicalProcessExitReasons = ((ActivityManager) context.getSystemService(ActivityManager.class)).getHistoricalProcessExitReasons(null, 0, 15);
            if (historicalProcessExitReasons.isEmpty()) {
                return;
            }
            sb.append("--- process exits (newest first)\n");
            synchronized (TIME) {
                try {
                    for (ApplicationExitInfo applicationExitInfo : historicalProcessExitReasons) {
                        sb.append(TIME.format(new Date(applicationExitInfo.getTimestamp()))).append("  ").append(applicationExitInfo.getProcessName().contains("sandboxed") ? "webview renderer" : "app").append(" reason=").append(applicationExitInfo.getReason()).append(" importance=").append(applicationExitInfo.getImportance()).append(" ").append(applicationExitInfo.getDescription()).append('\n');
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        } catch (RuntimeException e) {
            sb.append("--- process exits unavailable: ").append(e).append('\n');
        }
    }

    static synchronized void clear() {
        if (f5app == null) {
            return;
        }
        Ring ring = MAIN;
        ring.lines.clear();
        ring.scheduleSave();
    }

    static synchronized void clearConn() {
        if (f5app == null) {
            return;
        }
        Ring ring = CONN;
        ring.lines.clear();
        ring.scheduleSave();
    }

    private static String versionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception unused) {
            return "?";
        }
    }

    private static String webViewVersion() {
        try {
            PackageInfo currentWebViewPackage = WebView.getCurrentWebViewPackage();
            if (currentWebViewPackage == null) {
                return "?";
            }
            return currentWebViewPackage.packageName + " " + currentWebViewPackage.versionName;
        } catch (RuntimeException unused) {
            return "?";
        }
    }
}
