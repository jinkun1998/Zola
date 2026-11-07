package com.zola.app;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class Downloads {
    private Downloads() {
    }

    static final class Listener implements DownloadListener {

        /* JADX INFO: renamed from: app, reason: collision with root package name */
        private final Context f6app;
        private final WebView owner;

        Listener(Context context, WebView webView) {
            this.f6app = context.getApplicationContext();
            this.owner = webView;
        }

        @Override // android.webkit.DownloadListener
        public void onDownloadStart(String str, String str2, String str3, String str4, long j) {
            String strGuessFileName = URLUtil.guessFileName(str, str3, str4);
            if (str.startsWith("blob:") || str.startsWith("data:")) {
                this.owner.evaluateJavascript("window.__zw&&window.__zw.saveUrl(" + JSONObject.quote(str) + "," + JSONObject.quote(strGuessFileName) + ")", null);
            } else if (str.startsWith("https://") || str.startsWith("http://")) {
                Downloads.enqueue(this.f6app, str, str2, strGuessFileName, str4);
            }
        }
    }

    static void enqueue(Context context, String str, String str2, String str3, String str4) {
        String strKindOf = Storage.kindOf(str4, str3);
        if (strKindOf != null) {
            fetchToStorage(context, str, str2, sanitize(str3), str4, strKindOf);
            return;
        }
        if (Build.VERSION.SDK_INT < 29 && !hasLegacyStoragePermission(context)) {
            askStoragePermission(context);
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(str));
            if (str4 != null && !str4.isEmpty()) {
                request.setMimeType(str4);
            }
            String cookie = CookieManager.getInstance().getCookie(str);
            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }
            if (str2 == null) {
                str2 = WebViewHolder.userAgent;
            }
            request.addRequestHeader("User-Agent", str2);
            request.setTitle(str3);
            request.setNotificationVisibility(1);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Zalo/" + sanitize(str3));
            ((DownloadManager) context.getSystemService(DownloadManager.class)).enqueue(request);
            WebViewHolder.toast(context.getString(R.string.download_started, str3));
        } catch (RuntimeException e) {
            Log.w("Zola", "enqueue download", e);
            WebViewHolder.toast(context.getString(R.string.download_failed));
        }
    }

    private static void fetchToStorage(final Context context, final String str, final String str2, final String str3, final String str4, final String str5) {
        WebViewHolder.toast(context.getString(R.string.download_started, str3));
        final String cookie = CookieManager.getInstance().getCookie(str);
        App.IO.execute(() -> {
            try {
                doFetchToStorage(str, cookie, str2, str4, context, str5, str3);
            } catch (Throwable th) {
                Log.w("Zola", "fetchToStorage", th);
            }
        });
    }

    static /* synthetic */ void doFetchToStorage(String str, String str2, String str3, String str4, Context context, String str5, String str6) throws Throwable {
        HttpURLConnection httpURLConnection = null;
        try {
            try {
                HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(str).openConnection();
                try {
                    httpURLConnection2.setInstanceFollowRedirects(true);
                    httpURLConnection2.setConnectTimeout(15000);
                    httpURLConnection2.setReadTimeout(30000);
                    if (str2 != null) {
                        httpURLConnection2.setRequestProperty("Cookie", str2);
                    }
                    if (str3 == null) {
                        str3 = WebViewHolder.userAgent;
                    }
                    httpURLConnection2.setRequestProperty("User-Agent", str3);
                    int responseCode = httpURLConnection2.getResponseCode();
                    if (responseCode / 100 != 2) {
                        throw new IOException("HTTP " + responseCode);
                    }
                    if (str4 == null || str4.isEmpty() || "application/octet-stream".equals(str4)) {
                        str4 = httpURLConnection2.getContentType();
                    }
                    WebViewHolder.toast(context.getString(R.string.download_saved_at, Storage.save(context, str5, str6, str4, httpURLConnection2.getInputStream())));
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                } catch (Exception e) {
                    Log.w("Zola", "fetchToStorage", e);
                    WebViewHolder.toast(context.getString(R.string.download_failed));
                } finally {
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                }
            } catch (Exception e2) {
                Log.w("Zola", "fetchToStorage", e2);
            }
        } catch (Throwable th2) {
            Log.w("Zola", "fetchToStorage", th2);
        }
    }

    static void saveBase64(Context context, String str, String str2, String str3) {
        String strSanitize = sanitize(str);
        String str4 = (str2 == null || str2.isEmpty()) ? "application/octet-stream" : str2;
        String strKindOf = Storage.kindOf(str2, strSanitize);
        if (strKindOf != null) {
            try {
                WebViewHolder.toast(context.getString(R.string.download_saved_at, Storage.save(context, strKindOf, strSanitize, str4, new ByteArrayInputStream(Base64.decode(str3, 0)))));
                return;
            } catch (Exception e) {
                Log.w("Zola", "saveBase64", e);
                WebViewHolder.toast(context.getString(R.string.download_failed));
                return;
            }
        }
        try {
            byte[] bArrDecode = Base64.decode(str3, 0);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver contentResolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put("_display_name", strSanitize);
                contentValues.put("mime_type", str4);
                contentValues.put("relative_path", Environment.DIRECTORY_DOWNLOADS + "/Zalo");
                contentValues.put("is_pending", (Integer) 1);
                Uri uriInsert = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uriInsert == null) {
                    throw new IOException("MediaStore insert failed");
                }
                OutputStream outputStreamOpenOutputStream = contentResolver.openOutputStream(uriInsert);
                try {
                    if (outputStreamOpenOutputStream == null) {
                        throw new IOException("openOutputStream failed");
                    }
                    outputStreamOpenOutputStream.write(bArrDecode);
                    if (outputStreamOpenOutputStream != null) {
                        outputStreamOpenOutputStream.close();
                    }
                    contentValues.clear();
                    contentValues.put("is_pending", (Integer) 0);
                    contentResolver.update(uriInsert, contentValues, null, null);
                } catch (Throwable th) {
                    if (outputStreamOpenOutputStream != null) {
                        try {
                            outputStreamOpenOutputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } else {
                if (!hasLegacyStoragePermission(context)) {
                    askStoragePermission(context);
                    return;
                }
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Zalo");
                if (!file.exists() && !file.mkdirs()) {
                    throw new IOException("mkdirs failed");
                }
                File fileUnique = unique(file, strSanitize);
                FileOutputStream fileOutputStream = new FileOutputStream(fileUnique);
                try {
                    fileOutputStream.write(bArrDecode);
                    fileOutputStream.close();
                    MediaScannerConnection.scanFile(context, new String[]{fileUnique.getAbsolutePath()}, new String[]{str4}, null);
                    strSanitize = fileUnique.getName();
                } catch (Throwable th3) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                    throw th3;
                }
            }
            WebViewHolder.toast(context.getString(R.string.download_saved, strSanitize));
        } catch (Exception e2) {
            Log.w("Zola", "saveBase64", e2);
            WebViewHolder.toast(context.getString(R.string.download_failed));
        }
    }

    private static boolean hasLegacyStoragePermission(Context context) {
        return context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    private static void askStoragePermission(Context context) {
        WebViewHolder.toast(context.getString(R.string.download_need_permission));
        App.MAIN.post(Downloads::lambda$askStoragePermission$1);
    }

    static /* synthetic */ void lambda$askStoragePermission$1() {
        MainActivity mainActivityHost = WebViewHolder.host();
        if (mainActivityHost != null) {
            mainActivityHost.requestStoragePermission();
        }
    }

    static String sanitize(String str) {
        String strTrim = str == null ? "" : str.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (strTrim.isEmpty() || strTrim.equals(".") || strTrim.equals("..")) {
            strTrim = "zalo-file";
        }
        return strTrim.length() > 120 ? strTrim.substring(strTrim.length() - 120) : strTrim;
    }

    static File unique(File file, String str) {
        File file2 = new File(file, str);
        if (!file2.exists()) {
            return file2;
        }
        int iLastIndexOf = str.lastIndexOf(46);
        String strSubstring = iLastIndexOf > 0 ? str.substring(0, iLastIndexOf) : str;
        String strSubstring2 = iLastIndexOf > 0 ? str.substring(iLastIndexOf) : "";
        for (int i = 1; i < 1000; i++) {
            File file3 = new File(file, strSubstring + " (" + i + ")" + strSubstring2);
            if (!file3.exists()) {
                return file3;
            }
        }
        return new File(file, strSubstring + "-" + System.currentTimeMillis() + strSubstring2);
    }
}
