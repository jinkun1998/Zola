package com.zola.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class NativeDock extends FrameLayout {
    private static final int H = 56;
    private static final int M = 24;
    private static final int MAX = 5;
    private static final String SAVED = "native_dock_state";
    private int W;
    private final Paint badgeBg;
    private final Paint badgeText;
    private final String[] badges;
    private int baseTint;
    private final RectF box;
    private final Path boxPath;
    private final float density;
    private int down;
    private final Paint edge;
    private ObjectAnimator fade;
    private View glass;
    private final Paint glyph;
    private final String[] icons;
    private final String[] keys;
    private String last;
    private int n;
    private final Paint pill;
    private ValueAnimator pillAnim;
    private float pillX;
    private final boolean[] present;
    private final Runnable restore;
    private int sel;
    private final Paint shadow;
    private final float[] sizes;
    private boolean slid;
    private ObjectAnimator slide;
    private boolean stateV;
    private final Paint tint;
    private final RectF tmp;
    private final Path topLine;
    private Paint topPaint;
    private boolean visible;
    private final float zoom;
    private static final PathInterpolator EASE = new PathInterpolator(0.22f, 0.61f, 0.36f, 1.0f);
    private static final PathInterpolator PILL_EASE = new PathInterpolator(0.2f, 0.8f, 0.2f, 1.0f);

    NativeDock(Context context) {
        super(context);
        this.W = 228;
        this.n = 3;
        this.tint = new Paint(1);
        Paint paint = new Paint(1);
        this.pill = paint;
        Paint paint2 = new Paint(1);
        this.shadow = paint2;
        Paint paint3 = new Paint(1);
        this.edge = paint3;
        Paint paint4 = new Paint(1);
        this.glyph = paint4;
        Paint paint5 = new Paint(1);
        this.badgeBg = paint5;
        Paint paint6 = new Paint(1);
        this.badgeText = paint6;
        this.box = new RectF();
        this.tmp = new RectF();
        this.boxPath = new Path();
        this.topLine = new Path();
        this.icons = new String[]{"Contact_28_Line", "Message_28_Filled", "MyDocs_24_Line", "", ""};
        this.keys = new String[]{"STR_TAB_CONTACT", "STR_TAB_MESSAGE", "KEY_NAME_SEND2ME", "", ""};
        this.badges = new String[]{"", "", "", "", ""};
        this.sizes = new float[]{28.0f, 28.0f, 28.0f, 28.0f, 28.0f};
        this.present = new boolean[]{true, true, true, false, false};
        this.sel = 1;
        this.pillX = -1.0f;
        this.visible = true;
        this.baseTint = -16753952;
        this.stateV = true;
        this.restore = new Runnable() { // from class: com.zola.app.NativeDock$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                NativeDock.this.lambda$new$1();
            }
        };
        this.down = -1;
        setWillNotDraw(false);
        setClipChildren(false);
        float f = getResources().getDisplayMetrics().density;
        this.density = f;
        float fTextZoom = Prefs.textZoom(context) / 100.0f;
        this.zoom = fTextZoom;
        setLayerType(2, null);
        paint4.setTypeface(ZaloIcons.font(context));
        paint4.setTextAlign(Paint.Align.CENTER);
        paint6.setTypeface(Typeface.DEFAULT_BOLD);
        paint6.setTextAlign(Paint.Align.CENTER);
        paint6.setColor(-1);
        paint6.setTextSize(10.0f * f * fTextZoom);
        paint5.setColor(-1755077);
        paint2.setColor(0);
        // no shadow layer under dock
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setStrokeWidth(f);
        paint3.setColor(536870911);
        applyOpacity();
        paint4.setColor(-1);
        paint.setColor(687865856);
        setTranslationZ(1001.0f);
        apply(Prefs.sp(context).getString(SAVED, null), false);
    }

    void setGlass(View view) {
        if (view == null) {
            return;
        }
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        this.glass = view;
        addView(view, 0, glassParams());
    }

    private FrameLayout.LayoutParams glassParams() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dp(this.W), dp(56.0f), 8388659);
        layoutParams.leftMargin = dp(24.0f);
        layoutParams.topMargin = dp(24.0f);
        return layoutParams;
    }

    void applyOpacity() {
        this.tint.setColor((Math.round(Math.max(0, Math.min(100, Prefs.dockOpacity(getContext()))) * 2.55f) << 24) | (this.baseTint & ViewCompat.MEASURED_SIZE_MASK));
        invalidate();
    }

    FrameLayout.LayoutParams params() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dp(this.W + 48), dp(104.0f), 81);
        layoutParams.bottomMargin = dp(Prefs.dockBottom(getContext()) - 24);
        return layoutParams;
    }

    private int dp(float f) {
        return Math.round(f * this.density);
    }

    void setState(String str) {
        apply(str, true);
    }

    private void apply(String str, boolean z) {
        int i;
        if (str == null || str.equals(this.last)) {
            return;
        }
        this.last = str;
        try {
            JSONObject jSONObject = new JSONObject(str);
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("t");
            int i2 = this.sel;
            boolean z2 = false;
            if (jSONArrayOptJSONArray != null) {
                this.n = Math.max(1, Math.min(5, jSONArrayOptJSONArray.length()));
                int i3 = 0;
                while (true) {
                    i = this.n;
                    if (i3 >= i) {
                        break;
                    }
                    JSONArray jSONArrayOptJSONArray2 = jSONArrayOptJSONArray.optJSONArray(i3);
                    this.present[i3] = jSONArrayOptJSONArray2 != null;
                    if (jSONArrayOptJSONArray2 != null) {
                        if (!jSONArrayOptJSONArray2.optString(0).isEmpty()) {
                            this.icons[i3] = jSONArrayOptJSONArray2.optString(0);
                        }
                        this.badges[i3] = jSONArrayOptJSONArray2.optString(1);
                        if (jSONArrayOptJSONArray2.optInt(2) == 1) {
                            i2 = i3;
                        }
                        this.sizes[i3] = (float) jSONArrayOptJSONArray2.optDouble(3, 28.0d);
                        String[] strArr = this.keys;
                        strArr[i3] = jSONArrayOptJSONArray2.optString(4, strArr[i3]);
                    }
                    i3++;
                }
                if (this.sel >= i) {
                    this.sel = Math.min(1, i - 1);
                }
            }
            int iOptInt = jSONObject.optInt("w", this.W);
            if (iOptInt != this.W) {
                this.W = iOptInt;
                if (getParent() != null) {
                    setLayoutParams(params());
                }
                View view = this.glass;
                if (view != null) {
                    view.setLayoutParams(glassParams());
                }
            }
            int i4 = parse(jSONObject.optString("c"));
            if (i4 != 0) {
                this.baseTint = i4;
                applyOpacity();
            }
            int i5 = parse(jSONObject.optString("ic"));
            if (i5 != 0) {
                this.glyph.setColor(i5);
            }
            int i6 = parse(jSONObject.optString("ind"));
            if (i6 != 0) {
                this.pill.setColor(i6);
            }
            if (i2 != this.sel) {
                if (z && this.pillX >= 0.0f) {
                    z2 = true;
                }
                moveTo(i2, z2);
            }
            boolean zOptBoolean = jSONObject.optBoolean("v");
            this.stateV = zOptBoolean;
            setShown(zOptBoolean, jSONObject.optBoolean("dh"), z);
            if (z) {
                Prefs.sp(getContext()).edit().putString(SAVED, str).apply();
            }
            invalidate();
        } catch (Exception e) {
            Log.w("Zola", "bad dock state", e);
        }
    }

    private static int parse(String str) {
        Pattern patternCompile = Pattern.compile("rgba?\\(\\s*([\\d.]+),\\s*([\\d.]+),\\s*([\\d.]+)(?:,\\s*([\\d.]+))?");
        if (str == null) {
            str = "";
        }
        Matcher matcher = patternCompile.matcher(str);
        if (matcher.find()) {
            return Color.argb(Math.round((matcher.group(4) == null ? 1.0f : Float.parseFloat(matcher.group(4))) * 255.0f), Math.round(Float.parseFloat(matcher.group(1))), Math.round(Float.parseFloat(matcher.group(2))), Math.round(Float.parseFloat(matcher.group(3))));
        }
        return 0;
    }

    private void setShown(boolean z, boolean z2, boolean z3) {
        if (z != this.visible) {
            this.visible = z;
            ObjectAnimator objectAnimator = this.fade;
            if (objectAnimator != null) {
                objectAnimator.cancel();
            }
            if (z) {
                setVisibility(0);
            }
            if (z3) {
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, View.ALPHA, z ? 1.0f : 0.0f);
                this.fade = objectAnimatorOfFloat;
                objectAnimatorOfFloat.setDuration(z ? 200L : 120L);
                this.fade.setInterpolator(EASE);
                this.fade.addListener(new AnimatorListenerAdapter() { // from class: com.zola.app.NativeDock.1
                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        if (NativeDock.this.visible) {
                            return;
                        }
                        NativeDock.this.setVisibility(8);
                    }
                });
                this.fade.start();
            } else {
                setAlpha(z ? 1.0f : 0.0f);
                setVisibility(z ? 0 : 8);
            }
        }
        if (z2 != this.slid) {
            this.slid = z2;
            float fDp = z2 ? dp(Prefs.dockBottom(getContext()) + 68) : 0.0f;
            ObjectAnimator objectAnimator2 = this.slide;
            if (objectAnimator2 != null) {
                objectAnimator2.cancel();
            }
            if (z3) {
                ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, fDp);
                this.slide = objectAnimatorOfFloat2;
                objectAnimatorOfFloat2.setDuration(420L);
                this.slide.setInterpolator(EASE);
                this.slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.zola.app.NativeDock$$ExternalSyntheticLambda0
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        NativeDock.this.lambda$setShown$0(valueAnimator);
                    }
                });
                this.slide.start();
            } else {
                setTranslationY(fDp);
            }
            View view = this.glass;
            if (view != null) {
                view.invalidate();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setShown$0(ValueAnimator valueAnimator) {
        View view = this.glass;
        if (view != null) {
            view.invalidate();
        }
    }

    void hideForOpen() {
        setShown(false, this.slid, true);
        removeCallbacks(this.restore);
        postDelayed(this.restore, 1500L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1() {
        if (this.stateV) {
            setShown(true, this.slid, true);
        }
    }

    void hideNow() {
        setShown(false, this.slid, false);
    }

    private float btnW() {
        return ((dp(this.W) - dp(4.0f)) - (this.n * dp(4.0f))) / this.n;
    }

    private float btnLeft(int i) {
        return this.box.left + dp(2.0f) + dp(2.0f) + (i * (btnW() + dp(4.0f)));
    }

    private void moveTo(int i, boolean z) {
        this.sel = i;
        float fBtnLeft = btnLeft(i);
        ValueAnimator valueAnimator = this.pillAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        if (!z || this.box.isEmpty()) {
            if (this.box.isEmpty()) {
                fBtnLeft = -1.0f;
            }
            this.pillX = fBtnLeft;
            invalidate();
            return;
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.pillX, fBtnLeft);
        this.pillAnim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(350L);
        this.pillAnim.setInterpolator(PILL_EASE);
        this.pillAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.zola.app.NativeDock$$ExternalSyntheticLambda2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                NativeDock.this.lambda$moveTo$2(valueAnimator2);
            }
        });
        this.pillAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$moveTo$2(ValueAnimator valueAnimator) {
        this.pillX = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.box.set(dp(24.0f), dp(24.0f), dp(24.0f) + dp(this.W), dp(24.0f) + dp(56.0f));
        float fHeight = this.box.height() / 2.0f;
        this.boxPath.reset();
        this.boxPath.addRoundRect(this.box, fHeight, fHeight, Path.Direction.CW);
        Path path = new Path();
        this.tmp.set(this.box);
        this.tmp.offset(0.0f, this.density);
        path.addRoundRect(this.tmp, fHeight, fHeight, Path.Direction.CW);
        this.topLine.reset();
        this.topLine.op(this.boxPath, path, Path.Op.DIFFERENCE);
        this.pillX = btnLeft(this.sel);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        float fHeight = this.box.height() / 2.0f;
        // removed black shadow under dock pill
        canvas.save();
        canvas.clipPath(this.boxPath);
        super.dispatchDraw(canvas);
        canvas.drawRoundRect(this.box, fHeight, fHeight, this.tint);
        float f = this.pillX;
        if (f >= 0.0f && this.present[this.sel]) {
            this.tmp.set(f, this.box.top + dp(4.0f), this.pillX + btnW(), this.box.top + dp(4.0f) + dp(48.0f));
            canvas.drawRoundRect(this.tmp, dp(24.0f), dp(24.0f), this.pill);
        }
        Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
        for (int i = 0; i < this.n; i++) {
            if (this.present[i]) {
                float fBtnLeft = btnLeft(i) + (btnW() / 2.0f);
                float fCenterY = this.box.centerY();
                this.glyph.setTextSize(this.sizes[i] * this.density * this.zoom);
                this.glyph.getFontMetrics(fontMetrics);
                canvas.drawText(ZaloIcons.glyph(getContext(), this.icons[i]), fBtnLeft, fCenterY - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.glyph);
                String str = this.badges[i];
                float f2 = this.sizes[i];
                float f3 = this.density;
                float f4 = this.zoom;
                drawBadge(canvas, str, fBtnLeft + (((f2 * f3) * f4) / 2.0f), fCenterY - (((f2 * f3) * f4) / 2.0f));
            }
        }
        canvas.drawPath(this.topLine, edgeTop());
        canvas.restore();
        canvas.drawRoundRect(this.box.left + (this.density / 2.0f), this.box.top + (this.density / 2.0f), this.box.right - (this.density / 2.0f), this.box.bottom - (this.density / 2.0f), fHeight, fHeight, this.edge);
    }

    private Paint edgeTop() {
        if (this.topPaint == null) {
            Paint paint = new Paint(1);
            this.topPaint = paint;
            paint.setColor(956301311);
        }
        return this.topPaint;
    }

    private void drawBadge(Canvas canvas, String str, float f, float f2) {
        if (str == null || str.isEmpty()) {
            return;
        }
        if (str.equals(".")) {
            canvas.drawCircle(f - dp(3.0f), f2 + dp(3.0f), dp(4.0f), this.badgeBg);
            return;
        }
        float fDp = dp(16.0f) * this.zoom;
        float fMax = Math.max(fDp, this.badgeText.measureText(str) + dp(8.0f)) / 2.0f;
        float f3 = fDp / 2.0f;
        this.tmp.set(f - fMax, (f2 - f3) + dp(2.0f), f + fMax, f2 + f3 + dp(2.0f));
        canvas.drawRoundRect(this.tmp, f3, f3, this.badgeBg);
        Paint.FontMetrics fontMetrics = this.badgeText.getFontMetrics();
        canvas.drawText(str, this.tmp.centerX(), this.tmp.centerY() - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.badgeText);
    }

    private int hit(float f, float f2) {
        if (!this.box.contains(f, f2)) {
            return -1;
        }
        for (int i = 0; i < this.n; i++) {
            if (this.present[i] && f >= btnLeft(i) - dp(2.0f) && f < btnLeft(i) + btnW() + dp(2.0f)) {
                return i;
            }
        }
        return -1;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            if (!this.visible || this.slid || !this.box.contains(motionEvent.getX(), motionEvent.getY())) {
                return false;
            }
            this.down = hit(motionEvent.getX(), motionEvent.getY());
            return true;
        }
        if (actionMasked != 1) {
            if (actionMasked != 3) {
                return true;
            }
            this.down = -1;
            return true;
        }
        int iHit = hit(motionEvent.getX(), motionEvent.getY());
        if (iHit >= 0 && iHit == this.down) {
            tap(iHit);
        }
        this.down = -1;
        return true;
    }

    private void tap(int i) {
        performHapticFeedback(4);
        if (i != this.sel && ("STR_TAB_CONTACT".equals(this.keys[i]) || "STR_TAB_MESSAGE".equals(this.keys[i]))) {
            moveTo(i, true);
        }
        this.last = null;
        WebViewHolder.evalMain("window.__zw&&window.__zw.dockTap&&window.__zw.dockTap(" + i + ")");
    }
}
