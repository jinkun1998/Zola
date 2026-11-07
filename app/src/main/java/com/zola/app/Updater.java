package com.zola.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Updater {
    private static final String API_LATEST = "https://api.github.com/repos/jinkun1998/Zola/releases/latest";

    private Updater() {}

    public static void check(Activity activity, boolean manual) {
        if (activity == null || activity.isFinishing()) return;
        if (manual) {
            Toast.makeText(activity, "Đang kiểm tra cập nhật...", Toast.LENGTH_SHORT).show();
        }

        App.IO.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(API_LATEST);
                int code = conn.getResponseCode();
                if (code == 404) {
                    if (manual) toast(activity, "Chưa có bản phát hành trên GitHub");
                    return;
                }
                if (code != 200) {
                    if (manual) toast(activity, "Lỗi kiểm tra cập nhật (HTTP " + code + ")");
                    return;
                }

                String jsonStr;
                try (InputStream in = conn.getInputStream()) {
                    jsonStr = readStream(in);
                }
                JSONObject release = new JSONObject(jsonStr);
                String tagName = release.optString("tag_name", "");
                String curVer = currentVersion(activity);

                if (!isNewer(tagName, curVer)) {
                    if (manual) toast(activity, "Bạn đang dùng phiên bản mới nhất (" + curVer + ")");
                    return;
                }

                JSONArray assets = release.optJSONArray("assets");
                String apkUrl = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset != null) {
                            String name = asset.optString("name", "");
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url");
                                break;
                            }
                        }
                    }
                }

                if (apkUrl == null || apkUrl.isEmpty()) {
                    if (manual) toast(activity, "Bản mới " + tagName + " chưa có file APK đính kèm");
                    return;
                }

                final String finalUrl = apkUrl;
                final String body = release.optString("body", "");
                activity.runOnUiThread(() -> showUpdateDialog(activity, tagName, body, finalUrl));
            } catch (Exception e) {
                if (manual) toast(activity, "Không thể kiểm tra cập nhật: " + e.getMessage());
            }
        });
    }

    private static void showUpdateDialog(Activity activity, String tagName, String body, String apkUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        String msg = (body.isEmpty() ? "" : body + "\n\n") + "Tải về và cài đặt ngay?";
        new AlertDialog.Builder(activity)
                .setTitle("Bản cập nhật mới: " + tagName)
                .setMessage(msg)
                .setPositiveButton("Cập nhật", (d, w) -> downloadAndInstall(activity, apkUrl, tagName))
                .setNegativeButton("Để sau", null)
                .show();
    }

    private static void downloadAndInstall(Activity activity, String apkUrl, String tagName) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        ProgressDialog progress = new ProgressDialog(activity);
        progress.setTitle("Đang tải Zola " + tagName);
        progress.setMessage("Vui lòng đợi...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        App.IO.execute(() -> {
            File apkFile = null;
            try {
                HttpURLConnection conn = openConnection(apkUrl);
                int totalLen = conn.getContentLength();

                File dir = ShareFiles.dir(activity);
                dir.mkdirs();
                apkFile = new File(dir, "zola-update.apk");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(apkFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    long done = 0;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        done += read;
                        if (totalLen > 0) {
                            int pct = (int) (done * 100 / totalLen);
                            activity.runOnUiThread(() -> progress.setProgress(pct));
                        }
                    }
                    out.flush();
                }

                final File fileToInstall = apkFile;
                activity.runOnUiThread(() -> {
                    try { progress.dismiss(); } catch (Exception ignored) {}
                    installApk(activity, fileToInstall);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    try { progress.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(activity, "Lỗi tải cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public static void installApk(Context context, File apkFile) {
        if (context == null || apkFile == null || !apkFile.exists()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(context, "Vui lòng cho phép Zola cài đặt ứng dụng", Toast.LENGTH_LONG).show();
            Intent permIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName()));
            if (!(context instanceof Activity)) permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(permIntent);
        }

        Uri apkUri = Uri.parse("content://" + ShareFiles.AUTHORITY + "/" + apkFile.getName());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setClipData(ClipData.newRawUri(apkFile.getName(), apkUri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở bộ cài đặt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    static boolean isNewer(String remote, String current) {
        if (remote == null || current == null) return false;
        String r = remote.trim().replaceAll("^[vV]", "");
        String c = current.trim().replaceAll("^[vV]", "");
        String[] rParts = r.split("[.\\-_]");
        String[] cParts = c.split("[.\\-_]");
        int max = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < max; i++) {
            int rVal = i < rParts.length ? parseNum(rParts[i]) : 0;
            int cVal = i < cParts.length ? parseNum(cParts[i]) : 0;
            if (rVal > cVal) return true;
            if (rVal < cVal) return false;
        }
        return false;
    }

    private static int parseNum(String s) {
        try {
            String digits = s.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String currentVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private static void toast(Activity activity, String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[2048];
        int n;
        while ((n = is.read(b)) != -1) bos.write(b, 0, n);
        return bos.toString("UTF-8");
    }

    private static HttpURLConnection openConnection(String urlStr) throws IOException {
        String cur = urlStr;
        for (int i = 0; i < 5; i++) {
            URL url = new URL(cur);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Zola-Updater");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                if (loc != null) {
                    conn.disconnect();
                    cur = loc;
                    continue;
                }
            }
            return conn;
        }
        throw new IOException("Too many redirects");
    }
}
