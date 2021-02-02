package net.aquadc.fiftyshades;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Property;
import androidx.annotation.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.aquadc.fiftyshades.Numbers.ceil;
import static net.aquadc.fiftyshades.Numbers.requireNonNegative;

/**
 * A shadow dropped by a rectangle with rounded corners.
 */
public final class RectShadow extends Drawable {

    private ShadowState state;
    RectShadow(ShadowState state) {
        this.state = state;
    }
    public RectShadow() {
        this(0, 0f, 0f, 0f, Color.TRANSPARENT);
    }
    public RectShadow(@Px int cornerRadius, @NonNull ShadowSpec shadow) {
        this(cornerRadius, shadow.dx, shadow.dy, shadow.radius, shadow.color);
    }
    public RectShadow(@Px int cornerRadius, @Px float dx, @Px float dy, @Px float radius, @ColorInt int color) {
        this.state = new ShadowState(cornerRadius, dx, dy, radius, color);
    }

    // int get-set

    public int cornerRadius() { return state.cornerRadius; }
    public RectShadow cornerRadius(int cornerRadius) { return i(0, cornerRadius); }
    @RequiresApi(14) public static final Property<RectShadow, Integer> CORNER_RADIUS = intProp(0, "cornerRadius");

    @ColorInt public int shadowColor() { return state.shadow.color; }
    public RectShadow shadowColor(@ColorInt int color) { return i(1, color); }
    @RequiresApi(14) public static final Property<RectShadow, Integer> SHADOW_COLOR = intProp(1, "shadowColor");

    int i(int index) {
        switch (index) {
            case 0: return state.cornerRadius;
            case 1: return state.shadow.color;
            default: throw new AssertionError();
        }
    }
    RectShadow i(int index, int value) {
        if (i(index) != value) {
            switch (index) {
                case 0:
                    state.cornerRadius = requireNonNegative(value, "cornerRadius");
                    break;
                case 1:
                    state.shadow.color = value;
                    edgeShader = null;
                    break;
                default:
                    throw new AssertionError();
            }
            // both cornerRadius and shadowColor invalidate cornerShader (but edgeShader don't mind cornerRadius)
            cornerShader = null;
            invalidateSelf();
        }
        return this;
    }

    public RectShadow shadow(@NonNull ShadowSpec shadow) {
        if (state.shadow.setFrom(shadow)) {
            cornerShader = null;
            edgeShader = null;
            invalidateSelf();
        }
        return this;
    }

    // float get-set

    @Px public float shadowDx() { return state.shadow.dx; }
    public RectShadow shadowDx(@Px float dx) { return f(1, dx); }
    @RequiresApi(14) public static final Property<RectShadow, Float> SHADOW_DX = floatProp(1, "shadowDx");

    @Px public float shadowDy() { return state.shadow.dy; }
    public RectShadow shadowDy(@Px float dy) { return f(2, dy); }
    @RequiresApi(14) public static final Property<RectShadow, Float> SHADOW_DY = floatProp(2, "shadowDy");

    @Px public float shadowRadius() { return state.shadow.radius; }
    public RectShadow shadowRadius(@Px float radius) { return f(3, radius); }
    @RequiresApi(14) public static final Property<RectShadow, Float> SHADOW_RADIUS = floatProp(3, "shadowRadius");

    float f(int index) {
        ShadowSpec shadow = state.shadow;
        switch (index) {
            // 0 is reserved for possible cornerRadius
            case 1: return shadow.dx;
            case 2: return shadow.dy;
            case 3: return shadow.radius;
            default: throw new AssertionError();
        }
    }
    RectShadow f(int index, float value) {
        ShadowSpec shadow = state.shadow;
        if (f(index) != value) {
            switch (index) {
                case 1: shadow.dx(value); break;
                case 2: shadow.dy(value); break;
                case 3: shadow.radius(value); break;
                default: throw new AssertionError();
            }
            edgeShader = null;
            cornerShader = null;
            invalidateSelf();
        }
        return this;
    }

    // invalidation

    @Override public void setBounds(@NonNull Rect bounds) {
        int corners = boundedCornerRadius();
        float sd = shadowDistance();
        super.setBounds(bounds);

        // quite rare cases when we're extremely small
        boolean sdInvalid = sd != shadowDistance();
        if (sdInvalid) edgeShader = null;
        if (sdInvalid || corners != boundedCornerRadius()) cornerShader = null;
    }

    // drawing

    private final Paint paint = new Paint(Paint.DITHER_FLAG);

    @Override public int getAlpha() { return paint.getAlpha(); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Nullable @Override public ColorFilter getColorFilter() { return paint.getColorFilter(); }
    @Override public void setColorFilter(@Nullable ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

    private final int[] radialColors = new int[5];
    private final float[] radialPositions = { 0f, Float.NaN, Float.NaN, Float.NaN, 1f };
    private RadialGradient cornerShader;
    private final int[] linearColors = new int[3];
    private final float[] linearPositions = { 0f, Float.NaN, 1f };
    private LinearGradient edgeShader;
    @Override public void draw(@NonNull Canvas canvas) {
        if (Color.alpha(state.shadow.color) == 0) return;

        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();

        canvas.save();
        canvas.translate(
            (width < 0 ? bounds.centerX() : bounds.left) + state.shadow.dx,
            (height < 0 ? bounds.centerY() : bounds.top) + state.shadow.dy
        ); //       ^^^ guard against drawing ugly shadow when we're squeezed. This looks quite possible in InsetDrawable
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        float shadowDistance = shadowDistance();
        int cornerRadius = boundedCornerRadius();
        drawCorners(canvas, cornerRadius, width, height, shadowDistance);
        drawEdges(canvas, cornerRadius, width, height, shadowDistance);
        canvas.restore();
    }
    private int boundedCornerRadius() {
        // limit corners to size we can afford:
        Rect bounds = getBounds();
        return min(min(bounds.width(), bounds.height()) / 2, state.cornerRadius);
    }
    private float shadowDistance() {
        ShadowSpec shadow = state.shadow;
        Rect bounds = getBounds();
        return min(
            (float) Math.sqrt(shadow.dx * shadow.dx + shadow.dy * shadow.dy) + 1, // <-- extra pixel for nicer look
            //      ^^^^^^^^^ Math.hypot is way slower but we don't need extreme accuracy here
            min(bounds.width(), bounds.height()) / 2f // don't overlap self
        );
    }

    private void drawCorners(Canvas canvas, int cornerRadius, int width, int height, float shadowDistance) {
        float shRad = state.shadow.radius;
        float gRad = cornerRadius + shRad;
        if (cornerShader == null) buildCornerShader(cornerRadius, shadowDistance, gRad);
        paint.setShader(cornerShader);
        int shadowRadInt = ceil(shRad);
        drawOuterCorner(canvas, cornerRadius, gRad, -shadowRadInt, -shadowRadInt, cornerRadius, cornerRadius);
        int cornerDiameter = cornerRadius + cornerRadius;
        canvas.translate(width - cornerDiameter, 0f);
        drawOuterCorner(canvas, cornerRadius, gRad, cornerRadius, -shadowRadInt, cornerDiameter + shadowRadInt, cornerRadius);
        canvas.translate(0f, height - cornerDiameter);
        drawOuterCorner(canvas, cornerRadius, gRad, cornerRadius, cornerRadius, cornerDiameter + shadowRadInt, cornerDiameter + shadowRadInt);
        canvas.translate(-width + cornerDiameter, 0f);
        drawOuterCorner(canvas, cornerRadius, gRad, -shadowRadInt, cornerRadius, cornerRadius, cornerDiameter + shadowRadInt);
        canvas.translate(0f, -height + cornerDiameter);
    }
    private void buildCornerShader(int cornerRadius, float shadowDistance, float gRad) {
        int shCol = state.shadow.color;
        radialColors[0] = radialColors[1] = radialColors[4] = 0xFFFFFF & shCol;
        radialColors[2] = radialColors[3] = shCol;
        radialPositions[1] = (cornerRadius - shadowDistance - .66f) / gRad;
        radialPositions[2] = (cornerRadius - shadowDistance + .34f) / gRad; // this gives us nice inner edge even without anti-alias
        radialPositions[3] = cornerRadius / gRad;
        cornerShader = new RadialGradient(cornerRadius, cornerRadius, gRad, radialColors, radialPositions, Shader.TileMode.CLAMP);
    }
    private void drawOuterCorner(
        Canvas canvas, int cornerRadius, float gradientRadius,
        int clipL, int clipT, int clipR, int clipB
    ) {
        canvas.save();
        canvas.clipRect(clipL, clipT, clipR, clipB);
        canvas.drawCircle(cornerRadius, cornerRadius, gradientRadius, paint);
        canvas.restore();
    }

    private void drawEdges(Canvas canvas, int cornerRadius, int width, int height, float shadowDistance) {
        if (edgeShader == null) buildEdgeShader(shadowDistance);
        paint.setShader(edgeShader);

        float shRad = state.shadow.radius;
        canvas.drawRect(cornerRadius, -shRad, width - cornerRadius, shadowDistance, paint);
        float angle = width > height ? -90f : 90f;
        float halfMinSize = min(width, height) / 2f;
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawRect(cornerRadius, -shRad, height - cornerRadius, shadowDistance, paint);
        float halfMaxSize = max(width, height) / 2f;
        canvas.rotate(angle, halfMaxSize, halfMaxSize);
        canvas.drawRect(cornerRadius, -shRad, width - cornerRadius, shadowDistance, paint);
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawRect(cornerRadius, -shRad, height - cornerRadius, shadowDistance, paint);
    }
    private void buildEdgeShader(float shadowDistance) {
        int shCol = state.shadow.color;
        linearColors[0] = 0xFFFFFF & shCol;
        linearColors[1] = linearColors[2] = shCol;
        float shRad = state.shadow.radius;
        float gSize = shRad + shadowDistance;
        linearPositions[1] = shRad / gSize;
        edgeShader = new LinearGradient(0f, -shRad, 0f, shadowDistance, linearColors, linearPositions, Shader.TileMode.CLAMP);
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    // state

    @Override public ConstantState getConstantState() {
        return state;
    }
    @NonNull @Override public Drawable mutate() {
        ShadowSpec shadow = state.shadow;
        state = new ShadowState(state.cornerRadius, shadow.dx, shadow.dy, shadow.radius, shadow.color);
        return this;
    }

    private static final class ShadowState extends ConstantState {
        int cornerRadius; // should this be fractional? TODO decide
        final ShadowSpec shadow;
        // do we need CornerSet here? TODO decide
        ShadowState(int cornerRadius, float dx, float dy, float radius, int color) {
            this.cornerRadius = cornerRadius;
            this.shadow = new ShadowSpec(dx, dy, radius, color);
        }
        @NonNull @Override public Drawable newDrawable() {
            return new RectShadow(this);
        }
        @Override public int getChangingConfigurations() {
            return 0;
        }
    }

    // property utils

    private static Property<RectShadow, Integer> intProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new IntProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new IntProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class IntProp14 extends Property<RectShadow, Integer> {
        private final int index;
        public IntProp14(int index, String name) { super(Integer.class, name); this.index = index; }
        @Override public Integer get(RectShadow object) { return object.i(index); }
        @Override public void set(RectShadow object, Integer value) { object.i(index, value); }
    }
    @RequiresApi(24) private static final class IntProp24 extends IntProperty<RectShadow> {
        private final int index;
        public IntProp24(int index, String name) { super(name); this.index = index; }
        @Override public Integer get(RectShadow object) { return object.i(index); }
        @Override public void setValue(RectShadow object, int value) { object.i(index, value); }
    }

    private static Property<RectShadow, Float> floatProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new FloatProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new FloatProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class FloatProp14 extends Property<RectShadow, Float> {
        private final int index;
        public FloatProp14(int index, String name) { super(Float.class, name); this.index = index; }
        @Override public Float get(RectShadow object) { return object.f(index); }
        @Override public void set(RectShadow object, Float value) { object.f(index, value); }
    }
    @RequiresApi(24) private static final class FloatProp24 extends FloatProperty<RectShadow> {
        private final int index;
        public FloatProp24(int index, String name) { super(name); this.index = index; }
        @Override public Float get(RectShadow object) { return object.f(index); }
        @Override public void setValue(RectShadow object, float value) { object.f(index, value); }
    }
}
