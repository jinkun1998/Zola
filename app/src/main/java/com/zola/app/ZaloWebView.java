package com.zola.app;

import android.content.Context;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.webkit.ScriptHandler;

public final class ZaloWebView extends WebView {
    volatile String lastUrl;
    volatile int[] passExclude;
    volatile int passTop;
    final boolean popup;
    ScriptHandler scriptHandler;
    boolean shown;

    ZaloWebView(Context context, boolean z) {
        super(context);
        this.lastUrl = "";
        this.passTop = -1;
        this.popup = z;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        int[] iArr;
        if (motionEvent.getActionMasked() != 0 || this.passTop < 0 || motionEvent.getY() < this.passTop || ((iArr = this.passExclude) != null && motionEvent.getX() >= iArr[0] && motionEvent.getX() <= iArr[2] && motionEvent.getY() >= iArr[1] && motionEvent.getY() <= iArr[3])) {
            return super.dispatchTouchEvent(motionEvent);
        }
        return false;
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        if (Prefs.keepAlive(getContext())) {
            i = 0;
        }
        super.onWindowVisibilityChanged(i);
    }
}
