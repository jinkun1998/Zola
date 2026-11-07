package com.zola.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class SettingsActivity extends Activity {
    static final String EXTRA_PAGE = "page";
    private static final String P_BUBBLES = "bubbles";
    private static final String P_CALLS = "calls";
    private static final String P_CONN = "conn";
    private static final String P_DISPLAY = "display";
    private static final String P_OTHER = "other";
    private static final String P_PERMS = "perms";
    private static final int REQ_RINGBACK = 78;
    private static final int REQ_RINGTONE = 77;

    private Button cacheButton;
    private LinearLayout list;
    private Button ringbackButton;
    private Button ringtoneButton;
    private TextView status;
    private Button storageButton;

    interface OnToggle {
        void changed(boolean z);
    }

    interface OnValue {
        void changed(int i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) {
            refreshStatus();
        }
        if (storageButton != null) {
            storageButton.setText(storageLabel());
        }
    }

    private void refreshStatus() {
        final String vName = "Zola " + versionName();
        final String perm = "Thông báo: " + ("granted".equals(Notifier.permissionState(this)) ? "đã cho phép" : "CHƯA cho phép");
        status.setText(vName + "\nĐang kiểm tra…");
        WebViewHolder.scriptStatus(str3 -> {
            String str4;
            String str5;
            if (str3 == null) {
                str5 = "Zalo chưa mở";
            } else if ("none".equals(str3)) {
                str5 = "Script trong trang Zalo: KHÔNG CHẠY";
            } else {
                int idx = str3.indexOf('|');
                String strSub = idx > 0 ? str3.substring(0, idx) : str3;
                if (str3.contains("\"enabled\":false")) {
                    str4 = "tắt";
                } else {
                    str4 = str3.contains("\"detected\":true") ? "đã nhận dạng bố cục Zalo" : "CHƯA nhận dạng được bố cục (bấm \"Sao chép cấu trúc giao diện\" và gửi đi)";
                }
                str5 = "Script trong trang Zalo: đang chạy (" + strSub + ")\nGiao diện điện thoại: " + str4;
            }
            status.setText(vName + "\n" + str5 + "\n" + perm);
        });
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFitsSystemWindows(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        list.setPadding(pad, pad, pad, pad);
        scrollView.addView(list);
        setContentView(scrollView);

        String page = getIntent().getStringExtra(EXTRA_PAGE);
        if (page != null && getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (P_PERMS.equals(page)) {
            setTitle("Cấp quyền");
            buildPermissions();
        } else if (P_CONN.equals(page)) {
            setTitle("Kết nối");
            buildConnection();
        } else if (P_CALLS.equals(page)) {
            setTitle("Cuộc gọi");
            buildCalls();
        } else if (P_DISPLAY.equals(page)) {
            setTitle("Giao diện");
            buildDisplay();
        } else if (P_BUBBLES.equals(page)) {
            setTitle("Bong bóng chat");
            buildBubbles();
        } else if (P_OTHER.equals(page)) {
            setTitle("Khác");
            buildOther();
        } else {
            buildMain();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buildMain() {
        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        status.setPadding(dp(12), dp(10), dp(12), dp(10));
        status.setBackgroundColor(0xfff1f3f4);
        list.addView(status);
        list.addView(new View(this), new LinearLayout.LayoutParams(1, dp(8)));

        navRow("Cấp quyền", "Thông báo, chạy nền không giới hạn pin, cuộc gọi khi khoá máy, lưu tệp", P_PERMS);
        navRow("Kết nối", "Chạy nền, tự chạy khi khởi động, nhận cuộc gọi đến, giữ kết nối khi khoá máy", P_CONN);
        navRow("Cuộc gọi", "Nhạc chuông, hồi chuông, ghi âm, thông số cuộc gọi", P_CALLS);
        navRow("Giao diện", "Giao diện điện thoại, thanh dock, bề rộng trang, cỡ chữ", P_DISPLAY);
        navRow("Bong bóng chat", "Tin nhắn mới hiện thành bong bóng nổi trên màn hình", P_BUBBLES);
        navRow("Khác", "Ảnh/video đã lưu, tải lại Zalo, đăng xuất", P_OTHER);
        note("Mẹo: nhấn giữ biểu tượng app để mở nhanh màn hình này.\nPhiên bản " + versionName());
    }

    private void navRow(String title, String desc, final String page) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(16);
        row.setPadding(dp(4), dp(14), dp(4), dp(14));
        TypedValue val = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, val, true);
        row.setBackgroundResource(val.resourceId);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(0xff1a1a1a);
        textCol.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        tvDesc.setTextColor(0xff6b7280);
        textCol.addView(tvDesc);

        row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26.0f);
        chevron.setTextColor(0xff9ca3af);
        chevron.setPadding(dp(12), 0, dp(4), 0);
        row.addView(chevron);

        row.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class).putExtra(EXTRA_PAGE, page)));
        list.addView(row, new LinearLayout.LayoutParams(-1, -2));

        View divider = new View(this);
        divider.setBackgroundColor(0xffe5e7eb);
        list.addView(divider, new LinearLayout.LayoutParams(-1, 1));
    }

    private void buildPermissions() {
        button("Cài đặt thông báo của app", this::openNotificationSettings);
        button("Cho phép chạy nền không giới hạn pin", () -> requestIgnoreBattery(this));
        if (isHuawei()) {
            button("Huawei: cho phép chạy nền (Quản lý khởi chạy)", this::openHuaweiLaunchManager);
        }
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
            button((nm == null || nm.canUseFullScreenIntent()) ? "Màn hình cuộc gọi đến khi khoá máy: đã cho phép" : "Cho phép hiện cuộc gọi đến toàn màn hình khi khoá máy", () -> safeStart(new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:" + getPackageName()))));
        }
        button(Settings.canDrawOverlays(this) ? "Hiển thị trên ứng dụng khác (bong bóng chat): đã cho phép" : "Cho phép hiển thị trên ứng dụng khác (bong bóng chat)", () -> safeStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        storageButton = button(storageLabel(), this::askStorage);
    }

    private void buildConnection() {
        toggle("Chạy nền để nhận tin nhắn", Prefs.KEEP_ALIVE, true, z -> {
            if (z) KeepAliveService.start(this); else KeepAliveService.stop(this);
        });
        toggle("Tự chạy khi khởi động máy", Prefs.START_ON_BOOT, true, null);
        toggle("Nhận cuộc gọi đến (thử nghiệm: báo với máy chủ Zalo app là Zalo PC — có thể làm thoát Zalo PC khác của tài khoản này)", Prefs.INCOMING_CALLS, true, null);
        toggle("Giữ kết nối khi khoá màn hình (để nhận cuộc gọi khi máy khoá — tốn pin hơn: máy bị đánh thức mỗi 2 phút. Nên bật thêm \"Cho phép chạy nền không giới hạn pin\" ở trang Cấp quyền)", Prefs.KEEP_ON_LOCK, false, z -> {
            if (z) {
                KeepAliveService.start(this);
                if (!isIgnoringBattery()) {
                    requestIgnoreBattery(this);
                }
            }
        });
        button("Tắt thông báo chạy nền (vẫn giữ kết nối)", this::openServiceChannelSettings);
    }

    private void buildCalls() {
        toggle("Hiện thông số cuộc gọi (ping, tốc độ tiếng / hình) ở góc màn hình gọi", Prefs.CALL_STATS, true, null);
        ringtoneButton = button(ringtoneLabel(), this::pickRingtone);
        ringbackButton = button(ringbackLabel(), this::pickRingback);
        dropdown("Tự động ghi âm cuộc gọi", new String[]{"Tắt", "Mọi cuộc gọi", "Với người lạ", "Với người trong danh bạ"}, Prefs.autoRecord(this), i -> Prefs.sp(this).edit().putInt(Prefs.AUTO_RECORD, i).apply());
        note("Ghi âm lưu tại " + Storage.base(this) + "/records" + (Storage.canWriteRoot(this) ? "" : " (cấp quyền ở trang Cấp quyền để lưu vào Bộ nhớ trong/Zola)") + ".");
        button("Sao chép nhật ký cuộc gọi (gửi khi báo lỗi gọi)", () -> {
            String dump = CallEvents.dump();
            ClipboardManager cm = (ClipboardManager) getSystemService(ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Zola call log", dump));
            Toast.makeText(this, dump.isEmpty() ? "Chưa có nhật ký cuộc gọi" : "Đã sao chép nhật ký cuộc gọi", Toast.LENGTH_SHORT).show();
        });
    }

    private void buildDisplay() {
        toggle("Giao diện điện thoại (danh sách chat → chạm để mở chat)", Prefs.MOBILE_UI, true, z -> WebViewHolder.applySettings(true));
        dropdown("Giao diện (khi bật giao diện điện thoại)", new String[]{"Native — vẽ bằng app (mặc định)", "WebView — giao diện của Zalo Web"}, !Prefs.nativeList(this) ? 1 : 0, i -> {
            Prefs.put(this, Prefs.NATIVE_LIST, i == 0);
            WebViewHolder.applySettings(true);
        });
        slider("Độ đậm nền thanh dock", Prefs.dockOpacity(this), 100, "%", i -> {
            Prefs.put(this, Prefs.DOCK_OPACITY, i);
            WebViewHolder.applyDockSettings();
        });
        slider("Độ cao thanh dock (cách đáy màn hình)", Prefs.dockBottom(this), 80, "px", i -> {
            Prefs.put(this, Prefs.DOCK_BOTTOM, i);
            WebViewHolder.applyDockSettings();
        });
        toggle("Thanh dock: nút zCloud", Prefs.DOCK_CLOUD, false, z -> WebViewHolder.applyDockButtons());
        toggle("Thanh dock: nút Công cụ", Prefs.DOCK_TOOLS, false, z -> WebViewHolder.applyDockButtons());
        toggle("Tự ẩn thanh dock và thanh tìm kiếm khi cuộn (cuộn lên để hiện lại)", Prefs.DOCK_AUTO_HIDE, true, z -> WebViewHolder.applyDockSettings());
        choice("Bề rộng trang Zalo (khi tắt giao diện điện thoại)", new String[]{"Vừa màn hình điện thoại", "800 px", "980 px (như \"Trang web cho máy tính\")", "1280 px"}, new int[]{0, 800, 980, 1280}, Prefs.VIEWPORT_WIDTH, Prefs.viewportWidth(this), true);
        choice("Cỡ chữ", new String[]{"Theo cỡ chữ của máy (" + Math.round(getResources().getConfiguration().fontScale * 100.0f) + "%)", "85%", "100%", "115%", "130%"}, new int[]{0, 85, 100, 115, 130}, Prefs.TEXT_ZOOM, Prefs.textZoomSetting(this), false);
    }

    private void buildBubbles() {
        toggle("Hiện tin nhắn mới dạng bong bóng chat", P_BUBBLES, true, null);
        if (Build.VERSION.SDK_INT < 30) {
            note("Bong bóng chat cần Android 11 trở lên.");
            return;
        }
        toggle("Vuốt app về màn hình chính khi đang chat: tự hiện bong bóng của cuộc trò chuyện đó", Prefs.BUBBLE_ON_HOME, true, null);
        dropdown("Hiện bong bóng cho", new String[]{"Mọi cuộc trò chuyện", "Chỉ chat 1-1", "Chỉ nhóm"}, Prefs.bubbleScope(this), i -> Prefs.put(this, Prefs.BUBBLE_SCOPE, i));
        toggle("Tự mở khung chat khi có tin nhắn mới (không chỉ hiện bong bóng thu gọn)", Prefs.BUBBLE_AUTO_EXPAND, false, null);
        toggle("Ẩn thông báo trên thanh trạng thái khi đã hiện bong bóng", Prefs.BUBBLE_HIDE_NOTIF, false, null);

        NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
        boolean allowed = true;
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= 31) {
                allowed = nm.getBubblePreference() != 0;
            } else {
                allowed = nm.areBubblesAllowed();
            }
        }
        button(allowed ? "Bong bóng của app: đã cho phép (chạm để đổi)" : "Cho phép bong bóng của app trong cài đặt Android", () -> safeStart(new Intent("android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE", getPackageName())));
        button(Settings.canDrawOverlays(this) ? "Hiển thị trên ứng dụng khác: đã cho phép (chạm để đổi)" : "Cho phép hiển thị trên ứng dụng khác", () -> safeStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        note("Android chỉ hiện bong bóng khi đã cho phép bong bóng cho app (nút trên). Chạm bong bóng để mở khung chat, kéo bong bóng xuống dấu X để đóng. Cài đặt áp dụng cho tin nhắn mới.");
    }

    private void buildOther() {
        button("Xóa ảnh/video đã lưu", () -> {
            new AlertDialog.Builder(this)
                .setMessage("Xóa ảnh và video tin nhắn đã lưu trong máy? Lần sau xem lại sẽ phải tải lại.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    App.IO.execute(() -> {
                        MediaCache.clear(this);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                            updateCacheLabel();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
        });
        cacheButton = (Button) list.getChildAt(list.getChildCount() - 1);
        updateCacheLabel();

        button("Tải lại Zalo", () -> {
            WebViewHolder.reload();
            Toast.makeText(this, "Đang tải lại", Toast.LENGTH_SHORT).show();
        });

        if ((getApplicationInfo().flags & 2) != 0) {
            button("Sao chép cấu trúc giao diện (gửi để sửa lỗi hiển thị)", this::copyLayout);
            button("Sao chép log debug (gửi khi gặp lỗi)", this::copyDebugLog);
            button("Xóa log debug", () -> {
                DiagLog.clear();
                Toast.makeText(this, "Đã xóa nhật ký", Toast.LENGTH_SHORT).show();
            });
        }

        button("Đăng xuất & xóa dữ liệu Zalo trong app", this::confirmReset);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
            .setTitle("Đăng xuất Zalo?")
            .setMessage("Xóa phiên đăng nhập, bộ nhớ đệm và dữ liệu Zalo Web trong app. Tin nhắn vẫn còn trên điện thoại chính.")
            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                WebViewHolder.resetAll(this, () -> {
                    Toast.makeText(this, "Đã xóa. Quét mã QR để đăng nhập lại.", Toast.LENGTH_LONG).show();
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void copyLayout() {
        WebViewHolder.dumpLayout(str -> {
            if (str == null || str.isEmpty()) {
                Toast.makeText(this, "Zalo chưa tải xong, mở app rồi thử lại", Toast.LENGTH_SHORT).show();
            } else {
                ClipboardManager cm = (ClipboardManager) getSystemService(ClipboardManager.class);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Zola layout", str));
                Toast.makeText(this, "Đã sao chép, dán vào cuộc trò chuyện để gửi", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void copyDebugLog() {
        WebViewHolder.debugState(str -> {
            StringBuilder sb = new StringBuilder("=== Zola debug log ===\nsettings: keepAlive=");
            PowerManager pm = (PowerManager) getSystemService(PowerManager.class);
            sb.append(Prefs.keepAlive(this)).append(" startOnBoot=").append(Prefs.startOnBoot(this))
              .append(" mobileUi=").append(Prefs.mobileUi(this)).append(" viewport=").append(Prefs.viewportWidth(this))
              .append(" textZoom=").append(Prefs.textZoom(this)).append(Prefs.textZoomSetting(this) == 0 ? "(device)" : "")
              .append(" dock(opacity=").append(Prefs.dockOpacity(this)).append(" bottom=").append(Prefs.dockBottom(this))
              .append(" autoHide=").append(Prefs.dockAutoHide(this)).append(")\npermissions: notifications=");
            sb.append(Notifier.permissionState(this)).append(" batteryUnrestricted=")
              .append(pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()))
              .append(" nightMode=").append((getResources().getConfiguration().uiMode & 48) == 32)
              .append("\npage: ");
            sb.append(str != null ? str : "zalo not open").append('\n');
            sb.append(DiagLog.dump(this));
            if (CrashLog.hasPending(this)) {
                sb.append("--- last crash\n").append(CrashLog.read(this)).append('\n');
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Zola debug log", sb.toString()));
            Toast.makeText(this, "Đã sao chép log debug, dán vào tin nhắn để gửi", Toast.LENGTH_LONG).show();
        });
    }

    private void updateCacheLabel() {
        App.IO.execute(() -> {
            long size = MediaCache.size(this);
            String label = size < 1048576 ? (size / 1024) + " KB" : String.format(Locale.getDefault(), "%.1f MB", (size / 1024.0f) / 1024.0f);
            runOnUiThread(() -> {
                if (cacheButton != null) {
                    cacheButton.setText("Xóa ảnh/video đã lưu (" + label + ")");
                }
            });
        });
    }

    private void toggle(String str, final String key, boolean def, final OnToggle onToggle) {
        Switch sw = new Switch(this);
        sw.setText(str);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setChecked(Prefs.bool(this, key, def));
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            Prefs.put(this, key, isChecked);
            if (onToggle != null) onToggle.changed(isChecked);
        });
        list.addView(sw, new LinearLayout.LayoutParams(-1, -2));
    }

    private void choice(String str, String[] labels, final int[] values, final String key, int current, final boolean reload) {
        TextView tv = new TextView(this);
        tv.setText(str);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        tv.setPadding(0, dp(10), 0, dp(4));
        list.addView(tv);

        RadioGroup group = new RadioGroup(this);
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(i + 1000);
            rb.setText(labels[i]);
            rb.setChecked(values[i] == current);
            group.addView(rb);
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            int idx = checkedId - 1000;
            if (idx >= 0 && idx < values.length) {
                Prefs.put(this, key, values[idx]);
                WebViewHolder.applySettings(reload);
            }
        });
        list.addView(group);
    }

    private void dropdown(String str, String[] items, int current, OnValue onValue) {
        TextView tv = new TextView(this);
        tv.setText(str);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        tv.setPadding(0, dp(10), 0, dp(4));
        list.addView(tv);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(current, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int last = current;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != last) {
                    last = position;
                    onValue.changed(position);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        list.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
    }

    private void slider(final String str, int current, int max, final String unit, final OnValue onValue) {
        final TextView tv = new TextView(this);
        tv.setText(str + ": " + current + unit);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        tv.setPadding(0, dp(10), 0, dp(4));
        list.addView(tv);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max);
        seekBar.setProgress(current);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tv.setText(str + ": " + progress + unit);
                    onValue.changed(progress);
                }
            }
        });
        list.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
    }

    private String ringtoneLabel() {
        Uri uri = Prefs.ringtone(this);
        String title;
        if (uri == null) {
            title = "Im lặng (chỉ rung)";
        } else if (Prefs.hasCustomRingtone(this)) {
            Ringtone r = RingtoneManager.getRingtone(this, uri);
            title = r != null ? r.getTitle(this) : uri.getLastPathSegment();
        } else {
            title = "Mặc định của máy";
        }
        return "Nhạc chuông cuộc gọi đến: " + title;
    }

    private void pickRingtone() {
        Uri uri = Prefs.ringtone(this);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Nhạc chuông cuộc gọi đến")
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_RINGTONE_URI);
        if (!Prefs.hasCustomRingtone(this)) {
            uri = Settings.System.DEFAULT_RINGTONE_URI;
        }
        try {
            startActivityForResult(intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri), REQ_RINGTONE);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, "Máy không có trình chọn nhạc chuông", Toast.LENGTH_SHORT).show();
        }
    }

    private String ringbackLabel() {
        Uri uri = Prefs.ringback(this);
        String title;
        if (uri == null) {
            title = "Im lặng";
        } else if (Prefs.hasCustomRingback(this)) {
            Ringtone r = RingtoneManager.getRingtone(this, uri);
            title = r != null ? r.getTitle(this) : uri.getLastPathSegment();
        } else {
            title = "Mặc định của app";
        }
        return "Hồi chuông khi gọi đi: " + title;
    }

    private void pickRingback() {
        Uri defUri = Prefs.defaultRingback(this);
        Uri uri = Prefs.ringback(this);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Hồi chuông khi gọi đi")
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defUri);
        try {
            startActivityForResult(intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri), REQ_RINGBACK);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, "Máy không có trình chọn nhạc chuông", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (requestCode == REQ_RINGTONE) {
            Prefs.setRingtone(this, uri);
            if (ringtoneButton != null) ringtoneButton.setText(ringtoneLabel());
        } else if (requestCode == REQ_RINGBACK) {
            Prefs.setRingback(this, uri);
            if (ringbackButton != null) ringbackButton.setText(ringbackLabel());
        }
    }

    private String storageLabel() {
        if (Storage.canWriteRoot(this)) {
            return "Ghi âm, ảnh, video lưu tại: Bộ nhớ trong/Zola";
        }
        return Build.VERSION.SDK_INT == 29 ? "Ghi âm, ảnh, video lưu tại: Download/Zola (Android 10)" : "Cho phép lưu vào Bộ nhớ trong/Zola (đang lưu ở " + Storage.base(this) + ")";
    }

    private void askStorage() {
        if (Storage.canWriteRoot(this) || Build.VERSION.SDK_INT == 29) return;
        if (Build.VERSION.SDK_INT >= 30) {
            safeStart(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
        } else {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 79);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 79 && storageButton != null) {
            storageButton.setText(storageLabel());
        }
    }

    private boolean isIgnoringBattery() {
        PowerManager pm = (PowerManager) getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    static void requestIgnoreBattery(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.getPackageName())));
        } catch (ActivityNotFoundException unused) {
            try {
                context.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (ActivityNotFoundException unused2) {}
        }
    }

    private static boolean isHuawei() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        return m.contains("huawei") || m.contains("honor");
    }

    private void openHuaweiLaunchManager() {
        String[][] acts = {
            {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
            {"com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"}
        };
        for (String[] act : acts) {
            try {
                startActivity(new Intent().setClassName(act[0], act[1]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Toast.makeText(this, "Tìm Zola, tắt \"Quản lý tự động\", bật cả Tự khởi chạy, Khởi chạy phụ, Chạy nền", Toast.LENGTH_LONG).show();
                return;
            } catch (RuntimeException unused) {}
        }
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    private void openNotificationSettings() {
        safeStart(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    }

    private void openServiceChannelSettings() {
        safeStart(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
            .putExtra(Settings.EXTRA_CHANNEL_ID, App.CH_SERVICE));
    }

    private void safeStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, "Máy không hỗ trợ màn hình này", Toast.LENGTH_SHORT).show();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception unused) {
            return "?";
        }
    }

    private Button button(String str, final Runnable runnable) {
        Button btn = new Button(this);
        btn.setText(str);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> runnable.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        list.addView(btn, lp);
        return btn;
    }

    private void note(String str) {
        TextView tv = new TextView(this);
        tv.setText(str);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        tv.setTextColor(0xff666666);
        tv.setPadding(0, dp(24), 0, 0);
        list.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
