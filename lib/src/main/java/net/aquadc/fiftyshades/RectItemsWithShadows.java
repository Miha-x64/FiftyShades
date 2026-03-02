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
import static net.aquadc.fiftyshades.Numbers.multiplyAlpha;
import static net.aquadc.fiftyshades.ViewDrawablePool.scrapUnused;
import static net.aquadc.fiftyshades.ViewDrawablePool.unsafeDrawableFor;
import static net.aquadc.fiftyshades.ViewDrawablePool.usedMarkFor;

/**
 * ItemDecoration which draws a round rect with a shadow for each item.
 */
@RequiresApi(11) public final class RectItemsWithShadows extends RecyclerView.ItemDecoration {

    private final Shadow.ShadowState factory;
    private final RectSpec rect;
    private final ShadowSpec shadow;
    private final Paint paint = new Paint();
    private final SparseArray<Shadow> drawables = new SparseArray<>();
    private final ArrayList<Shadow> scrap = new ArrayList<>(0);

    public RectItemsWithShadows(@NonNull RectSpec rect, @NonNull ShadowSpec shadow) {
        this(rect, shadow, false);
    }
    public RectItemsWithShadows(@NonNull RectSpec rect, @NonNull ShadowSpec shadow, boolean inner) {
        this.factory = new Shadow.ShadowState(0, new ShadowSpec(), inner);
        this.rect = rect;
        this.shadow = shadow;
    }

    // DRAWING

    private final RectF bounds = new RectF(); // drawRoundRect(l, t, r, b, …) is 21+, we use drawRoundRect(bounds, …)
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

            usedDrawables |= usedMarkFor(drawables, v);
        }
        scrapUnused(drawables, scrap, usedDrawables);
    }
    private void drawOuter(Canvas c, RecyclerView parent) {
        for (int i = 0, children = parent.getChildCount(); i < children; i++) {
            View v = parent.getChildAt(i);
            ShadowSpec viewShadow = shadowSpecOf(v);
            RectSpec viewRect;
            // draw shadow below, if outer
            if (viewShadow.isVisible() && // outer 0-shadow is visible only below transparent shape:
                    !((viewRect = rectSpecOf(v)).isOpaque() && viewShadow.isZero())) {
                bounds.set(0, 0, v.getWidth(), v.getHeight());
                Shadow drawable = unsafeDrawableFor(drawables, scrap, factory, v);
                c.save();
                c.translate(v.getLeft(), v.getTop());
                c.concat(v.getMatrix());
                drawShadow(c, drawable, viewRect.cornerRadius, viewShadow, (int) (v.getAlpha() * 255));
                c.restore();
            }
        }
    }
    private void drawRemaining(Canvas c, RecyclerView parent) {
        for (int i = 0, children = parent.getChildCount(); i < children; i++) {
            View v = parent.getChildAt(i);
            RectSpec viewRect = rectSpecOf(v);
            ShadowSpec inShadow = null;
            if (factory.inner) {
                inShadow = shadowSpecOf(v);
                if (!inShadow.isVisible() || inShadow.isZero()) inShadow = null;
            }
            if (viewRect.hasFill() || inShadow != null || viewRect.hasStroke()) {
                drawRemainingForView(c, v, viewRect, inShadow);
            }
        }
    }
    private void drawRemainingForView(Canvas c, View v, RectSpec viewRect, ShadowSpec inShadow) {
        bounds.set(0, 0, v.getWidth(), v.getHeight());
        float alpha = v.getAlpha();

        c.save();
        c.translate(v.getLeft(), v.getTop());
        c.concat(v.getMatrix());

        paint.setAntiAlias(viewRect.cornerRadius > 0);
        if (viewRect.hasFill())
            fill(c, alpha, viewRect.fillColor, viewRect.cornerRadius);

        // draw shadow above, if inner
        if (inShadow != null) {
            Shadow drawable = unsafeDrawableFor(drawables, scrap, factory, v);
            drawShadow(c, drawable, viewRect.cornerRadius, inShadow, (int) (alpha * 255));
        }

        // draw stroke above inner shadow
        if (viewRect.hasStroke())
            stroke(c, alpha, viewRect.strokeColor, viewRect.strokeWidth, viewRect.cornerRadius);

        c.restore();
    }

    private void fill(Canvas c, float alpha, int color, int cornerRadius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(multiplyAlpha(color, alpha));
        c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
    private void stroke(Canvas c, float alpha, int strokeColor, float strokeWidth, int cornerRadius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(multiplyAlpha(strokeColor, alpha));
        paint.setStrokeWidth(strokeWidth);
        c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
    private void drawShadow(Canvas c, Shadow drawable, int cornerRadius, ShadowSpec viewShadow, int alpha) {
        drawable.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
        drawable.setAlpha(alpha);
        drawable.cornerRadius(cornerRadius)
            .shadow(viewShadow)
            .draw(c);
    }

    private RectSpec rectSpecOf(View v) {
        RectSpec viewRect = (RectSpec) v.getTag(R.id.fiftyShades_decorRectSpec);
        return viewRect == null ? rect : viewRect;
    }
    private ShadowSpec shadowSpecOf(View v) {
        ShadowSpec viewShadow = (ShadowSpec) v.getTag(R.id.fiftyShades_decorShadowSpec);
        return viewShadow == null ? shadow : viewShadow;
    }

    // ANIMATION

    static int get_(View view, int at) {
        if (at < 4) {
            RectSpec rect = (RectSpec) view.getTag(R.id.fiftyShades_decorRectSpec);
            if (rect == null) return 0; // even floatToRawIntBits(0f) == 0
            switch (at) {
                case 0: return rect.fillColor;
                case 1: return rect.cornerRadius;
                case 2: return rect.strokeColor;
                case 3: return floatToRawIntBits(rect.strokeWidth);
                default: throw new AssertionError();
            }
        } else {
            ShadowSpec shadow = (ShadowSpec) view.getTag(R.id.fiftyShades_decorShadowSpec);
            if (shadow == null) return 0;
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
                case 1: rect.cornerRadius(value); break;
                case 2: rect.strokeColor = value; break;
                case 3: rect.strokeWidth(intBitsToFloat(value)); break;
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
        RectSpec rect = new RectSpec(1, 0, 1, 0f);
        rect.cornerRadius = Integer.MIN_VALUE;
        rect.strokeWidth = Float.NaN;
        return rect; // Since animator properties don't know about decor,
    } // invalid values indicate that decor should default to its own common values later.
    private static ShadowSpec invalidShadowSpec() {
        ShadowSpec shadow = new ShadowSpec(0f, 0f, 0f, 1);
        shadow.dx = Float.NaN;
        shadow.dy = Float.NaN;
        shadow.radius = Float.NaN;
        return shadow;
    } // And here comes that 'later' when we default to common values.
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
