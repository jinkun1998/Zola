package com.zola.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
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
import android.view.Gravity;
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
    private int columnWidth = 0;
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
    // ponytail: ceiling is dual-layer blur + 2D specular gradients; upgrade path: AGSL runtime shader for animated chromatic aberration.
    private final Paint pill;
    private final Paint pillShadow;
    private final Paint pillRimPaint;
    private ValueAnimator pillAnim;
    private float pillX;
    private final boolean[] present;
    private final Runnable restore;
    private int sel;
    private final Paint shadowAmbient;
    private final Paint shadowContact;
    private final Paint glossPaint;
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
    private static final PathInterpolator PILL_EASE = new PathInterpolator(0.28f, 1.28f, 0.42f, 1.0f);

    NativeDock(Context context) {
        super(context);
        this.W = 228;
        this.n = 3;
        if (context.getResources().getConfiguration().screenWidthDp >= 600) {
            this.columnWidth = dp(340.0f);
        }
        this.tint = new Paint(1);
        this.pill = new Paint(1);
        this.pillShadow = new Paint(1);
        this.pillRimPaint = new Paint(1);
        this.shadowAmbient = new Paint(1);
        this.shadowContact = new Paint(1);
        this.glossPaint = new Paint(1);
        this.edge = new Paint(1);
        this.glyph = new Paint(1);
        this.badgeBg = new Paint(1);
        this.badgeText = new Paint(1);
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
        this.restore = new Runnable() {
            @Override
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
        this.glyph.setTypeface(ZaloIcons.font(context));
        this.glyph.setTextAlign(Paint.Align.CENTER);
        this.glyph.setColor(-1);
        this.badgeText.setTypeface(Typeface.DEFAULT_BOLD);
        this.badgeText.setTextAlign(Paint.Align.CENTER);
        this.badgeText.setColor(-1);
        this.badgeText.setTextSize(10.0f * f * fTextZoom);
        this.badgeBg.setColor(-1755077);
        this.pillShadow.setColor(0);
        this.pillRimPaint.setStyle(Paint.Style.STROKE);
        this.pillRimPaint.setStrokeWidth(0.85f * f);
        this.edge.setStyle(Paint.Style.STROKE);
        this.edge.setStrokeWidth(1.2f * f);
        setTranslationZ(1001.0f);
        applyOpacity();
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
        updateGlassShaders();
        invalidate();
    }

    private void updateGlassShaders() {
        if (this.box.isEmpty()) return;
        boolean isDark = Color.luminance(this.baseTint) < 0.5f;
        float userOpacity = Math.max(0.2f, Math.min(1.0f, Prefs.dockOpacity(getContext()) / 100.0f));

        int topTint = isDark
                ? Color.argb(Math.round(90 * userOpacity), 45, 52, 65)
                : Color.argb(Math.round(110 * userOpacity), 255, 255, 255);
        int botTint = isDark
                ? Color.argb(Math.round(135 * userOpacity), 18, 22, 28)
                : Color.argb(Math.round(90 * userOpacity), 225, 235, 245);

        this.tint.setShader(new LinearGradient(
                this.box.left, this.box.top,
                this.box.left, this.box.bottom,
                topTint, botTint,
                Shader.TileMode.CLAMP));

        int edgeTop = isDark ? 0xA0FFFFFF : 0xC8FFFFFF;
        int edgeMid = isDark ? 0x38FFFFFF : 0x55FFFFFF;
        int edgeBot = isDark ? 0x18FFFFFF : 0x25FFFFFF;
        this.edge.setShader(new LinearGradient(
                this.box.left, this.box.top,
                this.box.left, this.box.bottom,
                new int[]{edgeTop, edgeMid, edgeBot},
                new float[]{0.0f, 0.4f, 1.0f},
                Shader.TileMode.CLAMP));

        this.glossPaint.setShader(new LinearGradient(
                this.box.left, this.box.top + dp(2.0f),
                this.box.left, this.box.top + dp(24.0f),
                isDark ? 0x25FFFFFF : 0x40FFFFFF,
                0x00FFFFFF,
                Shader.TileMode.CLAMP));

        this.shadowAmbient.setColor(0);
        this.shadowAmbient.setShadowLayer(dp(16.0f), 0, dp(6.0f),
                isDark ? Color.argb(70, 0, 0, 0) : Color.argb(38, 0, 16, 40));

        this.shadowContact.setColor(0);
        this.shadowContact.setShadowLayer(dp(5.0f), 0, dp(1.5f),
                isDark ? Color.argb(55, 0, 0, 0) : Color.argb(22, 0, 10, 24));
    }

    void setColumnWidth(int w) {
        if (this.columnWidth == w) {
            return;
        }
        this.columnWidth = w;
        if (getParent() != null) {
            setLayoutParams(params());
        }
    }

    FrameLayout.LayoutParams params() {
        int gravity = this.columnWidth > 0 ? (Gravity.BOTTOM | Gravity.START) : (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dp(this.W + 48), dp(104.0f), gravity);
        if (this.columnWidth > 0) {
            layoutParams.leftMargin = Math.max(0, (this.columnWidth - dp(this.W + 48)) / 2);
        }
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
        updateGlassShaders();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        float fHeight = this.box.height() / 2.0f;
        boolean isDark = Color.luminance(this.baseTint) < 0.5f;

        // 1. Soft liquid glass ambient elevation shadow
        canvas.save();
        canvas.clipOutPath(this.boxPath);
        canvas.drawRoundRect(this.box.left, this.box.top + dp(4.0f), this.box.right, this.box.bottom + dp(4.0f), fHeight, fHeight, this.shadowAmbient);
        canvas.drawRoundRect(this.box.left, this.box.top + dp(1.5f), this.box.right, this.box.bottom + dp(1.5f), fHeight, fHeight, this.shadowContact);
        canvas.restore();

        // 2. Translucent frosted glass backdrop & refraction
        canvas.save();
        canvas.clipPath(this.boxPath);
        super.dispatchDraw(canvas);
        canvas.drawRoundRect(this.box, fHeight, fHeight, this.tint);

        // 3. Curved optical liquid gloss reflection
        float glossMargin = dp(4.0f);
        this.tmp.set(this.box.left + glossMargin, this.box.top + dp(2.0f), this.box.right - glossMargin, this.box.top + dp(24.0f));
        canvas.drawRoundRect(this.tmp, dp(12.0f), dp(12.0f), this.glossPaint);

        // 4. Liquid Active Tab Pill (droplet lens)
        float f = this.pillX;
        if (f >= 0.0f && this.present[this.sel]) {
            float pLeft = f;
            float pTop = this.box.top + dp(4.0f);
            float pRight = f + btnW();
            float pBottom = this.box.top + dp(52.0f);
            this.tmp.set(pLeft, pTop, pRight, pBottom);

            // Droplet shadow in track
            canvas.drawRoundRect(this.tmp.left, this.tmp.top + dp(1.0f), this.tmp.right, this.tmp.bottom + dp(1.0f), dp(24.0f), dp(24.0f), this.pillShadow);

            // Droplet body lens gradient
            int pTopColor = isDark ? 0x3EFFFFFF : 0x5EFFFFFF;
            int pBotColor = isDark ? 0x16FFFFFF : 0x24FFFFFF;
            this.pill.setShader(new LinearGradient(pLeft, pTop, pLeft, pBottom, pTopColor, pBotColor, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(this.tmp, dp(24.0f), dp(24.0f), this.pill);

            // Droplet specular rim
            this.pillRimPaint.setShader(new LinearGradient(pLeft, pTop, pLeft, pBottom, 0x78FFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(this.tmp, dp(24.0f), dp(24.0f), this.pillRimPaint);
        }

        // 5. Icons & Badges
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

        // 6. Top Specular Meniscus
        canvas.drawPath(this.topLine, edgeTop());
        canvas.restore();

        // 7. Outer Refractive Edge Rim
        float halfStroke = this.edge.getStrokeWidth() / 2.0f;
        canvas.drawRoundRect(this.box.left + halfStroke, this.box.top + halfStroke, this.box.right - halfStroke, this.box.bottom - halfStroke, fHeight, fHeight, this.edge);
    }

    private Paint edgeTop() {
        if (this.topPaint == null) {
            Paint paint = new Paint(1);
            this.topPaint = paint;
            paint.setColor(0xDEFFFFFF);
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
