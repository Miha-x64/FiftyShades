package net.aquadc.fiftyshades;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Property;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;
import static net.aquadc.fiftyshades.ViewDrawablePool.scrapUnused;
import static net.aquadc.fiftyshades.ViewDrawablePool.unsafeDrawableFor;

/**
 * ItemDecoration which draws a round rect with a shadow below each item.
 */
@RequiresApi(11) public final class RectItemsWithShadows extends RecyclerView.ItemDecoration {

    private final Shadow.ShadowState factory;
    private final RectSpec rect;
    private final ShadowSpec shadow;
    private final Paint paint = new Paint();
    private final SparseArray<Shadow> drawables = new SparseArray<>();
    private final ArrayList<Shadow> scrap = new ArrayList<>(0);
    public RectItemsWithShadows(@NonNull RectSpec rect, @NonNull ShadowSpec shadow, boolean inner) {
        this.factory = new Shadow.ShadowState(0, 0f, 0f, 0f, 0, inner);
        this.rect = rect;
        this.shadow = shadow;
    }
    public RectItemsWithShadows(@NonNull RectSpec rect, @NonNull ShadowSpec shadow) {
        this(rect, shadow, false);
    }

    private final RectF bounds = new RectF();
    @Override public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        // prepare all the drawables
        normalize(parent);

        // first pass: draw outer shadows, they could overlap each other but must not overlap fill or stroke
        if (!factory.inner) drawOuter(c, parent);

        // second pass: draw fill, inner shadow, and stroke
        drawRemaining(c, parent);
    }

    private void normalize(RecyclerView parent) {
        long usedDrawables = 0;
        for (int i = 0, children = parent.getChildCount(); i < children; i++) {
            View v = parent.getChildAt(i);

            ShadowSpec viewShadow = (ShadowSpec) v.getTag(R.id.fiftyShades_decorShadowSpec);
            if (viewShadow != null) fix(viewShadow);

            RectSpec viewRect = (RectSpec) v.getTag(R.id.fiftyShades_decorRectSpec);
            if (viewRect != null) fix(viewRect);

            int iof = drawables.indexOfKey(System.identityHashCode(v));
            if (iof >= 0) usedDrawables |= 1L << iof; // on hash collision we'll just mark same index twice
            // thus, popCount(usedDrawables) <= children
        }
        scrapUnused(drawables, scrap, usedDrawables);
    }
    private void drawOuter(Canvas c, RecyclerView parent) {
        for (int i = 0, children = parent.getChildCount(); i < children; i++) {
            View v = parent.getChildAt(i);
            ShadowSpec viewShadow = (ShadowSpec) v.getTag(R.id.fiftyShades_decorShadowSpec);
            if (viewShadow == null) viewShadow = shadow;

            RectSpec viewRect = (RectSpec) v.getTag(R.id.fiftyShades_decorRectSpec);
            if (viewRect == null) viewRect = rect;

            // draw shadow below, if outer
            if ((viewShadow.color >>> 24) != 0) {
                gatherBoundsOf(v);
                Shadow drawable = unsafeDrawableFor(drawables, scrap, factory, v);
                drawShadow(c, drawable, viewRect.cornerRadius, viewShadow, (int) (v.getAlpha() * 255));
            }
        }
    }
    private void drawRemaining(Canvas c, RecyclerView parent) {
        for (int i = 0, children = parent.getChildCount(); i < children; i++) {
            View v = parent.getChildAt(i);

            RectSpec viewRect = (RectSpec) v.getTag(R.id.fiftyShades_decorRectSpec);
            if (viewRect == null) viewRect = rect;

            ShadowSpec viewShadow = null;
            boolean fill = viewRect.fillColor >>> 24 != 0;
            boolean inner = factory.inner &&
                (((viewShadow = (ShadowSpec) v.getTag(R.id.fiftyShades_decorShadowSpec)) == null
                    ? viewShadow = shadow : viewShadow
                ).color >>> 24) != 0;
            boolean stroke = viewRect.hasVisibleStroke();
            if (fill || inner || stroke) {
                gatherBoundsOf(v);
                int alpha = (int) (v.getAlpha() * 255);

                paint.setAntiAlias(viewRect.cornerRadius > 0);
                if (fill)
                    fill(c, alpha, viewRect.fillColor, viewRect.cornerRadius);

                // draw shadow above, if inner
                if (inner) {
                    Shadow drawable = unsafeDrawableFor(drawables, scrap, factory, v);
                    drawShadow(c, drawable, viewRect.cornerRadius, viewShadow, alpha);
                }

                // draw stroke above inner shadow
                if (stroke)
                    stroke(c, alpha, viewRect.strokeColor, viewRect.strokeWidth, viewRect.cornerRadius);
            }
        }
    }
    private void fix(ShadowSpec sh) {
        if (Float.isNaN(sh.dx)) sh.dx = shadow.dx;
        if (Float.isNaN(sh.dy)) sh.dy = shadow.dy;
        if (Float.isNaN(sh.radius)) sh.radius = shadow.radius;
        if (sh.color == 1) sh.color = shadow.color;
    }
    private void fix(RectSpec r) {
        if (r.fillColor == 1) r.fillColor = rect.fillColor;
        if (r.cornerRadius == Integer.MIN_VALUE) r.cornerRadius = rect.cornerRadius;
        if (r.strokeColor == 1) r.strokeColor = rect.strokeColor;
        if (Float.isNaN(r.strokeWidth)) r.strokeWidth = rect.strokeWidth;
    }
    private void gatherBoundsOf(View v) {
        bounds.left = v.getX();
        bounds.top = v.getY();
        bounds.right = bounds.left + v.getWidth();
        bounds.bottom = bounds.top + v.getHeight();
    }
    private void fill(Canvas c, int alpha, int color, int cornerRadius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(color, alpha));
        c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
    private void stroke(Canvas c, int alpha, int strokeColor, float strokeWidth, int cornerRadius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(withAlpha(strokeColor, alpha));
        paint.setStrokeWidth(strokeWidth);
        c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
    private static int withAlpha(int color, int alpha) {
        return ((Color.alpha(color) * alpha / 255) << 24) | (0xFFFFFF & color);
    }
    private void drawShadow(Canvas c, Shadow drawable, int cornerRadius, ShadowSpec viewShadow, int alpha) {
        drawable.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
        drawable.setAlpha(alpha);
        drawable.cornerRadius(cornerRadius)
            .shadow(viewShadow)
            .draw(c);
    }

    private static final RectSpec DEFAULT_RECT = new RectSpec(Color.TRANSPARENT, 0);
    private static final ShadowSpec DEFAULT_SHADOW = new ShadowSpec(0f, 0f, 0f, Color.TRANSPARENT);
    static int get_(View view, int at) {
        if (at < 4) {
            RectSpec rect = (RectSpec) view.getTag(R.id.fiftyShades_decorRectSpec);
            if (rect == null) rect = DEFAULT_RECT;
            switch (at) {
                case 0: return rect.fillColor;
                case 1: return rect.cornerRadius;
                case 2: return rect.strokeColor;
                case 3: return floatToRawIntBits(rect.strokeWidth);
                default: throw new AssertionError();
            }
        } else {
            ShadowSpec shadow = (ShadowSpec) view.getTag(R.id.fiftyShades_decorShadowSpec);
            if (shadow == null) shadow = DEFAULT_SHADOW;
            switch (at) {
                case 4: return floatToRawIntBits(shadow.dx);
                case 5: return floatToRawIntBits(shadow.dy);
                case 6: return floatToRawIntBits(shadow.radius);
                case 7: return shadow.color;
                default: throw new AssertionError();
            }
        }
    }
    static void set_(View view, int at, int value) {
        if (at < 4) {
            RectSpec rect = (RectSpec) view.getTag(R.id.fiftyShades_decorRectSpec);
            if (rect == null) view.setTag(R.id.fiftyShades_decorRectSpec, rect = invalidRectSpec());
            switch (at) {
                case 0: rect.fillColor = value; break;
                case 1: rect.cornerRadius = value; break;
                case 2: rect.strokeColor = value; break;
                case 3: rect.strokeWidth = intBitsToFloat(value); break;
                default: throw new AssertionError();
            }
        } else {
            ShadowSpec shadow = (ShadowSpec) view.getTag(R.id.fiftyShades_decorShadowSpec);
            if (shadow == null) view.setTag(R.id.fiftyShades_decorShadowSpec, shadow = invalidShadowSpec());
            switch (at) {
                case 4: shadow.dx = intBitsToFloat(value); break;
                case 5: shadow.dy = intBitsToFloat(value); break;
                case 6: shadow.radius = intBitsToFloat(value); break;
                case 7: shadow.color = value; break;
                default: throw new AssertionError();
            }
        }
        View parent = (View) view.getParent();
        if (parent != null) parent.invalidate();
        // invalidateItemDecorations would invalidate offsets and relayout, we don't need this
    }
    private static RectSpec invalidRectSpec() {
        RectSpec rect;
        rect = new RectSpec(1, 0, 1, 0f);
        rect.cornerRadius = Integer.MIN_VALUE;
        rect.strokeWidth = Float.NaN;
        return rect;
    }
    private static ShadowSpec invalidShadowSpec() {
        ShadowSpec shadow = new ShadowSpec(0f, 0f, 0f, 1);
        shadow.dx = Float.NaN;
        shadow.dy = Float.NaN;
        shadow.radius = Float.NaN;
        return shadow;
    }

    @RequiresApi(14) public static final Property<View, Integer> DECOR_RECT_FILL_COLOR = intProp(0, "itemRectFillColor"); // TODO maybe support DECOR_RECT_FILL_SHADER
    @RequiresApi(14) public static final Property<View, Integer> DECOR_RECT_CORNER_RADIUS = intProp(1, "itemRectCornerRadius");
    @RequiresApi(14) public static final Property<View, Integer> DECOR_RECT_STROKE_COLOR = intProp(2, "itemRectStrokeColor");
    @RequiresApi(14) public static final Property<View, Float> DECOR_RECT_STROKE_WIDTH = floatProp(3, "itemRectStrokeWidth");
    @RequiresApi(14) public static final Property<View, Float> DECOR_SHADOW_DX = floatProp(4, "itemShadowDx");
    @RequiresApi(14) public static final Property<View, Float> DECOR_SHADOW_DY = floatProp(5, "itemShadowDy");
    @RequiresApi(14) public static final Property<View, Float> DECOR_SHADOW_RADIUS = floatProp(6, "itemShadowRadius");
    @RequiresApi(14) public static final Property<View, Integer> DECOR_SHADOW_COLOR = intProp(7, "itemShadowColor");

    private static Property<View, Integer> intProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new IntProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new IntProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class IntProp14 extends Property<View, Integer> {
        private final int index;
        public IntProp14(int index, String name) { super(Integer.class, name); this.index = index; }
        @Override public Integer get(View object) { return get_(object, index); }
        @Override public void set(View object, Integer value) { set_(object, index, value); }
    }
    @RequiresApi(24) private static final class IntProp24 extends IntProperty<View> {
        private final int index;
        public IntProp24(int index, String name) { super(name); this.index = index; }
        @Override public Integer get(View object) { return get_(object, index); }
        @Override public void setValue(View object, int value) { set_(object, index, value); }
    }

    private static Property<View, Float> floatProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new FloatProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new FloatProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class FloatProp14 extends Property<View, Float> {
        private final int index;
        public FloatProp14(int index, String name) { super(Float.class, name); this.index = index; }
        @Override public Float get(View object) { return intBitsToFloat(get_(object, index)); }
        @Override public void set(View object, Float value) { set_(object, index, floatToRawIntBits(value)); }
    }
    @RequiresApi(24) private static final class FloatProp24 extends FloatProperty<View> {
        private final int index;
        public FloatProp24(int index, String name) { super(name); this.index = index; }
        @Override public Float get(View object) { return intBitsToFloat(get_(object, index)); }
        @Override public void setValue(View object, float value) { set_(object, index, floatToRawIntBits(value)); }
    }

}
