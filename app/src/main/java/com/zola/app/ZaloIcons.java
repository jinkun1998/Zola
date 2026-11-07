package com.zola.app;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

final class ZaloIcons {
    private static JSONObject codes;
    private static Typeface font;

    private ZaloIcons() {
    }

    static synchronized Typeface font(Context context) {
        if (font == null) {
            try {
                font = context.getResources().getFont(R.font.zalo_icons);
            } catch (RuntimeException e) {
                Log.w("Zola", "zalo icon font", e);
                font = Typeface.DEFAULT;
            }
        }
        return font;
    }

    static synchronized String glyph(Context context, String str) {
        if (codes == null) {
            try (InputStream inputStream = context.getAssets().open("zalo_icons.json")) {
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                int total = 0;
                while (total < size) {
                    int r = inputStream.read(buffer, total, size - total);
                    if (r < 0) break;
                    total += r;
                }
                codes = new JSONObject(new String(buffer, 0, total, StandardCharsets.UTF_8));
            } catch (Exception e) {
                Log.w("Zola", "zalo icon map", e);
                codes = new JSONObject();
            }
        }
        String hex = codes.optString(str);
        if (hex.isEmpty()) return "";
        return new String(Character.toChars(Integer.parseInt(hex, 16)));
    }

    static TextView view(Context context, String str, float sizeSp, int color) {
        TextView textView = new TextView(context);
        textView.setTypeface(font(context));
        textView.setText(glyph(context, str));
        textView.setTextSize(1, sizeSp);
        textView.setTextColor(color);
        textView.setGravity(17);
        textView.setIncludeFontPadding(false);
        return textView;
    }
}
