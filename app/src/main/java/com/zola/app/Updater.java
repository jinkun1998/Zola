package com.zola.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Updater {
    private static final String API_LATEST = "https://api.github.com/repos/jinkun1998/Zola/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/jinkun1998/Zola/releases";

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

                String url = release.optString("html_url", RELEASES_PAGE);
                String body = release.optString("body", "");
                activity.runOnUiThread(() -> showUpdateDialog(activity, tagName, body, url));
            } catch (Exception e) {
                if (manual) toast(activity, "Không thể kiểm tra cập nhật: " + e.getMessage());
            }
        });
    }

    private static void showUpdateDialog(Activity activity, String tagName, String body, String url) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        String msg = (body.isEmpty() ? "" : body + "\n\n") + "Mở GitHub để tải bản cập nhật?";
        new AlertDialog.Builder(activity)
                .setTitle("Bản cập nhật mới: " + tagName)
                .setMessage(msg)
                .setPositiveButton("Mở GitHub", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    activity.startActivity(intent);
                })
                .setNegativeButton("Để sau", null)
                .show();
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
