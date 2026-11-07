package com.zola.app;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    static final String CH_BUBBLE = "chat_bubble";
    static final String CH_CALLS = "calls";
    static final String CH_CALLS_QUIET = "calls_quiet";
    static final String CH_CALL_INCOMING = "call_incoming";
    static final String CH_CALL_ONGOING = "call_ongoing";
    static final String CH_MESSAGES = "messages";
    static final String CH_MESSAGES_SILENT = "messages_silent";
    static final String CH_SERVICE = "service";

    static Context ctx;
    static final Handler MAIN = new Handler(Looper.getMainLooper());
    static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final long[] RING_VIBRATION = {0, 1000, 800, 1000, 800, 1000};

    @Override
    public void onCreate() {
        super.onCreate();
        ctx = this;
        DiagLog.init(this);
        DiagLog.sys("process start");
        if ((getApplicationInfo().flags & 2) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        createChannels();
    }

    private void createChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel chMessages = new NotificationChannel(CH_MESSAGES, getString(R.string.ch_messages), NotificationManager.IMPORTANCE_HIGH);
        chMessages.setDescription(getString(R.string.ch_messages_desc));
        chMessages.enableVibration(true);
        chMessages.setShowBadge(true);

        NotificationChannel chMessagesSilent = new NotificationChannel(CH_MESSAGES_SILENT, getString(R.string.ch_messages_silent), NotificationManager.IMPORTANCE_LOW);
        chMessagesSilent.setShowBadge(true);

        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel chCalls = new NotificationChannel(CH_CALLS, getString(R.string.ch_calls), NotificationManager.IMPORTANCE_HIGH);
        chCalls.setDescription(getString(R.string.ch_calls_desc));
        chCalls.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), audioAttrs);
        chCalls.enableVibration(true);
        chCalls.setVibrationPattern(RING_VIBRATION);
        chCalls.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        chCalls.setBypassDnd(false);

        NotificationChannel chCallsQuiet = new NotificationChannel(CH_CALLS_QUIET, getString(R.string.ch_calls_quiet), NotificationManager.IMPORTANCE_HIGH);
        chCallsQuiet.setSound(null, null);
        chCallsQuiet.enableVibration(true);
        chCallsQuiet.setVibrationPattern(RING_VIBRATION);
        chCallsQuiet.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel chService = new NotificationChannel(CH_SERVICE, getString(R.string.ch_service), NotificationManager.IMPORTANCE_MIN);
        chService.setDescription(getString(R.string.ch_service_desc));
        chService.setShowBadge(false);

        NotificationChannel chCallOngoing = new NotificationChannel(CH_CALL_ONGOING, "Cuộc gọi đang diễn ra", NotificationManager.IMPORTANCE_HIGH);
        chCallOngoing.setSound(null, null);
        chCallOngoing.enableVibration(false);
        chCallOngoing.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel chCallIncoming = new NotificationChannel(CH_CALL_INCOMING, "Cuộc gọi đến qua Zola", NotificationManager.IMPORTANCE_HIGH);
        chCallIncoming.setSound(null, null);
        chCallIncoming.enableVibration(true);
        chCallIncoming.setVibrationPattern(RING_VIBRATION);
        chCallIncoming.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel chBubble = new NotificationChannel(CH_BUBBLE, "Bong bóng chat", NotificationManager.IMPORTANCE_HIGH);
        chBubble.setSound(null, null);
        chBubble.enableVibration(false);
        chBubble.setShowBadge(false);
        if (Build.VERSION.SDK_INT >= 29) {
            chBubble.setAllowBubbles(true);
        }

        nm.createNotificationChannels(Arrays.asList(
                chMessages, chMessagesSilent, chCalls, chCallsQuiet, chService, chCallOngoing, chCallIncoming, chBubble
        ));
    }
}
