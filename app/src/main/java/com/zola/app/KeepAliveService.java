package com.zola.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/* JADX INFO: loaded from: classes.dex */
public class KeepAliveService extends Service {
    static final String ACTION_ANSWER = "com.zola.app.ANSWER";
    static final String ACTION_CHECK = "com.zola.app.CHECK";
    static final String ACTION_HANGUP = "com.zola.app.HANGUP";
    static final String ACTION_MIC = "com.zola.app.MIC";
    static final String ACTION_SPEAKER = "com.zola.app.SPEAKER";
    static final int CALL_NOTIF_ID = 3;
    private static final long CHECK_LOCKED_MS = 120000;
    private static final long CHECK_OFF_MS = 30000;
    private static final long CHECK_ON_MS = 120000;
    static final int NOTIF_ID = 1;
    private static final long TICK_MS = 30000;
    private static String conn = "";
    private static KeepAliveService instance;
    private static boolean mediaActive;
    private static boolean mediaVideo;
    private static boolean speakerOn;
    private PowerManager.WakeLock checkLock;
    private PowerManager.WakeLock proximityLock;
    private final BroadcastReceiver diagReceiver = new BroadcastReceiver() { // from class: com.zola.app.KeepAliveService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                DiagLog.sys("screen on");
                WebViewHolder.wake("screen on");
                KeepAliveService.scheduleCheck(context, 30000L);
                return;
            }
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                WebViewHolder.wake("unlocked");
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                DiagLog.sys("screen off");
                if (Prefs.keepOnLock(context)) {
                    KeepAliveService.scheduleCheck(context, 120000L);
                    DiagLog.conn("screen locked: keep-alive continues (keep connection when locked)");
                    return;
                } else {
                    if (KeepAliveService.mediaActive) {
                        return;
                    }
                    KeepAliveService.cancelCheck(context);
                    if (KeepAliveService.this.checkLock != null && KeepAliveService.this.checkLock.isHeld()) {
                        KeepAliveService.this.checkLock.release();
                    }
                    DiagLog.conn("screen locked: keep-alive paused");
                    return;
                }
            }
            if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
                boolean z = powerManager != null && powerManager.isDeviceIdleMode();
                DiagLog.sys(z ? "doze on" : "doze off");
                if (z) {
                    return;
                }
                WebViewHolder.wake("doze off");
            }
        }
    };
    private long lastTick = SystemClock.elapsedRealtime();
    private boolean lastInteractive = true;
    private final Runnable tick = new Runnable() { // from class: com.zola.app.KeepAliveService.2
        @Override // java.lang.Runnable
        public void run() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long j = jElapsedRealtime - KeepAliveService.this.lastTick;
            PowerManager powerManager = (PowerManager) KeepAliveService.this.getSystemService(PowerManager.class);
            boolean z = powerManager == null || powerManager.isInteractive();
            if (j > 90000 && z && KeepAliveService.this.lastInteractive) {
                DiagLog.conn("app process was paused ~" + (j / 1000) + "s while the screen was on (system froze the app)");
            }
            KeepAliveService.this.lastTick = jElapsedRealtime;
            KeepAliveService.this.lastInteractive = z;
            App.MAIN.postDelayed(this, 30000L);
        }
    };
    private int fgId = 1;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void refreshNotification() {
        KeepAliveService keepAliveService = instance;
        if (keepAliveService != null) {
            keepAliveService.goForeground();
        }
    }

    static void start(Context context) {
        try {
            context.startForegroundService(new Intent(context, (Class<?>) KeepAliveService.class));
        } catch (RuntimeException e) {
            Log.w("Zola", "Cannot start keep-alive service", e);
        }
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, (Class<?>) KeepAliveService.class));
    }

    static boolean isSpeakerOn() {
        return speakerOn;
    }

    static boolean isMediaActive() {
        return mediaActive;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        instance = this;
        DiagLog.sys("service start");
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        registerReceiver(this.diagReceiver, intentFilter);
        App.MAIN.postDelayed(this.tick, 30000L);
        WebViewHolder.get(this);
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent intent) {
        DiagLog.sys("app swiped away from recents");
        super.onTaskRemoved(intent);
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        goForeground();
        if (intent != null && ACTION_SPEAKER.equals(intent.getAction())) {
            setSpeaker(this, !speakerOn);
        }
        if (intent != null && ACTION_HANGUP.equals(intent.getAction())) {
            // ponytail: VoIP hangup stub
        }
        if (intent != null && ACTION_MIC.equals(intent.getAction())) {
            // ponytail: VoIP mute stub
        }
        if (intent != null && ACTION_ANSWER.equals(intent.getAction())) {
            // ponytail: VoIP answer stub
        }
        if (intent == null || !ACTION_CHECK.equals(intent.getAction())) {
            scheduleCheck(this, 120000L);
        } else {
            check();
        }
        return 1;
    }

    @Override // android.app.Service
    public void onDestroy() {
        DiagLog.sys("service stop");
        App.MAIN.removeCallbacks(this.tick);
        try {
            unregisterReceiver(this.diagReceiver);
        } catch (IllegalArgumentException unused) {
        }
        instance = null;
        PowerManager.WakeLock wakeLock = this.proximityLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            this.proximityLock.release();
        }
        PowerManager.WakeLock wakeLock2 = this.checkLock;
        if (wakeLock2 != null && wakeLock2.isHeld()) {
            this.checkLock.release();
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(PendingIntent.getService(this, 103, new Intent(this, (Class<?>) KeepAliveService.class).setAction(ACTION_CHECK), 201326592));
        }
        super.onDestroy();
    }

    private void goForeground() {
        Notification notificationBuildNotification = buildNotification();
        int i = 1;
        if (Build.VERSION.SDK_INT >= 34) {
            int iMediaTypes = mediaActive ? mediaTypes() : 0;
            if (iMediaTypes != 0) {
                try {
                    startForeground(i, notificationBuildNotification, iMediaTypes | 512);
                } catch (RuntimeException e) {
                    Log.w("Zola", "Cannot add microphone/camera type", e);
                    try {
                        startForeground(i, notificationBuildNotification, 512);
                    } catch (RuntimeException e2) {
                        giveUp(e2);
                        return;
                    }
                }
            } else {
                startForeground(i, notificationBuildNotification, 512);
            }
        } else {
            try {
                startForeground(i, notificationBuildNotification);
            } catch (RuntimeException e3) {
                giveUp(e3);
                return;
            }
        }
        if (i != this.fgId) {
            ((NotificationManager) getSystemService(NotificationManager.class)).cancel(this.fgId);
            this.fgId = i;
        }
    }

    private void giveUp(RuntimeException runtimeException) {
        Log.w("Zola", "startForeground failed, stopping keep-alive service", runtimeException);
        stopSelf();
    }

    private int mediaTypes() {
        int i = checkSelfPermission("android.permission.RECORD_AUDIO") == 0 ? 128 : 0;
        return (mediaVideo && checkSelfPermission("android.permission.CAMERA") == 0) ? i | 64 : i;
    }

    private Notification buildNotification() {

        PendingIntent activity = PendingIntent.getActivity(this, 100, new Intent(this, (Class<?>) MainActivity.class).addFlags(268435456), 201326592);
        PendingIntent activity2 = PendingIntent.getActivity(this, 101, new Intent(this, (Class<?>) SettingsActivity.class).addFlags(268435456), 201326592);
        String str = NotificationCompat.CATEGORY_SERVICE;
        Notification.Builder showWhen = new Notification.Builder(this, NotificationCompat.CATEGORY_SERVICE).setSmallIcon(mediaActive ? R.drawable.ic_stat_call : R.drawable.ic_stat_message).setColor(-16750337).setContentTitle(getString(mediaActive ? R.string.svc_in_call : R.string.svc_running)).setContentText(mediaActive ? getString(R.string.svc_in_call_text) : connText()).setContentIntent(activity).setOngoing(mediaActive).setShowWhen(false);
        if (mediaActive) {
            str = NotificationCompat.CATEGORY_CALL;
        }
        Notification.Builder group = showWhen.setCategory(str).setGroup("zola.service");
        if (mediaActive) {
            group.addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_speaker), getString(speakerOn ? R.string.action_speaker_off : R.string.action_speaker_on), PendingIntent.getService(this, 102, new Intent(this, (Class<?>) KeepAliveService.class).setAction(ACTION_SPEAKER), 201326592)).build());
        }
        group.addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_tune), getString(R.string.action_settings), activity2).build());
        if (Build.VERSION.SDK_INT >= 31) {
            group.setForegroundServiceBehavior(1);
        }
        return group.build();
    }

    private String connText() {
        String str = conn;
        str.hashCode();
        switch (str) {
            case "on":
                return getString(R.string.svc_conn_on);
            case "off":
                return getString(R.string.svc_conn_off);
            case "wait":
                return getString(R.string.svc_conn_wait);
            default:
                return getString(R.string.svc_running_text);
        }
    }

    static void setConn(String str) {
        if (str.equals(conn)) {
            return;
        }
        conn = str;
        KeepAliveService keepAliveService = instance;
        if (keepAliveService == null) {
            return;
        }
        if (!mediaActive) {
            keepAliveService.goForeground();
        }
        scheduleCheck(keepAliveService, "on".equals(str) ? 120000L : 30000L);
    }

    private static boolean inUse(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        return mediaActive || Prefs.keepOnLock(context) || powerManager == null || powerManager.isInteractive();
    }

    private static boolean locked(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        return (powerManager == null || powerManager.isInteractive()) ? false : true;
    }

    private static PendingIntent checkIntent(Context context) {
        return PendingIntent.getService(context, 103, new Intent(context, (Class<?>) KeepAliveService.class).setAction(ACTION_CHECK), 201326592);
    }

    static void cancelCheck(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(checkIntent(context));
        }
    }

    static void scheduleCheck(Context context, long j) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        if (!inUse(context)) {
            alarmManager.cancel(checkIntent(context));
            return;
        }
        try {
            alarmManager.setAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + j, checkIntent(context));
        } catch (RuntimeException e) {
            Log.w("Zola", "schedule connection check", e);
        }
    }

    private void check() {
        if (!inUse(this)) {
            DiagLog.conn("alarm check skipped: screen locked");
            return;
        }
        if (this.checkLock == null) {
            PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) getSystemService(PowerManager.class)).newWakeLock(1, "Zola:conncheck");
            this.checkLock = wakeLockNewWakeLock;
            wakeLockNewWakeLock.setReferenceCounted(false);
        }
        this.checkLock.acquire(20000L);
        DiagLog.add("connection check (" + (conn.isEmpty() ? "?" : conn) + ")");
        DiagLog.conn("alarm check, state=" + (conn.isEmpty() ? "?" : conn) + " | " + DiagLog.powerState(this));
        WebViewHolder.wake(NotificationCompat.CATEGORY_ALARM);
        WebViewHolder.evalMain("window.__zw&&window.__zw.connReport&&window.__zw.connReport()");
        long j = 120000;
        if ((!locked(this) || !Prefs.keepOnLock(this) || mediaActive) && !"on".equals(conn)) {
            j = 30000;
        }
        scheduleCheck(this, j);
    }

    /* JADX WARN: Code duplicated, block: B:12:0x0014  */
    static void setMediaActive(Context context, boolean z, boolean z2) {
        boolean z3;
        if (mediaActive == z) {
            if (mediaVideo != (z && z2)) {
                z3 = true;
            } else {
                z3 = false;
            }
        } else {
            z3 = true;
        }
        mediaActive = z;
        mediaVideo = z && z2;
        if (z3) {
            if (z) {
                Notifier.cancelCallAlert(context);
            } else if (speakerOn) {
                setSpeaker(context, false);
            }
            KeepAliveService keepAliveService = instance;
            if (keepAliveService != null) {
                keepAliveService.goForeground();
                keepAliveService.updateProximity();
            }
            MainActivity mainActivityHost = WebViewHolder.host();
            if (mainActivityHost != null) {
                mainActivityHost.onMediaActiveChanged(z);
            }
        }
    }

    static void setSpeaker(Context context, boolean z) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AudioManager.class);
        try {
            if (Build.VERSION.SDK_INT < 31) {
                audioManager.setSpeakerphoneOn(z);
            } else if (z) {
                for (AudioDeviceInfo audioDeviceInfo : audioManager.getAvailableCommunicationDevices()) {
                    if (audioDeviceInfo.getType() == 2) {
                        audioManager.setCommunicationDevice(audioDeviceInfo);
                        break;
                    }
                }
            } else {
                audioManager.clearCommunicationDevice();
            }
        } catch (RuntimeException e) {
            Log.w("Zola", "setSpeaker", e);
        }
        boolean z2 = speakerOn != z;
        speakerOn = z;
        KeepAliveService keepAliveService = instance;
        if (keepAliveService != null && z2) {
            keepAliveService.goForeground();
            keepAliveService.updateProximity();
        }
        if (z2) {
            
        }
    }

    private void updateProximity() {
        if (mediaActive && !mediaVideo && !speakerOn) {
            if (this.proximityLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(PowerManager.class);
                if (powerManager.isWakeLockLevelSupported(32)) {
                    this.proximityLock = powerManager.newWakeLock(32, "Zola:proximity");
                }
            }
            PowerManager.WakeLock wakeLock = this.proximityLock;
            if (wakeLock == null || wakeLock.isHeld()) {
                return;
            }
            this.proximityLock.acquire(7200000L);
            return;
        }
        PowerManager.WakeLock wakeLock2 = this.proximityLock;
        if (wakeLock2 == null || !wakeLock2.isHeld()) {
            return;
        }
        this.proximityLock.release(1);
    }
}
