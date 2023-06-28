package net.aquadc.fiftyshades;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Property;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;

import static java.lang.Math.min;
import static net.aquadc.fiftyshades.Numbers.requireNonNegative;


public abstract class Shadow extends Drawable {

    ShadowState state;
    final Paint paint;
    Shadow(@NonNull ShadowState state, int paintFlags) {
        this.state = state;
        paint = new Paint(paintFlags);
    }
    Shadow(@Px int cornerRadius, @NonNull ShadowSpec shadow, boolean inner, int paintFlags) {
        this.state = new ShadowState(cornerRadius, shadow, inner);
        paint = new Paint(paintFlags);
    }

    // int get-set

    public final int cornerRadius() { return state.cornerRadius; }
    @RequiresApi(14) public static final Property<Shadow, Integer> CORNER_RADIUS = intProp(0, "cornerRadius");

    @ColorInt public final int shadowColor() { return state.shadow.color; }
    @RequiresApi(14) public static final Property<Shadow, Integer> SHADOW_COLOR = intProp(1, "shadowColor");

    final int i(int index) {
        switch (index) {
            case 0: return state.cornerRadius;
            case 1: return state.shadow.color;
            default: throw new AssertionError();
        }
    }
    final Shadow i(int index, int value) {
        if (i(index) != value) {
            switch (index) {
                case 0:
                    state.cornerRadius = requireNonNegative(value, "cornerRadius");
                    radiusInvalidated();
                    break;
                case 1:
                    state.shadow.color = value;
                    shadowColorInvalidated();
                    break;
                default:
                    throw new AssertionError();
            }
            invalidateSelf();
        }
        return this;
    }

    // float get-set

    @Px public final float shadowDx() { return state.shadow.dx; }
    @RequiresApi(14) public static final Property<Shadow, Float> SHADOW_DX = floatProp(1, "shadowDx");

    @Px public final float shadowDy() { return state.shadow.dy; }
    @RequiresApi(14) public static final Property<Shadow, Float> SHADOW_DY = floatProp(2, "shadowDy");

    @Px public final float shadowRadius() { return state.shadow.radius; }
    @RequiresApi(14) public static final Property<Shadow, Float> SHADOW_RADIUS = floatProp(3, "shadowRadius");

    final float f(int index) {
        ShadowSpec shadow = state.shadow;
        switch (index) {
            // 0 is reserved for possible cornerRadius
            case 1: return shadow.dx;
            case 2: return shadow.dy;
            case 3: return shadow.radius;
            default: throw new AssertionError();
        }
    }
    final Shadow f(int index, float value) {
        ShadowSpec shadow = state.shadow;
        if (f(index) != value) {
            switch (index) {
                case 1:
                    shadow.dx(value);
                    shadowOffsetInvalidated();
                    break;
                case 2:
                    shadow.dy(value);
                    shadowOffsetInvalidated();
                    break;
                case 3:
                    shadow.radius(value);
                    shadowRadiusInvalidated();
                    break;
                default: throw new AssertionError();
            }
            invalidateSelf();
        }
        return this;
    }

    public Shadow cornerRadius(int cornerRadius) { return i(0, cornerRadius); }
    public Shadow shadowColor(@ColorInt int color) { return i(1, color); }
    public Shadow shadowDx(@Px float dx) { return f(1, dx); }
    public Shadow shadowDy(@Px float dy) { return f(2, dy); }
    public Shadow shadowRadius(@Px float radius) { return f(3, radius); }
    public Shadow shadow(@NonNull ShadowSpec shadow) {
        int changes = state.shadow.setFrom(shadow);
        if (changes == 0) return this;
        if ((changes & 3) != 0) shadowOffsetInvalidated();
        if ((changes & 4) != 0) shadowRadiusInvalidated();
        if ((changes & 8) != 0) shadowColorInvalidated();
        invalidateSelf();
        return this;
    }

    // invalidation

    abstract void radiusInvalidated();
    abstract void shadowOffsetInvalidated();
    abstract void shadowRadiusInvalidated();
    abstract void shadowColorInvalidated();

    // drawing

    @Override public final int getAlpha() { return paint.getAlpha(); }
    @Override public final void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Nullable @Override public final ColorFilter getColorFilter() { return paint.getColorFilter(); }
    @Override public final void setColorFilter(@Nullable ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

    @Override public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    final int boundedCornerRadius() {
        // limit corners to size we can afford:
        Rect bounds = getBounds();
        return min(min(bounds.width(), bounds.height()) / 2, state.cornerRadius);
    }

    // state

    @Override public final ConstantState getConstantState() {
        return state;
    }
    @NonNull @Override public final Drawable mutate() {
        ShadowSpec shadow = state.shadow;
        state = new ShadowState(state.cornerRadius, new ShadowSpec(shadow), state.inner);
        return this;
    }

    static final class ShadowState extends ConstantState {
        int cornerRadius; // should this be fractional? TODO decide
        final ShadowSpec shadow;
        final boolean inner;
        // do we need CornerSet here? TODO decide
        ShadowState(int cornerRadius, ShadowSpec shadow, boolean inner) {
            this.cornerRadius = cornerRadius;
            this.shadow = shadow;
            this.inner = inner;
        }
        @NonNull @Override public Drawable newDrawable() {
            return inner ? new RectInnerShadow(this) : new RectShadow(this);
        }
        @Override public int getChangingConfigurations() {
            return 0;
        }
    }

    // property utils

    private static Property<Shadow, Integer> intProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new IntProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new IntProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class IntProp14 extends Property<Shadow, Integer> {
        private final int index;
        public IntProp14(int index, String name) { super(Integer.class, name); this.index = index; }
        @Override public Integer get(Shadow object) { return object.i(index); }
        @Override public void set(Shadow object, Integer value) { object.i(index, value); }
    }
    @RequiresApi(24) private static final class IntProp24 extends IntProperty<Shadow> {
        private final int index;
        public IntProp24(int index, String name) { super(name); this.index = index; }
        @Override public Integer get(Shadow object) { return object.i(index); }
        @Override public void setValue(Shadow object, int value) { object.i(index, value); }
    }

    private static Property<Shadow, Float> floatProp(int index, String name) {
        if (Build.VERSION.SDK_INT >= 24) return new FloatProp24(index, name);
        else if (Build.VERSION.SDK_INT >= 14) return new FloatProp14(index, name);
        else return null;
    }
    @RequiresApi(14) private static final class FloatProp14 extends Property<Shadow, Float> {
        private final int index;
        public FloatProp14(int index, String name) { super(Float.class, name); this.index = index; }
        @Override public Float get(Shadow object) { return object.f(index); }
        @Override public void set(Shadow object, Float value) { object.f(index, value); }
    }
    @RequiresApi(24) private static final class FloatProp24 extends FloatProperty<Shadow> {
        private final int index;
        public FloatProp24(int index, String name) { super(name); this.index = index; }
        @Override public Float get(Shadow object) { return object.f(index); }
        @Override public void setValue(Shadow object, float value) { object.f(index, value); }
    }

}
