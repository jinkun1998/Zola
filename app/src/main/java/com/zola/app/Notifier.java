package com.zola.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class Notifier {
    static final int CALL_ID = 2;
    private static final int ZALO_BLUE = -16750337;
    private static final Pattern CALL_TEXT = Pattern.compile("cuộc gọi|đang gọi|gọi video|gọi thoại|gọi đến|incoming call|is calling|calling you", 66);
    private static final Map<String, Integer> idByJsId = new ConcurrentHashMap();
    private static final Set<String> cancelledCalls = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final AtomicInteger seq = new AtomicInteger(1000);
    private static final Map<String, Boolean> shownConv = new ConcurrentHashMap();
    private static final Map<String, String[]> convInfo = new ConcurrentHashMap();
    private static final Map<String, Boolean> groupConv = new ConcurrentHashMap();
    private static final Map<String, Boolean> lastUnread = new ConcurrentHashMap();

    private Notifier() {
    }

    static NotificationManager nm(Context context) {
        return (NotificationManager) context.getSystemService(NotificationManager.class);
    }

    static String permissionState(Context context) {
        if (nm(context).areNotificationsEnabled()) {
            return "granted";
        }
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0 && !Prefs.bool(context, "notif_asked", false)) {
            return "default";
        }
        return "denied";
    }

    static void show(final Context context, final String str, String str2, String str3, final String str4, final String str5, final boolean z) {
        int iNextId;
        final String str6 = str2 == null ? "" : str2;
        final String str7 = str3 == null ? "" : str3;
        final boolean z2 = Prefs.callAlerts(context) && CALL_TEXT.matcher(new StringBuilder().append(str6).append("\n").append(str7).toString()).find();
        if (z2) {
            iNextId = 2;
        } else {
            iNextId = (str4 == null || str4.isEmpty()) ? nextId() : idForTag(str4);
        }
        final int i = iNextId;
        if (!z2 && str4 != null && !str4.isEmpty()) {
            shownConv.put(str4, Boolean.valueOf(Boolean.TRUE.equals(lastUnread.get(str4))));
        }
        if (str != null) {
            Map<String, Integer> map = idByJsId;
            if (map.size() > 500) {
                map.clear();
            }
            map.put(str, Integer.valueOf(i));
        }
        DiagLog.add("android notification posted" + (z2 ? " (call)" : "") + (MainActivity.visible ? " while app open" : ""));
        App.IO.execute(() -> Notifier.doShowNotification(str5, z2, str, context, i, str6, str7, z, str4));
    }

    static /* synthetic */ void doShowNotification(String str, boolean z, String str2, Context context, int i, String str3, String str4, boolean z2, String str5) {
        Bitmap bitmapLoadIcon = loadIcon(str);
        if (z) {
            if ((str2 == null || !cancelledCalls.remove(str2)) && !MainActivity.visible) {
                post(context, i, buildCall(context, str3, str4, bitmapLoadIcon, true));
                return;
            }
            return;
        }
        post(context, i, buildMessage(context, str2, str3, str4, bitmapLoadIcon, z2, i, str5));
    }

    static void cancel(Context context, String str) {
        Integer numRemove;
        if (str == null || (numRemove = idByJsId.remove(str)) == null || numRemove.intValue() != 2) {
            return;
        }
        Set<String> set = cancelledCalls;
        if (set.size() > 100) {
            set.clear();
        }
        set.add(str);
        cancelCallAlert(context);
    }

    static void showRingAlert(Context context) {
        if (!Prefs.callAlerts(context) || MainActivity.visible) {
            return;
        }
        post(context, 2, buildCall(context, context.getString(R.string.call_incoming), context.getString(R.string.call_open_hint), null, false));
    }

    static void cancelCallAlert(Context context) {
        nm(context).cancel(2);
    }

    private static int idForTag(String str) {
        return (str.hashCode() & 1073741823) | 1073741824;
    }

    static void clearConversation(Context context, String str) {
        if (str == null || str.isEmpty()) {
            return;
        }
        final int iIdForTag = idForTag(str);
        cancelKeepBubble(context, iIdForTag);
        idByJsId.values().removeIf(obj -> Notifier.matchesConversationId(iIdForTag, (Integer) obj));
        shownConv.remove(str);
    }

    static /* synthetic */ boolean matchesConversationId(int i, Integer num) {
        return num.intValue() == i;
    }

    private static void cancelKeepBubble(Context context, int i) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                for (StatusBarNotification statusBarNotification : nm(context).getActiveNotifications()) {
                    if (statusBarNotification.getId() == i) {
                        Notification notification = statusBarNotification.getNotification();
                        Notification.BubbleMetadata bubbleMetadata = notification.getBubbleMetadata();
                        if (bubbleMetadata == null || (notification.flags & 4096) == 0) {
                            break;
                        } else {
                            if (bubbleMetadata.isNotificationSuppressed()) {
                                return;
                            }
                            post(context, i, Notification.Builder.recoverBuilder(context, notification).setOnlyAlertOnce(true).setBubbleMetadata(copyBubble(bubbleMetadata, true)).build());
                            return;
                        }
                    }
                }
            } catch (RuntimeException e) {
                Log.w("Zola", "keep bubble", e);
            }
        }
        nm(context).cancel(i);
    }

    private static Notification.BubbleMetadata copyBubble(Notification.BubbleMetadata bubbleMetadata, boolean z) {
        Notification.BubbleMetadata.Builder builder;
        if (bubbleMetadata.getShortcutId() != null) {
            builder = new Notification.BubbleMetadata.Builder(bubbleMetadata.getShortcutId());
        } else {
            builder = new Notification.BubbleMetadata.Builder(bubbleMetadata.getIntent(), bubbleMetadata.getIcon());
        }
        if (bubbleMetadata.getDesiredHeight() > 0) {
            builder.setDesiredHeight(bubbleMetadata.getDesiredHeight());
        }
        return builder.setAutoExpandBubble(false).setSuppressNotification(z).build();
    }

    static void syncRead(Context context, String str) {
        if (str == null) {
            return;
        }
        try {
            JSONArray jSONArray = new JSONArray(str);
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strOptString = jSONObjectOptJSONObject.optString("id");
                    boolean z = jSONObjectOptJSONObject.optInt("u") > 0 || jSONObjectOptJSONObject.optInt("at") > 0 || jSONObjectOptJSONObject.optInt("ud") > 0;
                    lastUnread.put(strOptString, Boolean.valueOf(z));
                    groupConv.put(strOptString, Boolean.valueOf(jSONObjectOptJSONObject.optBoolean("g")));
                    convInfo.put(strOptString, new String[]{jSONObjectOptJSONObject.optString("n"), jSONObjectOptJSONObject.optString("a"), jSONObjectOptJSONObject.optString("m")});
                    Map<String, Boolean> map = shownConv;
                    Boolean bool = map.get(strOptString);
                    if (bool != null) {
                        if (z) {
                            if (!bool.booleanValue()) {
                                map.put(strOptString, true);
                            }
                        } else if (bool.booleanValue()) {
                            map.remove(strOptString);
                            final int iIdForTag = idForTag(strOptString);
                            cancelKeepBubble(context, iIdForTag);
                            idByJsId.values().removeIf(obj -> Notifier.matchesReadSyncId(iIdForTag, (Integer) obj));
                            DiagLog.add("notification cleared: conversation read on another device");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w("Zola", "syncRead", e);
        }
    }

    static /* synthetic */ boolean matchesReadSyncId(int i, Integer num) {
        return num.intValue() == i;
    }

    static void clearMessages(Context context) {
        try {
            for (StatusBarNotification statusBarNotification : nm(context).getActiveNotifications()) {
                int id = statusBarNotification.getId();
                if (id != 1 && id != 3 && id != 2) {
                    nm(context).cancel(statusBarNotification.getTag(), id);
                }
            }
        } catch (RuntimeException e) {
            Log.w("Zola", "clearMessages", e);
        }
        idByJsId.values().removeIf(obj -> Notifier.isMessageId((Integer) obj));
        shownConv.clear();
    }

    static /* synthetic */ boolean isMessageId(Integer num) {
        return num.intValue() != 2;
    }

    static void clearAll(Context context) {
        clearMessages(context);
        cancelCallAlert(context);
    }

    private static Notification buildMessage(Context context, String str, String str2, String str3, Bitmap bitmap, boolean z, int i, String str4) {
        Notification.Builder contentIntent = new Notification.Builder(context, z ? "messages_silent" : "messages").setSmallIcon(R.drawable.ic_stat_message).setColor(ZALO_BLUE).setContentTitle(str2.isEmpty() ? context.getString(R.string.app_name) : str2).setContentText(str3).setStyle(new Notification.BigTextStyle().bigText(str3)).setCategory(NotificationCompat.CATEGORY_MESSAGE).setShowWhen(true).setWhen(System.currentTimeMillis()).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context, i, new Intent(context, (Class<?>) MainActivity.class).addFlags(268435456).putExtra("zw_click_id", str), 201326592));
        if (bitmap != null) {
            contentIntent.setLargeIcon(bitmap);
        }
        if (Build.VERSION.SDK_INT >= 30 && wantBubble(context, str4)) {
            try {
                addBubble(context, contentIntent, str4, str2.isEmpty() ? context.getString(R.string.app_name) : str2, str3, bitmap, i, Prefs.bubbleAutoExpand(context), Prefs.bubbleHideNotif(context));
            } catch (RuntimeException e) {
                Log.w("Zola", "bubble", e);
            }
        }
        return contentIntent.build();
    }

    private static boolean wantBubble(Context context, String str) {
        return false;
    }

    private static void addBubble(Context context, Notification.Builder builder, String str, String str2, String str3, Bitmap bitmap, int i, boolean z, boolean z2) {
        Icon iconCreateWithAdaptiveBitmap = Icon.createWithAdaptiveBitmap(adaptive(bitmap, str2));
        Person personBuild = new Person.Builder().setName(str2).setIcon(iconCreateWithAdaptiveBitmap).setKey(str).setImportant(true).build();
        builder.setStyle(new Notification.MessagingStyle(new Person.Builder().setName("Bạn").build()).addMessage(str3, System.currentTimeMillis(), personBuild));
        String str4 = "conv_" + str;
        Intent intent = new Intent(context, MainActivity.class).putExtra("zw_conv", str);
        ((ShortcutManager) context.getSystemService(ShortcutManager.class)).pushDynamicShortcut(new ShortcutInfo.Builder(context, str4).setShortLabel(str2).setLongLived(true).setIcon(iconCreateWithAdaptiveBitmap).setPerson(personBuild).setIntent(new Intent(intent).setAction("android.intent.action.VIEW")).setCategories(Collections.singleton("com.zola.app.category.CONVERSATION")).build());
        builder.setShortcutId(str4);
        builder.setBubbleMetadata(new Notification.BubbleMetadata.Builder(PendingIntent.getActivity(context, i, intent, 167772160), iconCreateWithAdaptiveBitmap).setDesiredHeight(640).setAutoExpandBubble(z).setSuppressNotification(z2).build());
    }

    static void bubbleForConversation(final Context context, final String str) {
        if (Prefs.bubbleOnHome(context) && wantBubble(context, str) && Build.VERSION.SDK_INT >= 30) {
            String[] strArr = convInfo.get(str);
            final String string = (strArr == null || strArr[0].isEmpty()) ? context.getString(R.string.app_name) : strArr[0];
            final String str2 = strArr != null ? strArr[1] : null;
            final String str3 = (strArr == null || strArr[2].isEmpty()) ? string : strArr[2];
            final int iIdForTag = idForTag(str);
            App.IO.execute(() -> Notifier.doBubbleForConversation(str2, context, str, iIdForTag, string, str3));
        }
    }

    static /* synthetic */ void doBubbleForConversation(String str, Context context, String str2, int i, String str3, String str4) {
        Bitmap bitmapLoadIcon = loadIcon(str);
        Notification.Builder contentIntent = new Notification.Builder(context, "chat_bubble").setSmallIcon(R.drawable.ic_stat_message).setColor(ZALO_BLUE).setContentTitle(str3).setContentText(str4).setCategory(NotificationCompat.CATEGORY_MESSAGE).setOnlyAlertOnce(true).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context, i, new Intent(context, (Class<?>) MainActivity.class).addFlags(268435456).putExtra("zw_conv", str2), 201326592));
        if (bitmapLoadIcon != null) {
            contentIntent.setLargeIcon(bitmapLoadIcon);
        }
        try {
            addBubble(context, contentIntent, str2, str3, str4, bitmapLoadIcon, i, false, true);
            post(context, i, contentIntent.build());
            DiagLog.add("bubble for open conversation (app to home)");
        } catch (RuntimeException e) {
            Log.w("Zola", "home bubble", e);
        }
    }

    private static Bitmap adaptive(Bitmap bitmap, String str) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.drawColor(ZALO_BLUE);
        if (bitmap != null) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, 216, 216, true), 0.0f, 0.0f, (Paint) null);
        } else {
            Paint paint = new Paint(1);
            paint.setColor(-1);
            paint.setTextSize(64.0f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            String strTrim = (str == null || str.trim().isEmpty()) ? "Z" : str.trim();
            String upperCase = strTrim.substring(0, strTrim.offsetByCodePoints(0, 1)).toUpperCase();
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float f = 216 / 2.0f;
            canvas.drawText(upperCase, f, f - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), paint);
        }
        return bitmapCreateBitmap;
    }

    private static Notification buildCall(Context context, String str, String str2, Bitmap bitmap, boolean z) {
        PendingIntent activity = PendingIntent.getActivity(context, 2, new Intent(context, (Class<?>) MainActivity.class).addFlags(268435456).putExtra("zw_call", true), 201326592);
        Notification.Builder color = new Notification.Builder(context, z ? "calls" : "calls_quiet").setSmallIcon(R.drawable.ic_stat_call).setColor(ZALO_BLUE);
        if (str.isEmpty()) {
            str = context.getString(R.string.call_incoming);
        }
        Notification.Builder contentTitle = color.setContentTitle(str);
        if (str2.isEmpty()) {
            str2 = context.getString(R.string.call_open_hint);
        }
        Notification.Builder builderAddAction = contentTitle.setContentText(str2).setCategory(NotificationCompat.CATEGORY_CALL).setVisibility(1).setOngoing(true).setAutoCancel(true).setTimeoutAfter(60000L).setContentIntent(activity).setFullScreenIntent(activity, true).addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stat_call), context.getString(R.string.call_open), activity).build());
        if (bitmap != null) {
            builderAddAction.setLargeIcon(bitmap);
        }
        Notification notificationBuild = builderAddAction.build();
        if (z) {
            notificationBuild.flags |= 4;
        }
        return notificationBuild;
    }

    private static void post(Context context, int i, Notification notification) {
        try {
            nm(context).notify(i, notification);
        } catch (SecurityException e) {
            Log.w("Zola", "No notification permission", e);
        }
    }

    private static int nextId() {
        AtomicInteger atomicInteger = seq;
        int iIncrementAndGet = atomicInteger.incrementAndGet();
        if (iIncrementAndGet <= 1073741808) {
            return iIncrementAndGet;
        }
        atomicInteger.set(1000);
        return 1000;
    }

    /* JADX WARN: Code duplicated, block: B:33:0x007d  */
    private static Bitmap loadIcon(String str) {
        Bitmap bitmapDecodeByteArray;
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            if (str.startsWith("data:")) {
                int iIndexOf = str.indexOf(44);
                if (iIndexOf <= 0 || !str.substring(0, iIndexOf).contains(";base64")) {
                    bitmapDecodeByteArray = null;
                } else {
                    byte[] bArrDecode = Base64.decode(str.substring(iIndexOf + 1), 0);
                    bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArrDecode, 0, bArrDecode.length);
                }
            } else if (str.startsWith("https://")) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
                httpURLConnection.setConnectTimeout(4000);
                httpURLConnection.setReadTimeout(4000);
                httpURLConnection.setRequestProperty("User-Agent", WebViewHolder.userAgent);
                try {
                    InputStream inputStream = httpURLConnection.getInputStream();
                    try {
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        httpURLConnection.disconnect();
                        bitmapDecodeByteArray = bitmapDecodeStream;
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    httpURLConnection.disconnect();
                    throw th3;
                }
            } else {
                bitmapDecodeByteArray = null;
            }
            if (bitmapDecodeByteArray == null) {
                return null;
            }
            return circle(bitmapDecodeByteArray, 160);
        } catch (Exception unused) {
            return null;
        }
    }

    private static Bitmap circle(Bitmap bitmap, int i) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(3);
        int iMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int width = (bitmap.getWidth() - iMin) / 2;
        int height = (bitmap.getHeight() - iMin) / 2;
        float f = i / 2.0f;
        canvas.drawCircle(f, f, f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, new Rect(width, height, width + iMin, iMin + height), new Rect(0, 0, i, i), paint);
        return bitmapCreateBitmap;
    }
}
