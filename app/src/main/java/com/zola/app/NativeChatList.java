package com.zola.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class NativeChatList extends FrameLayout {
    private static final String BOOT_LAYOUT = "native_boot_layout";
    private static final String BOOT_ME = "native_boot_me";
    private static final String FILE = "convlist.json";
    private final Adapter adapter;
    private boolean announced;
    private int bg;
    private int columnWidth = 0;
    private String selectedId = null;
    private final Paint colBorderPaint = new Paint(1);
    private boolean bootEnding;
    private boolean booting;
    private boolean borrowed;
    private RenderNode content;
    private final float density;
    private boolean endingBoot;
    private int fg;
    private int fg2;
    private int fullTop;
    private DockGlass glass;
    private Header header;
    private final List<Item> items;
    private String lastLayout;
    private ZaloWebView lastWv;
    private final ListView listView;
    Runnable onOpen;
    private long openingAt;
    private String pendingOpen;
    private String savedBoot;
    private String savedMe;
    private long shownAt;
    private ValueAnimator slideAnim;
    private boolean slideForward;
    private ImageView slideOld;
    private final Runnable slideRun;
    private int tabCount;
    private String tabNames;
    private int tabSel;
    private int topPx;
    private final float zoom;

    static final class Item {
        String avatar;
        boolean group;
        String icon;
        String id;
        boolean me;
        String[] members;
        int mention;
        boolean missCall;
        boolean muted;
        String name;
        int pin;
        String text;
        long time;
        int total;
        int unread;
        int unreadDot;

        Item() {
        }
    }

    NativeChatList(Context context) {
        super(context);
        Adapter adapter = new Adapter();
        this.adapter = adapter;
        this.items = new ArrayList();
        boolean dark = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        this.bg = dark ? 0xFF22262B : 0xFFFFFFFF;
        this.fg = dark ? -1644309 : -16245958;
        this.fg2 = dark ? -6642510 : -10851967;
        setBackgroundColor(this.bg);
        this.tabSel = -1;
        this.tabNames = "";
        this.slideRun = this::runTabSlide;
        if (context.getResources().getConfiguration().screenWidthDp >= 600) {
            this.columnWidth = dp(340.0f);
        }
        this.density = getResources().getDisplayMetrics().density;
        this.zoom = Prefs.textZoom(context) / 100.0f;
        ListView listView = new ListView(context) { // from class: com.zola.app.NativeChatList.1
            @Override // android.widget.AbsListView, android.view.View
            public void draw(Canvas canvas) {
                if (Build.VERSION.SDK_INT < 31 || !canvas.isHardwareAccelerated()) {
                    super.draw(canvas);
                    return;
                }
                if (NativeChatList.this.content == null) {
                    NativeChatList.this.content = new RenderNode("zd-list");
                }
                NativeChatList.this.content.setPosition(0, 0, getWidth(), getHeight());
                try {
                    super.draw(NativeChatList.this.content.beginRecording());
                    NativeChatList.this.content.endRecording();
                    canvas.drawRenderNode(NativeChatList.this.content);
                    if (NativeChatList.this.glass == null || NativeChatList.this.glass.getVisibility() != 0) {
                        return;
                    }
                    NativeChatList.this.glass.invalidate();
                } catch (Throwable th) {
                    NativeChatList.this.content.endRecording();
                    throw th;
                }
            }
        };
        this.listView = listView;
        listView.setDivider(null);
        listView.setSelector(new GradientDrawable());
        listView.setClipToPadding(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setOverScrollMode(2);
        listView.setAdapter((ListAdapter) adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda2
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                NativeChatList.this.lambda$new$0(adapterView, view, i, j);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda3
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public final boolean onItemLongClick(AdapterView adapterView, View view, int i, long j) {
                return NativeChatList.this.lambda$new$1(adapterView, view, i, j);
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() { // from class: com.zola.app.NativeChatList.2
            private int lastTop = 0;
            private int lastFirst = 0;
            private long lastLoadMore = 0;

            @Override // android.widget.AbsListView.OnScrollListener
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (i != 0) {
                    NativeChatList.this.settleMoves();
                }
            }

            @Override // android.widget.AbsListView.OnScrollListener
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                int height;
                boolean z = false;
                View childAt = absListView.getChildAt(0);
                if (childAt == null) {
                    return;
                }
                int top = childAt.getTop();
                int i4 = this.lastFirst;
                if (i == i4) {
                    height = this.lastTop - top;
                } else {
                    height = childAt.getHeight();
                    if (i <= i4) {
                        height = -height;
                    }
                }
                this.lastTop = top;
                this.lastFirst = i;
                if (i == 0 && top >= NativeChatList.this.listView.getPaddingTop() - 2) {
                    z = true;
                }
                int iRound = Math.round(height / NativeChatList.this.density);
                NativeChatList.this.glass.invalidate();
                if (iRound != 0 || z) {
                    WebViewHolder.evalMain("window.__zw&&window.__zw.nativeScroll&&window.__zw.nativeScroll(" + iRound + "," + z + ")");
                }
                if (i3 > 0 && i + i2 >= i3 - 4) {
                    long now = SystemClock.uptimeMillis();
                    if (now - this.lastLoadMore > 800) {
                        this.lastLoadMore = now;
                        WebViewHolder.evalMain("window.__zw&&window.__zw.loadMoreConvs&&window.__zw.loadMoreConvs()");
                    }
                }
            }
        });
        addView(listView, new FrameLayout.LayoutParams(-1, -1));
        DockGlass dockGlass = new DockGlass(context);
        this.glass = dockGlass;
        dockGlass.setVisibility(8);
        Header header = new Header(context);
        this.header = header;
        addView(header, new FrameLayout.LayoutParams(-1, -2, 48));
        WebViewHolder.evalMain("window.__zw&&window.__zw.connReport&&window.__zw.connReport()");
        setVisibility(8);
        load();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(AdapterView adapterView, View view, int i, long j) {
        if (this.columnWidth <= 0 && opening()) {
            return;
        }
        Item item = this.items.get(i);
        if (this.booting) {
            this.pendingOpen = item.id;
            return;
        }
        if (this.columnWidth <= 0) {
            this.openingAt = SystemClock.uptimeMillis();
            Runnable runnable = this.onOpen;
            if (runnable != null) {
                runnable.run();
            }
        }
        setSelectedId(item.id);
        WebViewHolder.evalMain("window.__zw&&window.__zw.openConv&&window.__zw.openConv(" + JSONObject.quote(item.id) + ")");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$new$1(AdapterView adapterView, View view, int i, long j) {
        if (!this.booting && !opening()) {
            Item item = this.items.get(i);
            view.performHapticFeedback(0);
            int[] iArr = new int[2];
            int[] iArr2 = new int[2];
            view.getLocationInWindow(iArr);
            getLocationInWindow(iArr2);
            int iRound = Math.round((iArr[1] - iArr2[1]) / this.density);
            int iRound2 = Math.round(view.getHeight() / this.density);
            view.setPressed(false);
            WebViewHolder.evalMain("window.__zw&&window.__zw.convMenu&&window.__zw.convMenu(" + JSONObject.quote(item.id) + "," + i + "," + iRound + "," + iRound2 + "," + JSONObject.quote(snapshot(view)) + ")");
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float f) {
        return Math.round(f * this.density);
    }

    private static String snapshot(View view) {
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            try {
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
                view.draw(new Canvas(bitmapCreateBitmap));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmapCreateBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                bitmapCreateBitmap.recycle();
                return "data:image/png;base64," + Base64.encodeToString(byteArrayOutputStream.toByteArray(), 2);
            } catch (OutOfMemoryError | RuntimeException unused) {
            }
        }
        return "";
    }

    void setData(String str) {
        String str2;
        boolean zHasData = hasData();
        if (parse(str)) {
            if (hasData() && !this.borrowed) {
                save(getContext(), str);
            }
            if (zHasData != hasData() && (str2 = this.lastLayout) != null) {
                setLayout(str2, this.lastWv);
            }
            if (this.announced) {
                return;
            }
            this.announced = true;
            WebViewHolder.evalMain("window.__zw&&window.__zw.nativeReady&&window.__zw.nativeReady(true)");
        }
    }

    static void save(Context context, final String str) {
        final File file = new File(context.getApplicationContext().getFilesDir(), FILE);
        App.IO.execute(new Runnable() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                NativeChatList.lambda$save$2(file, str);
            }
        });
    }

    static /* synthetic */ void lambda$save$2(File file, String str) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } catch (Throwable th) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
            Log.w("Zola", "save conv list", e);
        }
    }

    private void load() {
        File file = new File(getContext().getFilesDir(), FILE);
        if (file.exists()) {
            try {
                parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (IOException e) {
                Log.w("Zola", "load conv list", e);
            }
        }
    }

    boolean hasData() {
        return !this.items.isEmpty();
    }

    private Map<String, Integer> captureTops() {
        HashMap map = new HashMap();
        int firstVisiblePosition = this.listView.getFirstVisiblePosition();
        for (int i = 0; i < this.listView.getChildCount(); i++) {
            int i2 = firstVisiblePosition + i;
            if (i2 < this.items.size()) {
                map.put(this.items.get(i2).id, Integer.valueOf(this.listView.getChildAt(i).getTop()));
            }
        }
        return map;
    }

    private void animateMoves(final Map<String, Integer> map, final List<String> list) {
        if (!map.isEmpty() && this.slideOld == null && getVisibility() == 0 && isShown()) {
            this.listView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: com.zola.app.NativeChatList.3
                @Override // android.view.ViewTreeObserver.OnPreDrawListener
                public boolean onPreDraw() {
                    int i;
                    int top = 0;
                    NativeChatList.this.listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int firstVisiblePosition = NativeChatList.this.listView.getFirstVisiblePosition();
                    int height = NativeChatList.this.listView.getHeight();
                    PathInterpolator pathInterpolator = new PathInterpolator(0.2f, 0.9f, 0.25f, 1.0f);
                    for (int i2 = 0; i2 < NativeChatList.this.listView.getChildCount() && (i = firstVisiblePosition + i2) < NativeChatList.this.items.size(); i2++) {
                        View childAt = NativeChatList.this.listView.getChildAt(i2);
                        String str = ((Item) NativeChatList.this.items.get(i)).id;
                        Integer num = (Integer) map.get(str);
                        if (num != null) {
                            top = num.intValue() - childAt.getTop();
                        } else {
                            int iIndexOf = list.indexOf(str);
                            if (iIndexOf < 0) {
                                childAt.setAlpha(0.0f);
                                childAt.animate().alpha(1.0f).setDuration(260L).setInterpolator(pathInterpolator).start();
                            } else {
                                top = iIndexOf < firstVisiblePosition ? (-childAt.getHeight()) * 2 : height - childAt.getTop();
                            }
                        }
                        float f = top;
                        if (Math.abs(f) >= 1.0f) {
                            childAt.setTranslationY(f);
                            childAt.animate().translationY(0.0f).setDuration(320L).setInterpolator(pathInterpolator).start();
                        }
                    }
                    return true;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settleMoves() {
        for (int i = 0; i < this.listView.getChildCount(); i++) {
            View childAt = this.listView.getChildAt(i);
            childAt.animate().cancel();
            childAt.setTranslationY(0.0f);
            childAt.setAlpha(1.0f);
        }
    }

    private boolean parse(String str) {
        try {
            JSONArray jSONArray = new JSONArray(str);
            ArrayList arrayList = new ArrayList(jSONArray.length());
            HashMap map = new HashMap();
            for (Item item : this.items) {
                map.put(item.id, item);
            }
            this.borrowed = false;
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                Item item2 = new Item();
                item2.id = jSONObject.optString("id");
                item2.group = jSONObject.optBoolean("g");
                item2.name = jSONObject.optString("n");
                item2.avatar = jSONObject.optString("a");
                item2.time = jSONObject.optLong("t");
                item2.text = jSONObject.optString("m");
                item2.unread = jSONObject.optInt("u");
                item2.mention = jSONObject.optInt("at");
                item2.unreadDot = jSONObject.optInt("ud");
                item2.pin = jSONObject.optInt("p");
                item2.muted = jSONObject.optBoolean("mu");
                item2.me = jSONObject.optBoolean("me");
                item2.icon = jSONObject.optString("ic");
                item2.missCall = jSONObject.optBoolean("miss");
                item2.total = jSONObject.optInt("tm");
                if (item2.unread <= 0 && (item2.time <= 0 || "Chưa có tin nhắn".equals(item2.text))) {
                    continue;
                }
                JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("ms");
                item2.members = new String[jSONArrayOptJSONArray == null ? 0 : jSONArrayOptJSONArray.length()];
                for (int i2 = 0; i2 < item2.members.length; i2++) {
                    item2.members[i2] = jSONArrayOptJSONArray.optString(i2);
                }
                Item item3 = (Item) map.get(item2.id);
                if (item3 != null && item2.name.equals(item2.id) && !item3.name.equals(item3.id)) {
                    item2.name = item3.name;
                    if (item2.avatar.isEmpty()) {
                        item2.avatar = item3.avatar;
                    }
                    if (item2.members.length == 0) {
                        item2.members = item3.members;
                    }
                    if (item2.total == 0) {
                        item2.total = item3.total;
                    }
                    this.borrowed = true;
                }
                arrayList.add(item2);
            }
            Map<String, Integer> mapCaptureTops = captureTops();
            ArrayList arrayList2 = new ArrayList(this.items.size());
            Iterator<Item> it = this.items.iterator();
            while (it.hasNext()) {
                arrayList2.add(it.next().id);
            }
            this.items.clear();
            this.items.addAll(arrayList);
            this.adapter.notifyDataSetChanged();
            animateMoves(mapCaptureTops, arrayList2);
            final DockGlass dockGlass = this.glass;
            if (dockGlass != null) {
                Objects.requireNonNull(dockGlass);
                dockGlass.post(new Runnable() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        dockGlass.invalidate();
                    }
                });
            }
            if (this.slideOld != null) {
                this.listView.setSelection(0);
                this.listView.post(this::runTabSlide);
            }
            return true;
        } catch (Exception e) {
            Log.w("Zola", "bad conv list", e);
            return false;
        }
    }

    boolean boot(boolean z) {
        JSONArray jSONArrayOptJSONArray;
        JSONArray jSONArray;
        if (z) {
            if (!hasData()) {
                return false;
            }
            this.booting = true;
            try {
                JSONObject jSONObject = new JSONObject(Prefs.sp(getContext()).getString(BOOT_LAYOUT, "{}"));
                int sw = jSONObject.optInt("sw", 0);
                if (sw > 0) {
                    setColumnWidthPx(dp(sw));
                } else if (getResources().getConfiguration().screenWidthDp >= 600) {
                    setColumnWidthPx(dp(340.0f));
                } else {
                    setColumnWidthPx(0);
                }
                applyColors(parseCss(jSONObject.optString("bg")), parseCss(jSONObject.optString("fg")), parseCss(jSONObject.optString("fg2")));
                animateTop(dp(jSONObject.optInt("top", 96)));
                jSONArrayOptJSONArray = jSONObject.optJSONArray("tabs");
                try {
                    placeGlass(jSONObject.optJSONArray("dock"), false);
                } catch (Exception unused) {
                    animateTop(dp(96.0f));
                }
            } catch (Exception unused2) {
                jSONArrayOptJSONArray = null;
            }
            try {
                jSONArray = new JSONArray(Prefs.sp(getContext()).getString(BOOT_ME, "[]"));
            } catch (Exception unused3) {
                jSONArray = null;
            }
            if (jSONArray != null) {
                this.header.setMe(jSONArray.optString(0), jSONArray.optString(1));
            }
            if (jSONArrayOptJSONArray != null) {
                this.header.setTabs(jSONArrayOptJSONArray.optJSONArray(0), jSONArrayOptJSONArray.optInt(1), false);
            }
            this.header.setConn("wait");
            setVisibility(0);
            setOutlineProvider(null);
            setTranslationZ(1000.0f);
            return true;
        }
        if (!this.booting) {
            return false;
        }
        if (!bootLayoutOn()) {
            this.bootEnding = true;
            return true;
        }
        endBoot();
        return true;
    }

    private boolean opening() {
        return this.openingAt != 0 && SystemClock.uptimeMillis() - this.openingAt < 1500;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Header extends LinearLayout {
        final ImageView av;
        ObjectAnimator blink;
        final TextView cat;
        String cats;
        final TextView chev;
        String conn;
        final View dot;
        final GradientDrawable dotBg;
        final TextView[] icons;
        final View indicator;
        final View line;
        String me;
        String meAv;
        final TextView more;
        final TextView name;
        int sel;
        final TextView status;
        final FrameLayout tabRow;
        final LinearLayout tabs;

        Header(Context context) {
            super(context);
            GradientDrawable gradientDrawable = new GradientDrawable();
            this.dotBg = gradientDrawable;
            this.icons = new TextView[3];
            this.me = "";
            this.meAv = "";
            this.conn = "";
            this.cats = "";
            this.sel = -1;
            setOrientation(1);
            setClickable(true);
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setGravity(16);
            linearLayout.setPadding(NativeChatList.this.dp(12.0f), 0, NativeChatList.this.dp(10.0f), 0);
            addView(linearLayout, new LinearLayout.LayoutParams(-1, NativeChatList.this.dp(64.0f)));
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setGravity(16);
            linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    Header.this.lambda$new$0(view);
                }
            });
            linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -1, 1.0f));
            ImageView imageView = new ImageView(context);
            this.av = imageView;
            linearLayout2.addView(imageView, new LinearLayout.LayoutParams(NativeChatList.this.dp(36.0f), NativeChatList.this.dp(36.0f)));
            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setOrientation(1);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
            layoutParams.leftMargin = NativeChatList.this.dp(10.0f);
            linearLayout2.addView(linearLayout3, layoutParams);
            TextView textView = new TextView(context);
            this.name = textView;
            textView.setTextSize(1, NativeChatList.this.zoom * 17.0f);
            textView.setTypeface(Typeface.create("sans-serif-medium", 0));
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout3.addView(textView);
            LinearLayout linearLayout4 = new LinearLayout(context);
            linearLayout4.setGravity(16);
            View view = new View(context);
            this.dot = view;
            gradientDrawable.setShape(1);
            view.setBackground(gradientDrawable);
            linearLayout4.addView(view, new LinearLayout.LayoutParams(NativeChatList.this.dp(7.0f), NativeChatList.this.dp(7.0f)));
            TextView textView2 = new TextView(context);
            this.status = textView2;
            textView2.setTextSize(1, NativeChatList.this.zoom * 12.0f);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
            layoutParams2.leftMargin = NativeChatList.this.dp(5.0f);
            linearLayout4.addView(textView2, layoutParams2);
            linearLayout3.addView(linearLayout4);
            String[] strArr = {"search", "outline-add-new-contact-2", "Group_Create_24_Line"};
            String[] strArr2 = {"search", "add", "group"};
            float[] fArr = {16.800001f, 20.0f, 20.0f};
            int i = 0;
            for (int i2 = 3; i < i2; i2 = 3) {
                final String str = strArr2[i];
                this.icons[i] = ZaloIcons.view(context, strArr[i], fArr[i] * NativeChatList.this.zoom, NativeChatList.this.fg);
                this.icons[i].setOnClickListener(new View.OnClickListener() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda1
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view2) {
                        Header.this.lambda$new$1(str, view2);
                    }
                });
                LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(NativeChatList.this.dp(32.0f), NativeChatList.this.dp(32.0f));
                layoutParams3.leftMargin = NativeChatList.this.dp(4.0f);
                linearLayout.addView(this.icons[i], layoutParams3);
                i++;
            }
            FrameLayout frameLayout = new FrameLayout(context);
            this.tabRow = frameLayout;
            addView(frameLayout, new LinearLayout.LayoutParams(-1, NativeChatList.this.dp(32.0f)));
            LinearLayout linearLayout5 = new LinearLayout(context);
            linearLayout5.setGravity(16);
            linearLayout5.setPadding(NativeChatList.this.dp(8.0f), 0, NativeChatList.this.dp(8.0f), 0);
            frameLayout.addView(linearLayout5, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout linearLayout6 = new LinearLayout(context);
            this.tabs = linearLayout6;
            linearLayout5.addView(linearLayout6, new LinearLayout.LayoutParams(-2, -1));
            linearLayout5.addView(new View(context), new LinearLayout.LayoutParams(0, 0, 1.0f));
            LinearLayout linearLayout7 = new LinearLayout(context);
            linearLayout7.setGravity(16);
            linearLayout7.setOnClickListener(new View.OnClickListener() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    Header.this.lambda$new$2(view2);
                }
            });
            linearLayout5.addView(linearLayout7, new LinearLayout.LayoutParams(-2, -1));
            TextView textView3 = new TextView(context);
            this.cat = textView3;
            textView3.setTextSize(1, NativeChatList.this.zoom * 14.0f);
            textView3.setSingleLine();
            linearLayout7.addView(textView3);
            TextView textViewView = ZaloIcons.view(context, "icon-solid-down", NativeChatList.this.zoom * 16.0f, NativeChatList.this.fg);
            this.chev = textViewView;
            LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(Math.round(NativeChatList.this.dp(16.0f) * NativeChatList.this.zoom), Math.round(NativeChatList.this.dp(16.0f) * NativeChatList.this.zoom));
            layoutParams4.leftMargin = NativeChatList.this.dp(4.0f);
            linearLayout7.addView(textViewView, layoutParams4);
            TextView textViewView2 = ZaloIcons.view(context, "solid-more", NativeChatList.this.zoom * 16.0f, NativeChatList.this.fg);
            this.more = textViewView2;
            textViewView2.setOnClickListener(new View.OnClickListener() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    Header.this.lambda$new$3(view2);
                }
            });
            LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(NativeChatList.this.dp(24.0f), NativeChatList.this.dp(24.0f));
            layoutParams5.leftMargin = NativeChatList.this.dp(8.0f);
            layoutParams5.rightMargin = NativeChatList.this.dp(9.0f);
            linearLayout5.addView(textViewView2, layoutParams5);
            View view2 = new View(context);
            this.indicator = view2;
            view2.setBackgroundColor(-16750337);
            view2.setPivotX(0.0f);
            frameLayout.addView(view2, new FrameLayout.LayoutParams(NativeChatList.this.dp(10.0f), NativeChatList.this.dp(2.0f), 8388691));
            view2.setVisibility(4);
            View view3 = new View(context);
            this.line = view3;
            view3.setVisibility(8);
            addView(view3, new LinearLayout.LayoutParams(-1, 0));
            setCats("Phân loại");
            setConn("wait");
            applyColors();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$0(View view) {
            NativeChatList.this.tap("head", 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$1(String str, View view) {
            NativeChatList.this.tap(str, 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$2(View view) {
            NativeChatList.this.tap("cat", 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$3(View view) {
            NativeChatList.this.tap("more", 0);
        }

        void applyColors() {
            setBackgroundColor(NativeChatList.this.bg);
            this.name.setTextColor(NativeChatList.this.fg);
            this.status.setTextColor(NativeChatList.this.fg2);
            this.cat.setTextColor(NativeChatList.this.fg);
            this.chev.setTextColor(NativeChatList.this.fg);
            this.more.setTextColor(NativeChatList.this.fg);
            int i = 0;
            for (TextView textView : this.icons) {
                textView.setTextColor(NativeChatList.this.fg);
            }
            this.line.setBackgroundColor(0);
            while (i < this.tabs.getChildCount()) {
                ((TextView) this.tabs.getChildAt(i)).setTextColor(i == this.sel ? -16750337 : NativeChatList.this.fg2);
                i++;
            }
        }

        void setMe(String str, final String str2) {
            if (str == null || str.isEmpty()) {
                return;
            }
            if (str.equals(this.me) && str2.equals(this.meAv)) {
                return;
            }
            this.me = str;
            this.meAv = str2;
            this.name.setText(str);
            this.av.setImageBitmap(Avatars.placeholder(str, false, NativeChatList.this.dp(36.0f)));
            if (str2.isEmpty()) {
                return;
            }
            Avatars.load(getContext(), str2, NativeChatList.this.dp(36.0f), new Avatars.Done() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda4
                @Override // com.zola.app.NativeChatList.Avatars.Done
                public final void loaded(Bitmap bitmap) {
                    Header.this.lambda$setMe$4(str2, bitmap);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$setMe$4(String str, Bitmap bitmap) {
            if (bitmap == null || !str.equals(this.meAv)) {
                return;
            }
            this.av.setImageBitmap(bitmap);
        }

        void setConn(String str) {
            int i;
            String str2;
            ObjectAnimator objectAnimator;
            if (str == null || str.equals(this.conn)) {
                return;
            }
            this.conn = str;
            boolean zEquals = "wait".equals(str);
            GradientDrawable gradientDrawable = this.dotBg;
            if ("on".equals(str)) {
                i = -13589396;
            } else {
                i = zEquals ? -678620 : -1750963;
            }
            gradientDrawable.setColor(i);
            TextView textView = this.status;
            if ("on".equals(str)) {
                str2 = "Đã kết nối";
            } else {
                str2 = zEquals ? "Đang kết nối" : "Mất kết nối";
            }
            textView.setText(str2);
            if (zEquals && this.blink == null) {
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.dot, (Property<View, Float>) View.ALPHA, 1.0f, 0.15f);
                this.blink = objectAnimatorOfFloat;
                objectAnimatorOfFloat.setDuration(550L);
                this.blink.setRepeatMode(2);
                this.blink.setRepeatCount(-1);
                this.blink.start();
                return;
            }
            if (zEquals || (objectAnimator = this.blink) == null) {
                return;
            }
            objectAnimator.cancel();
            this.blink = null;
            this.dot.setAlpha(1.0f);
        }

        void setCats(String str) {
            if (str == null || str.isEmpty() || str.equals(this.cats)) {
                return;
            }
            this.cats = str;
            this.cat.setText(str);
        }

        void setTabs(JSONArray jSONArray, int i, boolean z) {
            if (jSONArray == null) {
                return;
            }
            String string = jSONArray.toString();
            if (!string.equals(this.tabs.getTag())) {
                this.tabs.setTag(string);
                this.tabs.removeAllViews();
                z = false;
                for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                    final int i2Final = i2;
                    TextView textView = new TextView(getContext());
                    textView.setText(jSONArray.optString(i2));
                    textView.setGravity(17);
                    textView.setPadding(NativeChatList.this.dp(8.0f), 0, NativeChatList.this.dp(8.0f), 0);
                    textView.setTextSize(1, NativeChatList.this.zoom * 14.0f);
                    textView.setTypeface(Typeface.create("sans-serif-medium", 0));
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Header.this.lambda$setTabs$5(i2Final, view);
                        }
                    });
                    this.tabs.addView(textView, new LinearLayout.LayoutParams(-2, -1));
                }
                NativeChatList.this.tabCount = jSONArray.length();
                this.sel = -1;
            }
            select(i, z);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$setTabs$5(int i, View view) {
            NativeChatList.this.tapTab(i);
        }

        void select(int i, boolean z) {
            if (i < 0 || i >= this.tabs.getChildCount()) {
                return;
            }
            boolean z2 = i != this.sel;
            this.sel = i;
            int i2 = 0;
            while (i2 < this.tabs.getChildCount()) {
                ((TextView) this.tabs.getChildAt(i2)).setTextColor(i2 == this.sel ? -16750337 : NativeChatList.this.fg2);
                i2++;
            }
            if (z2 || this.indicator.getVisibility() != 0) {
                final boolean z3 = z && this.indicator.getVisibility() == 0;
                this.tabs.post(new Runnable() { // from class: com.zola.app.NativeChatList$Header$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        Header.this.lambda$select$6(z3);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX INFO: renamed from: moveIndicator, reason: merged with bridge method [inline-methods] */
        public void lambda$select$6(boolean z) {
            View childAt = this.tabs.getChildAt(this.sel);
            if (childAt == null || childAt.getWidth() == 0) {
                return;
            }
            float left = this.tabs.getLeft() + childAt.getLeft() + NativeChatList.this.dp(8.0f);
            float width = childAt.getWidth() - NativeChatList.this.dp(16.0f);
            float f = this.indicator.getLayoutParams().width;
            this.indicator.setVisibility(0);
            this.indicator.animate().cancel();
            if (!z) {
                this.indicator.setTranslationX(left);
                this.indicator.setScaleX(width / f);
            } else {
                this.indicator.animate().translationX(left).scaleX(width / f).setDuration(320L).setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.25f, 1.0f)).start();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tap(String str, int i) {
        final ZaloWebView zaloWebView;
        if (this.booting) {
            return;
        }
        WebViewHolder.evalMain("window.__zw&&window.__zw.headTap&&window.__zw.headTap('" + str + "'," + i + ")");
        if (!"search".equals(str) || (zaloWebView = this.lastWv) == null) {
            return;
        }
        zaloWebView.requestFocus();
        postDelayed(new Runnable() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                NativeChatList.this.lambda$tap$3(zaloWebView);
            }
        }, 200L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$tap$3(ZaloWebView zaloWebView) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(InputMethodManager.class);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(zaloWebView, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tapTab(int i) {
        Header header;
        if (this.booting || (header = this.header) == null || i == header.sel) {
            return;
        }
        boolean z = i > this.header.sel;
        this.header.select(i, true);
        startTabSlide(z);
        this.tabSel = i;
        tap("tab", i);
    }

    void setConn(String str) {
        Header header = this.header;
        if (header != null) {
            header.setConn(str);
        }
    }

    void onPageReload() {
        this.announced = false;
    }

    private boolean bootLayoutOn() {
        try {
            return this.lastLayout != null && new JSONObject(this.lastLayout).optBoolean("on");
        } catch (Exception unused) {
            return false;
        }
    }

    void cancelBoot() {
        if (this.booting) {
            this.booting = false;
            this.bootEnding = false;
            this.endingBoot = false;
            if (this.columnWidth <= 0) {
                setTranslationZ(0.0f);
                setVisibility(8);
            }
            ZaloWebView zaloWebView = this.lastWv;
            if (zaloWebView != null) {
                zaloWebView.setBackgroundColor(MainActivity.pageBottomColor());
            }
        }
    }

    private void endBoot() {
        if (!this.booting || this.endingBoot) {
            return;
        }
        this.endingBoot = true;
        ZaloWebView zaloWebView = this.lastWv;
        if (zaloWebView != null) {
            zaloWebView.setBackgroundColor(0);
        }
        postDelayed(new Runnable() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NativeChatList.this.finishBoot();
            }
        }, 150L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishBoot() {
        this.endingBoot = false;
        if (this.booting) {
            this.booting = false;
            this.bootEnding = false;
            if (this.columnWidth <= 0) {
                setTranslationZ(0.0f);
            }
            WebViewHolder.evalMain("window.__zw&&window.__zw.connReport&&window.__zw.connReport()");
            String str = this.lastLayout;
            if (str != null) {
                setLayout(str, this.lastWv);
            } else if (this.columnWidth <= 0) {
                setVisibility(8);
            }
            if (this.pendingOpen != null) {
                WebViewHolder.evalMain("window.__zw&&window.__zw.openConv&&window.__zw.openConv(" + JSONObject.quote(this.pendingOpen) + ")");
                this.pendingOpen = null;
            }
        }
    }

    private void startTabSlide(boolean z) {
        int width = this.listView.getWidth();
        int height = this.listView.getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        ValueAnimator valueAnimator = this.slideAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        endTabSlide();
        try {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.drawColor(this.bg);
            this.listView.draw(canvas);
            ImageView imageView = new ImageView(getContext());
            this.slideOld = imageView;
            imageView.setImageBitmap(bitmapCreateBitmap);
            this.slideOld.setScaleType(ImageView.ScaleType.MATRIX);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height, 8388659);
            layoutParams.leftMargin = this.listView.getLeft();
            layoutParams.topMargin = this.listView.getTop();
            this.slideOld.setTranslationY(this.listView.getTranslationY());
            addView(this.slideOld, indexOfChild(this.listView) + 1, layoutParams);
            this.slideForward = z;
            this.listView.setTranslationX(z ? width : -width);
            removeCallbacks(this.slideRun);
            postDelayed(this.slideRun, 250L);
        } catch (OutOfMemoryError unused) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runTabSlide() {
        removeCallbacks(this.slideRun);
        final ImageView imageView = this.slideOld;
        if (imageView != null) {
            ValueAnimator valueAnimator = this.slideAnim;
            if (valueAnimator == null || !valueAnimator.isRunning()) {
                final int width = this.listView.getWidth();
                final float translationX = this.listView.getTranslationX();
                ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.slideAnim = valueAnimatorOfFloat;
                valueAnimatorOfFloat.setDuration(320L);
                this.slideAnim.setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.25f, 1.0f));
                this.slideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda5
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                        NativeChatList.this.lambda$runTabSlide$4(translationX, imageView, width, valueAnimator2);
                    }
                });
                this.slideAnim.addListener(new AnimatorListenerAdapter() { // from class: com.zola.app.NativeChatList.4
                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        NativeChatList.this.listView.setTranslationX(0.0f);
                        NativeChatList.this.endTabSlide();
                    }
                });
                this.slideAnim.start();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runTabSlide$4(float f, ImageView imageView, int i, ValueAnimator valueAnimator) {
        float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.listView.setTranslationX(f * (1.0f - fFloatValue));
        if (this.slideForward) {
            i = -i;
        }
        imageView.setTranslationX(i * fFloatValue);
        this.glass.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void endTabSlide() {
        ImageView imageView = this.slideOld;
        this.slideOld = null;
        if (imageView == null) {
            return;
        }
        removeView(imageView);
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).getBitmap().recycle();
        }
    }

    View dockGlass() {
        return this.glass;
    }

    private void placeGlass(JSONArray jSONArray, boolean z) {
        DockGlass dockGlass = this.glass;
        if (dockGlass == null) {
            return;
        }
        dockGlass.setVisibility((jSONArray == null || Build.VERSION.SDK_INT < 31) ? 8 : 0);
        this.glass.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class DockGlass extends View {
        private final int[] at;
        private final int[] from;

        DockGlass(Context context) {
            super(context);
            this.at = new int[2];
            this.from = new int[2];
            setWillNotDraw(false);
            setOutlineProvider(new ViewOutlineProvider() { // from class: com.zola.app.NativeChatList.DockGlass.1
                @Override // android.view.ViewOutlineProvider
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2.0f);
                }
            });
            setClipToOutline(true);
            if (Build.VERSION.SDK_INT >= 31) {
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(1.7f);
                float f = getResources().getDisplayMetrics().density * 28.0f;
                setRenderEffect(RenderEffect.createChainEffect(RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(colorMatrix)), RenderEffect.createBlurEffect(f, f, Shader.TileMode.CLAMP)));
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(NativeChatList.this.bg);
            NativeChatList.this.listView.getLocationInWindow(this.at);
            getLocationInWindow(this.from);
            int iSave = canvas.save();
            int[] iArr = this.at;
            int i = iArr[0];
            int[] iArr2 = this.from;
            canvas.translate(i - iArr2[0], iArr[1] - iArr2[1]);
            if (NativeChatList.this.content != null && canvas.isHardwareAccelerated()) {
                canvas.drawRenderNode(NativeChatList.this.content);
            }
            canvas.restoreToCount(iSave);
        }
    }

    void setLayout(String str, ZaloWebView zaloWebView) {
        boolean z;
        int i;
        int i2;
        this.lastLayout = str;
        this.lastWv = zaloWebView;
        if (this.booting) {
            if (bootLayoutOn() && zaloWebView != null) {
                zaloWebView.setBackgroundColor(0);
            }
            if (this.bootEnding && bootLayoutOn()) {
                endBoot();
                return;
            }
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject(str);
            boolean zOptBoolean = jSONObject.optBoolean("on");
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("me");
            if (jSONArrayOptJSONArray != null && !jSONArrayOptJSONArray.optString(0).isEmpty() && !jSONArrayOptJSONArray.toString().equals(this.savedMe)) {
                this.savedMe = jSONArrayOptJSONArray.toString();
                Prefs.sp(getContext()).edit().putString(BOOT_ME, this.savedMe).apply();
            }
            if (zOptBoolean && jSONObject.optBoolean("pass") && jSONObject.optInt("top") >= 80 && !str.equals(this.savedBoot)) {
                this.savedBoot = str;
                Prefs.sp(getContext()).edit().putString(BOOT_LAYOUT, str).apply();
            }
            boolean z2 = zOptBoolean && jSONObject.optBoolean("pass", true);
            if (!z2) {
                this.openingAt = 0L;
            }
            int iDp = dp(jSONObject.optInt("top"));
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("dock");
            int css = parseCss(jSONObject.optString("bg"));
            int css2 = parseCss(jSONObject.optString("fg"));
            int css3 = parseCss(jSONObject.optString("fg2"));
            if ((css != 0 && css != this.bg) || ((css2 != 0 && css2 != this.fg) || (css3 != 0 && css3 != this.fg2))) {
                applyColors(css, css2, css3);
            }
            JSONArray jSONArrayOptJSONArray3 = jSONObject.optJSONArray("tabs");
            int iOptInt = jSONArrayOptJSONArray3 == null ? -1 : jSONArrayOptJSONArray3.optInt(1, -1);
            if (jSONArrayOptJSONArray != null) {
                this.header.setMe(jSONArrayOptJSONArray.optString(0), jSONArrayOptJSONArray.optString(1));
            }
            if (jSONArrayOptJSONArray3 != null) {
                this.header.setTabs(jSONArrayOptJSONArray3.optJSONArray(0), iOptInt, true);
            }
            this.header.setCats(jSONObject.optString("cat"));
            String strValueOf = jSONArrayOptJSONArray3 == null ? "" : String.valueOf(jSONArrayOptJSONArray3.optJSONArray(0));
            if (zOptBoolean && z2 && getVisibility() == 0 && (i2 = this.tabSel) >= 0 && iOptInt >= 0 && iOptInt != i2 && strValueOf.equals(this.tabNames)) {
                z = zOptBoolean;
                if (SystemClock.uptimeMillis() - this.shownAt > 1500) {
                    startTabSlide(iOptInt > this.tabSel);
                }
            } else {
                z = zOptBoolean;
            }
            if (iOptInt >= 0) {
                this.tabSel = iOptInt;
                this.tabNames = strValueOf;
            }
            if (z && getVisibility() != 0) {
                this.shownAt = SystemClock.uptimeMillis();
            }
            int sw = jSONObject.optInt("sw", 0);
            setColumnWidthPx(sw > 0 ? dp(sw) : 0);
            setVisibility((z || this.columnWidth > 0) ? 0 : 8);
            if (this.columnWidth > 0) setTranslationZ(1000.0f);
            int[] iArr = null;
            placeGlass(z ? jSONArrayOptJSONArray2 : null, jSONObject.optBoolean("dh"));
            if (zaloWebView != null) {
                zaloWebView.passTop = z2 ? 0 : -1;
                zaloWebView.passRight = this.columnWidth;
                if (!z || jSONArrayOptJSONArray2 == null) {
                    i = 0;
                } else {
                    i = 0;
                    iArr = new int[]{dp(jSONArrayOptJSONArray2.optInt(0)), dp(jSONArrayOptJSONArray2.optInt(1)), dp(jSONArrayOptJSONArray2.optInt(2)), dp(jSONArrayOptJSONArray2.optInt(3))};
                }
                zaloWebView.passExclude = iArr;
                zaloWebView.setBackgroundColor((z || this.columnWidth > 0) ? i : MainActivity.pageBottomColor());
            }
            if (z) {
                animateTop(iDp);
            }
        } catch (Exception e) {
            Log.w("Zola", "bad native layout", e);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.columnWidth > 0) {
            float stroke = Math.max(1.0f, this.density);
            boolean dark = Color.luminance(this.bg) < 0.4f;
            this.colBorderPaint.setColor(dark ? 0x2EFFFFFF : 0x22000000);
            float right = getWidth();
            canvas.drawRect(right - stroke, 0.0f, right, getHeight(), this.colBorderPaint);
        }
    }

    public void setColumnWidthPx(int newColW) {
        if (this.columnWidth != newColW) {
            this.columnWidth = newColW;
            ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp != null) {
                lp.width = newColW > 0 ? newColW : ViewGroup.LayoutParams.MATCH_PARENT;
                setLayoutParams(lp);
            }
            invalidate();
        }
    }

    public int getColumnWidth() {
        return this.columnWidth;
    }

    public void setSelectedId(String id) {
        if (!TextUtils.equals(this.selectedId, id)) {
            this.selectedId = id;
            if (this.adapter != null) {
                this.adapter.notifyDataSetChanged();
            }
        }
    }

    private void animateTop(int i) {
        if (i > this.fullTop) {
            this.fullTop = i;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.listView.getLayoutParams();
            layoutParams.bottomMargin = -this.fullTop;
            this.listView.setLayoutParams(layoutParams);
            ListView listView = this.listView;
            int i2 = this.fullTop;
            listView.setPadding(0, i2, 0, dp(Prefs.dockBottom(getContext()) + 64) + i2);
        }
        if (i == this.topPx) {
            return;
        }
        this.topPx = i;
        float f = i - this.fullTop;
        float fMax = Math.max(-dp(64.0f), Math.min(0, i - (this.header.getHeight() > 0 ? this.header.getHeight() : dp(96.0f))));
        this.listView.animate().cancel();
        this.header.animate().cancel();
        if (getVisibility() != 0) {
            this.listView.setTranslationY(f);
            this.header.setTranslationY(fMax);
        } else {
            PathInterpolator pathInterpolator = new PathInterpolator(0.22f, 0.61f, 0.36f, 1.0f);
            this.listView.animate().translationY(f).setDuration(420L).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.zola.app.NativeChatList$$ExternalSyntheticLambda6
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    NativeChatList.this.lambda$animateTop$5(valueAnimator);
                }
            }).setInterpolator(pathInterpolator).start();
            this.header.animate().translationY(fMax).setDuration(420L).setInterpolator(pathInterpolator).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animateTop$5(ValueAnimator valueAnimator) {
        this.glass.invalidate();
    }

    private static int parseCss(String str) {
        if (str == null || str.isEmpty() || "transparent".equalsIgnoreCase(str)) {
            return 0;
        }
        Matcher matcher = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*([\\d.]+))?").matcher(str);
        if (matcher.find()) {
            String alphaStr = matcher.group(4);
            if (alphaStr != null) {
                try {
                    float a = Float.parseFloat(alphaStr);
                    if (a <= 0.05f) return 0;
                } catch (Exception ignored) {}
            }
            return Color.rgb(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        return 0;
    }

    private void applyColors(int i, int i2, int i3) {
        if (i != 0) {
            this.bg = i;
        } else if (this.bg == 0 || this.bg == -1) {
            boolean dark = (getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            this.bg = dark ? 0xFF22262B : 0xFFFFFFFF;
        }
        boolean z = Color.luminance(this.bg) < 0.4f;
        if (i2 == 0) {
            i2 = z ? -1644309 : -16245958;
        }
        this.fg = i2;
        if (i3 == 0) {
            i3 = z ? -6642510 : -10851967;
        }
        this.fg2 = i3;
        setBackgroundColor(this.bg);
        this.adapter.notifyDataSetChanged();
        Header header = this.header;
        if (header != null) {
            header.applyColors();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int iconRes(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        if (str.contains("picture") || str.contains("doodle")) {
            return R.drawable.zd_ic_image;
        }
        if (str.contains("video")) {
            return R.drawable.zd_ic_video;
        }
        if (str.contains("file") || str.contains("folder")) {
            return R.drawable.zd_ic_file;
        }
        if (str.contains("sticker") || str.equals("combined-shape")) {
            return R.drawable.zd_ic_sticker;
        }
        if (str.contains("link")) {
            return R.drawable.zd_ic_link;
        }
        if (str.contains("voice")) {
            return R.drawable.zd_ic_mic;
        }
        if (str.contains("Call")) {
            return R.drawable.zd_ic_phone;
        }
        if (str.contains("location")) {
            return R.drawable.zd_ic_location;
        }
        return 0;
    }

    private final class Adapter extends BaseAdapter {
        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        private Adapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return NativeChatList.this.items.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return NativeChatList.this.items.get(i);
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            Row row;
            if (view instanceof Row) {
                row = (Row) view;
            } else {
                NativeChatList nativeChatList = NativeChatList.this;
                row = nativeChatList.new Row(nativeChatList.getContext());
            }
            row.bind((Item) NativeChatList.this.items.get(i));
            return row;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Row extends LinearLayout {
        final ImageView avatar;
        final TextView badge;
        String boundAvatar;
        String boundId;
        boolean hasRealAvatar;
        final ImageView msgIc;
        final ImageView mutedIc;
        final TextView name;
        final ImageView pinIc;
        final TextView text;
        final TextView time;
        private final Paint divPaint = new Paint(1);

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            float h = getHeight();
            float stroke = Math.max(1.0f, NativeChatList.this.density * 0.6f);
            boolean dark = Color.luminance(NativeChatList.this.bg) < 0.4f;
            this.divPaint.setColor(dark ? 0x30FFFFFF : 0x26000000);
            canvas.drawRect(NativeChatList.this.dp(76.0f), h - stroke, getWidth(), h, this.divPaint);
        }

        void setAvatar(Bitmap bitmap) {
            Drawable drawable = this.avatar.getDrawable();
            if (this.hasRealAvatar && drawable != null && isShown()) {
                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{drawable, new BitmapDrawable(getResources(), bitmap)});
                transitionDrawable.setCrossFadeEnabled(true);
                this.avatar.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(200);
            } else {
                this.avatar.setImageBitmap(bitmap);
            }
            this.hasRealAvatar = true;
        }

        private ImageView icon(Context context, int i, int i2) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(i);
            float f = i2;
            imageView.setLayoutParams(new LinearLayout.LayoutParams(NativeChatList.this.dp(f), NativeChatList.this.dp(f)));
            return imageView;
        }

        Row(Context context) {
            super(context);
            setOrientation(0);
            setGravity(16);
            setPadding(NativeChatList.this.dp(16.0f), NativeChatList.this.dp(4.0f), NativeChatList.this.dp(16.0f), NativeChatList.this.dp(4.0f));
            setMinimumHeight(NativeChatList.this.dp(78.0f));
            ImageView imageView = new ImageView(context);
            this.avatar = imageView;
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            addView(imageView, new LinearLayout.LayoutParams(NativeChatList.this.dp(48.0f), NativeChatList.this.dp(48.0f)));
            ImageView imageViewIcon = icon(context, R.drawable.zd_ic_muted, 14);
            this.mutedIc = imageViewIcon;
            ImageView imageViewIcon2 = icon(context, R.drawable.zd_ic_image, 16);
            this.msgIc = imageViewIcon2;
            ImageView imageViewIcon3 = icon(context, R.drawable.zd_ic_pin, 16);
            this.pinIc = imageViewIcon3;
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(1);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
            layoutParams.leftMargin = NativeChatList.this.dp(12.0f);
            addView(linearLayout, layoutParams);
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setGravity(16);
            TextView textViewText = text(context, 16);
            this.name = textViewText;
            textViewText.setSingleLine();
            textViewText.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout2.addView(textViewText, new LinearLayout.LayoutParams(0, -2, 1.0f));
            ((LinearLayout.LayoutParams) imageViewIcon.getLayoutParams()).leftMargin = NativeChatList.this.dp(6.0f);
            linearLayout2.addView(imageViewIcon);
            TextView textViewText2 = text(context, 12);
            this.time = textViewText2;
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
            layoutParams2.leftMargin = NativeChatList.this.dp(8.0f);
            linearLayout2.addView(textViewText2, layoutParams2);
            linearLayout.addView(linearLayout2);
            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setGravity(16);
            ((LinearLayout.LayoutParams) imageViewIcon2.getLayoutParams()).rightMargin = NativeChatList.this.dp(4.0f);
            linearLayout3.addView(imageViewIcon2);
            TextView textViewText3 = text(context, 14);
            this.text = textViewText3;
            textViewText3.setSingleLine();
            textViewText3.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout3.addView(textViewText3, new LinearLayout.LayoutParams(0, -2, 1.0f));
            ((LinearLayout.LayoutParams) imageViewIcon3.getLayoutParams()).leftMargin = NativeChatList.this.dp(8.0f);
            linearLayout3.addView(imageViewIcon3);
            TextView textViewText4 = text(context, 11);
            this.badge = textViewText4;
            textViewText4.setTextColor(-1);
            textViewText4.setGravity(17);
            textViewText4.setMinWidth(NativeChatList.this.dp(20.0f));
            textViewText4.setPadding(NativeChatList.this.dp(6.0f), 0, NativeChatList.this.dp(6.0f), 0);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, NativeChatList.this.dp(20.0f));
            layoutParams3.leftMargin = NativeChatList.this.dp(8.0f);
            linearLayout3.addView(textViewText4, layoutParams3);
            LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams4.topMargin = NativeChatList.this.dp(4.0f);
            linearLayout.addView(linearLayout3, layoutParams4);
        }

        private TextView text(Context context, int i) {
            TextView textView = new TextView(context);
            textView.setTextSize(1, i * NativeChatList.this.zoom);
            textView.setIncludeFontPadding(false);
            return textView;
        }

        void bind(Item item) {
            String strValueOf;
            int i;
            if (NativeChatList.this.columnWidth > 0 && TextUtils.equals(NativeChatList.this.selectedId, item.id)) {
                boolean dark = Color.luminance(NativeChatList.this.bg) < 0.4f;
                setBackgroundColor(dark ? 0x26FFFFFF : 0x140068FF);
            } else {
                setBackgroundColor(0);
            }
            int i2 = (item.unread > 0 || item.mention > 0) ? 1 : 0;
            this.name.setText(item.name);
            this.name.setTextColor(NativeChatList.this.fg);
            this.name.setTypeface(Typeface.DEFAULT, i2);
            this.time.setText(NativeChatList.formatTime(item.time));
            TextView textView = this.time;
            NativeChatList nativeChatList = NativeChatList.this;
            textView.setTextColor(i2 != 0 ? nativeChatList.fg : nativeChatList.fg2);
            this.mutedIc.setVisibility(item.muted ? 0 : 8);
            this.mutedIc.setColorFilter(NativeChatList.this.fg2);
            int iIconRes = NativeChatList.iconRes(item.icon);
            this.msgIc.setVisibility(iIconRes != 0 ? 0 : 8);
            if (iIconRes != 0) {
                this.msgIc.setImageResource(iIconRes);
                ImageView imageView = this.msgIc;
                if (item.missCall) {
                    i = -1755077;
                } else {
                    NativeChatList nativeChatList2 = NativeChatList.this;
                    i = i2 != 0 ? nativeChatList2.fg : nativeChatList2.fg2;
                }
                imageView.setColorFilter(i);
            }
            this.pinIc.setVisibility((item.pin <= 0 || i2 != 0) ? 8 : 0);
            this.pinIc.setColorFilter(NativeChatList.this.fg2);
            this.text.setText(item.text);
            TextView textView2 = this.text;
            NativeChatList nativeChatList3 = NativeChatList.this;
            textView2.setTextColor(i2 != 0 ? nativeChatList3.fg : nativeChatList3.fg2);
            this.text.setTypeface(Typeface.DEFAULT, (i2 == 0 || item.muted) ? 0 : 1);
            if (i2 != 0) {
                this.badge.setVisibility(0);
                TextView textView3 = this.badge;
                if (item.mention > 0) {
                    strValueOf = "@";
                } else {
                    strValueOf = item.unread > 5 ? "5+" : String.valueOf(item.unread);
                }
                textView3.setText(strValueOf);
                this.badge.setMinWidth(NativeChatList.this.dp(20.0f));
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setCornerRadius(NativeChatList.this.dp(10.0f));
                gradientDrawable.setColor(item.muted ? -6642510 : -1755077);
                this.badge.setBackground(gradientDrawable);
            } else if (item.unreadDot > 0) {
                this.badge.setVisibility(0);
                this.badge.setText("");
                this.badge.setMinWidth(NativeChatList.this.dp(10.0f));
                GradientDrawable gradientDrawable2 = new GradientDrawable();
                gradientDrawable2.setShape(1);
                gradientDrawable2.setSize(NativeChatList.this.dp(10.0f), NativeChatList.this.dp(10.0f));
                gradientDrawable2.setColor(-1755077);
                this.badge.setBackground(gradientDrawable2);
            } else {
                this.badge.setVisibility(8);
            }
            String str = item.id + item.avatar + (item.members.length > 0 ? String.join(",", item.members) + item.total + NativeChatList.this.bg : "");
            if (TextUtils.equals(this.boundAvatar, str)) {
                return;
            }
            boolean z = TextUtils.equals(this.boundId, item.id) && this.hasRealAvatar;
            this.boundAvatar = str;
            this.boundId = item.id;
            if (!z) {
                this.avatar.setImageBitmap(Avatars.placeholder(item.name, item.me, NativeChatList.this.dp(48.0f)));
                this.hasRealAvatar = false;
            }
            if (item.group && TextUtils.isEmpty(item.avatar) && item.members.length > 0) {
                final String str2 = this.boundAvatar;
                boolean z2 = Color.luminance(NativeChatList.this.bg) < 0.4f;
                Avatars.loadGroup(getContext(), item.members, item.total, NativeChatList.this.dp(48.0f), NativeChatList.this.bg, z2 ? -12959930 : -1709585, z2 ? -1644309 : -10851967, new Avatars.Done() { // from class: com.zola.app.NativeChatList$Row$$ExternalSyntheticLambda0
                    @Override // com.zola.app.NativeChatList.Avatars.Done
                    public final void loaded(Bitmap bitmap) {
                        Row.this.lambda$bind$0(str2, bitmap);
                    }
                });
            } else {
                if (TextUtils.isEmpty(item.avatar)) {
                    return;
                }
                final String str3 = this.boundAvatar;
                Avatars.load(getContext(), item.avatar, NativeChatList.this.dp(48.0f), new Avatars.Done() { // from class: com.zola.app.NativeChatList$Row$$ExternalSyntheticLambda1
                    @Override // com.zola.app.NativeChatList.Avatars.Done
                    public final void loaded(Bitmap bitmap) {
                        Row.this.lambda$bind$1(str3, bitmap);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bind$0(String str, Bitmap bitmap) {
            if (bitmap != null && TextUtils.equals(str, this.boundAvatar)) {
                setAvatar(bitmap);
            }
            NativeChatList.this.glass.invalidate();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bind$1(String str, Bitmap bitmap) {
            if (bitmap != null && TextUtils.equals(str, this.boundAvatar)) {
                setAvatar(bitmap);
            }
            NativeChatList.this.glass.invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String formatTime(long j) {
        if (j <= 0) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j);
        long timeInMillis = calendar.getTimeInMillis() - j;
        if (timeInMillis < 60000) {
            return "Vài giây";
        }
        if (timeInMillis < 3600000) {
            return (timeInMillis / 60000) + " phút";
        }
        if (calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6)) {
            return (timeInMillis / 3600000) + " giờ";
        }
        Calendar calendar3 = (Calendar) calendar.clone();
        Calendar calendar4 = (Calendar) calendar2.clone();
        Calendar[] calendarArr = {calendar3, calendar4};
        for (int i = 0; i < 2; i++) {
            Calendar calendar5 = calendarArr[i];
            calendar5.set(11, 0);
            calendar5.set(12, 0);
            calendar5.set(13, 0);
            calendar5.set(14, 0);
        }
        long jRound = Math.round((calendar3.getTimeInMillis() - calendar4.getTimeInMillis()) / 8.64E7d);
        if (jRound <= 1) {
            return "Hôm qua";
        }
        if (jRound < 7) {
            return jRound + " ngày";
        }
        return new SimpleDateFormat("dd/MM", Locale.US).format(new Date(j));
    }

    static final class Avatars {
        private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(8388608) { // from class: com.zola.app.NativeChatList.Avatars.1
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public int sizeOf(String str, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
        private static final Map<String, List<Done>> WAITING = new HashMap();
        private static final int[] COLORS = {-11563273, -13715843, -877256, -5219088, -1746851, -13913152};

        interface Done {
            void loaded(Bitmap bitmap);
        }

        Avatars() {
        }

        static void load(Context context, final String str, final int i, Done done) {
            final String str2 = str + "@" + i;
            Bitmap bitmap = MEM.get(str2);
            if (bitmap != null) {
                done.loaded(bitmap);
                return;
            }
            Map<String, List<Done>> map = WAITING;
            synchronized (map) {
                List<Done> list = map.get(str2);
                if (list != null) {
                    list.add(done);
                    return;
                }
                ArrayList arrayList = new ArrayList();
                arrayList.add(done);
                map.put(str2, arrayList);
                final Context applicationContext = context.getApplicationContext();
                App.IO.execute(new Runnable() { // from class: com.zola.app.NativeChatList$Avatars$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        NativeChatList.Avatars.lambda$load$1(applicationContext, str, i, str2);
                    }
                });
            }
        }

        /* JADX WARN: Code duplicated, block: B:62:0x007b A[EXC_TOP_SPLITTER, SYNTHETIC] */
        static /* synthetic */ void lambda$load$1(Context context, String str, int i, String str2) {
            final List<Done> listRemove;
            Bitmap bitmapCircle = null;
            try {
                File file = new File(context.getCacheDir(), "avatars");
                file.mkdirs();
                File file2 = new File(file, Integer.toHexString(str.hashCode()) + ".img");
                if (!file2.exists()) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
                    httpURLConnection.setConnectTimeout(10000);
                    httpURLConnection.setReadTimeout(15000);
                    try (InputStream inputStream = httpURLConnection.getInputStream();
                         FileOutputStream fileOutputStream = new FileOutputStream(file2)) {
                        byte[] bArr = new byte[16384];
                        int i2;
                        while ((i2 = inputStream.read(bArr)) > 0) {
                            fileOutputStream.write(bArr, 0, i2);
                        }
                    } finally {
                        httpURLConnection.disconnect();
                    }
                }
                Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(file2.getPath());
                if (bitmapDecodeFile != null) {
                    bitmapCircle = circle(bitmapDecodeFile, i);
                } else {
                    file2.delete();
                }
            } catch (Exception e) {
                Log.d("Zola", "avatar load failed", e);
            }
            if (bitmapCircle != null) {
                MEM.put(str2, bitmapCircle);
            }
            Map<String, List<Done>> map = WAITING;
            synchronized (map) {
                listRemove = map.remove(str2);
            }
            final Bitmap finalCircle = bitmapCircle;
            App.MAIN.post(new Runnable() {
                @Override
                public void run() {
                    NativeChatList.Avatars.lambda$load$0(listRemove, finalCircle);
                }
            });
        }

        static /* synthetic */ void lambda$load$0(List list, Bitmap bitmap) {
            if (list != null) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    ((Done) it.next()).loaded(bitmap);
                }
            }
        }

        static void loadGroup(Context context, String[] strArr, final int i, final int i2, final int i3, final int i4, final int i5, final Done done) {
            int iMin = Math.min(3, strArr.length);
            final int iRound = Math.round(i2 * 0.54f);
            final Bitmap[] bitmapArr = new Bitmap[iMin];
            final int[] iArr = {iMin};
            final Runnable runnable = new Runnable() { // from class: com.zola.app.NativeChatList$Avatars$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    done.loaded(NativeChatList.Avatars.collage(bitmapArr, i, i2, iRound, i3, i4, i5));
                }
            };
            if (iMin == 0) {
                runnable.run();
                return;
            }
            for (int i6 = 0; i6 < iMin; i6++) {
                final int i6Final = i6;
                load(context, strArr[i6], iRound, new Done() {
                    @Override
                    public void loaded(Bitmap bitmap) {
                        NativeChatList.Avatars.lambda$loadGroup$3(bitmapArr, i6Final, iArr, runnable, bitmap);
                    }
                });
            }
        }

        static /* synthetic */ void lambda$loadGroup$3(Bitmap[] bitmapArr, int i, int[] iArr, Runnable runnable, Bitmap bitmap) {
            bitmapArr[i] = bitmap;
            int i2 = iArr[0] - 1;
            iArr[0] = i2;
            if (i2 == 0) {
                runnable.run();
            }
        }

        private static Bitmap collage(Bitmap[] bitmapArr, int i, int i2, int i3, int i4, int i5, int i6) {
            int[][] iArr;
            float f;
            Bitmap bitmap;
            int i7 = i3;
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i2, i2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint(3);
            char c = 1;
            char c2 = 0;
            int length = bitmapArr.length + (i > bitmapArr.length ? 1 : 0);
            int i8 = i2 - i7;
            if (length <= 1) {
                int i9 = i8 / 2;
                iArr = new int[][]{new int[]{i9, i9}};
            } else if (length == 2) {
                iArr = new int[][]{new int[]{0, 0}, new int[]{i8, i8}};
            } else if (length == 3) {
                iArr = new int[][]{new int[]{i8 / 2, 0}, new int[]{0, i8}, new int[]{i8, i8}};
            } else {
                iArr = new int[][]{new int[]{0, 0}, new int[]{i8, 0}, new int[]{0, i8}, new int[]{i8, i8}};
            }
            float fMax = Math.max(1.5f, i2 / 32.0f);
            int i10 = 0;
            while (i10 < length && i10 < iArr.length) {
                int[] iArr2 = iArr[i10];
                float f2 = i7 / 2.0f;
                float f3 = iArr2[c2] + f2;
                float f4 = iArr2[c] + f2;
                paint.setColor(i4);
                canvas.drawCircle(f3, f4, f2, paint);
                float f5 = f2 - fMax;
                if (i10 < bitmapArr.length && (bitmap = bitmapArr[i10]) != null) {
                    f = fMax;
                    canvas.drawBitmap(bitmap, (Rect) null, new RectF(f3 - f5, f4 - f5, f3 + f5, f4 + f5), paint);
                } else {
                    f = fMax;
                    paint.setColor(i10 < bitmapArr.length ? -3879721 : i5);
                    canvas.drawCircle(f3, f4, f5, paint);
                    if (i10 >= bitmapArr.length) {
                        paint.setColor(i6);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);
                        String strValueOf = i > 99 ? "99+" : String.valueOf(i);
                        paint.setTextSize(f5 * (strValueOf.length() > 2 ? 0.75f : 0.95f));
                        canvas.drawText(strValueOf, f3, f4 - ((paint.descent() + paint.ascent()) / 2.0f), paint);
                    }
                    i10++;
                    fMax = f;
                    i7 = i3;
                    c = 1;
                    c2 = 0;
                }
                i10++;
                fMax = f;
                i7 = i3;
                c = 1;
                c2 = 0;
            }
            return bitmapCreateBitmap;
        }

        private static Bitmap circle(Bitmap bitmap, int i) {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Path path = new Path();
            float f = i / 2.0f;
            path.addCircle(f, f, f, Path.Direction.CW);
            canvas.clipPath(path);
            int iMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bitmap, new Rect((bitmap.getWidth() - iMin) / 2, (bitmap.getHeight() - iMin) / 2, (bitmap.getWidth() + iMin) / 2, (bitmap.getHeight() + iMin) / 2), new Rect(0, 0, i, i), new Paint(2));
            return bitmapCreateBitmap;
        }

        static Bitmap placeholder(String str, boolean z, int i) {
            int i2;
            String str2 = "ph:" + str + z + i;
            LruCache<String, Bitmap> lruCache = MEM;
            Bitmap bitmap = lruCache.get(str2);
            if (bitmap != null) {
                return bitmap;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint(1);
            if (z) {
                i2 = -16750337;
            } else {
                int[] iArr = COLORS;
                i2 = iArr[Math.abs(str.hashCode()) % iArr.length];
            }
            paint.setColor(i2);
            float f = i;
            float f2 = f / 2.0f;
            canvas.drawCircle(f2, f2, f2, paint);
            paint.setColor(-1);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(f * 0.38f);
            canvas.drawText(z ? "☁" : initials(str), f2, f2 - ((paint.descent() + paint.ascent()) / 2.0f), paint);
            lruCache.put(str2, bitmapCreateBitmap);
            return bitmapCreateBitmap;
        }

        private static String initials(String str) {
            String[] strArrSplit = str.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String str2 : strArrSplit) {
                if (!str2.isEmpty() && Character.isLetterOrDigit(str2.codePointAt(0))) {
                    sb.appendCodePoint(Character.toUpperCase(str2.codePointAt(0)));
                    if (sb.length() >= 2) {
                        break;
                    }
                }
            }
            if (sb.length() == 0 && !str.isEmpty()) {
                sb.appendCodePoint(str.codePointAt(0));
            }
            return sb.toString();
        }
    }
}
