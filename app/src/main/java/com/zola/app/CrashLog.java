package com.zola.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Process;
import android.webkit.WebView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class CrashLog {
    private static final String FILE = "last_crash.txt";

    private CrashLog() {
    }

    static void install(Context context) {
        final Context applicationContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: com.zola.app.CrashLog$$ExternalSyntheticLambda0
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public final void uncaughtException(Thread thread, Throwable th) {
                CrashLog.lambda$install$0(applicationContext, defaultUncaughtExceptionHandler, thread, th);
            }
        });
    }

    static /* synthetic */ void lambda$install$0(Context context, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Thread thread, Throwable th) {
        try {
            save(context, thread, th);
        } catch (Throwable unused) {
        }
        if (uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.uncaughtException(thread, th);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }

    private static void save(Context context, Thread thread, Throwable th) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("Zola " + versionName(context));
        printWriter.println("Thời điểm: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        printWriter.println("Máy: " + Build.MANUFACTURER + " " + Build.MODEL + " | Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        printWriter.println("WebView: " + webViewVersion());
        printWriter.println("Luồng: " + thread.getName());
        printWriter.println();
        th.printStackTrace(printWriter);
        printWriter.flush();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), FILE));
        try {
            fileOutputStream.write(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } catch (Throwable th2) {
            try {
                fileOutputStream.close();
            } catch (Throwable th3) {
                th2.addSuppressed(th3);
            }
            throw th2;
        }
    }

    static boolean hasPending(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) {
            return false;
        }
        try {
            if (file.lastModified() >= context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime) {
                return true;
            }
            clear(context);
            return false;
        } catch (Exception unused) {
            return true;
        }
    }

    static String read(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                int iMin = (int) Math.min(file.length(), 262144L);
                byte[] bArr = new byte[iMin];
                int i = 0;
                while (i < iMin) {
                    int i2 = fileInputStream.read(bArr, i, iMin - i);
                    if (i2 < 0) {
                        break;
                    }
                    i += i2;
                }
                String str = new String(bArr, 0, i, StandardCharsets.UTF_8);
                fileInputStream.close();
                return str;
            } catch (Throwable th) {
                try {
                    fileInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
            return "Không đọc được log lỗi: " + e;
        }
    }

    static void clear(Context context) {
        new File(context.getFilesDir(), FILE).delete();
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
            return currentWebViewPackage == null ? "không rõ" : currentWebViewPackage.packageName + " " + currentWebViewPackage.versionName;
        } catch (Throwable th) {
            return "không rõ (" + th.getClass().getSimpleName() + ")";
        }
    }
}
