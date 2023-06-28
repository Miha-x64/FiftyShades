package net.aquadc.fiftyshades;

import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import static net.aquadc.fiftyshades.Numbers.appendColor;
import static net.aquadc.fiftyshades.Numbers.ceil;
import static net.aquadc.fiftyshades.Numbers.requireFinite;
import static net.aquadc.fiftyshades.Numbers.requireNonNegative;


/**
 * Shadow properties.
 */
public final class ShadowSpec {
    @Px float dx;
    @Px float dy;
    @Px float radius;
    @ColorInt int color;

    /**
     * Constructs new shadow spec.
     * @param dx     horizontal shadow offset
     * @param dy     vertical shadow offset
     * @param radius radius of shadow blur (non-negative)
     * @param color  colour of shadow
     * @throws IllegalArgumentException if any float is infinite or NaN, or radius is negative
     */
    public ShadowSpec(@Px float dx, @Px float dy, @Px float radius, @ColorInt int color) {
        dx(dx);
        dy(dy);
        radius(radius);
        this.color = color;
    }

    @Px public float dx() { return dx; }
    @Px public float dy() { return dy; }
    @Px public float radius() { return radius; }
    @ColorInt public int color() { return color; }

    boolean isVisible() { return (color >>> 24) != 0; }
    void dx(float dx) { this.dx = requireFinite(dx, "dx"); }
    void dy(float dy) { this.dy = requireFinite(dy, "dy"); }
    void radius(float radius) { this.radius = requireNonNegative(radius, "radius"); }
    int setFrom(@NonNull ShadowSpec original) {
        int changes = 0;
        if (this.dx != original.dx) {
            this.dx = original.dx;
            changes |= 1;
        }
        if (this.dy != original.dy) {
            this.dy = original.dy;
            changes |= 2;
        }
        if (this.radius != original.radius) {
            this.radius = original.radius;
            changes |= 4;
        }
        if (this.color != original.color) {
            this.color = original.color;
            changes |= 8;
        }
        return changes;
    }

    @NonNull Rect inferPaddings() {
        int l, t, r, b;
        l = t = r = b = ceil(radius);
        int d;
        if ((d = ceil(dx)) > 0) r += d; else l -= d;
        if ((d = ceil(dy)) > 0) b += d; else t -= d;
        return new Rect(l, t, r, b);
    }

    @Override public boolean equals(Object o) {
        ShadowSpec that;
        return this == o || (o instanceof ShadowSpec &&
            color == (that = (ShadowSpec) o).color &&
            Float.compare(that.dx, dx) == 0 &&
            Float.compare(that.dy, dy) == 0 &&
            Float.compare(that.radius, radius) == 0
        );
    }
    @Override public int hashCode() {
        return 31 * (31 * (31 *
            (dx != +0.0f ? Float.floatToIntBits(dx) : 0) +
            (dy != +0.0f ? Float.floatToIntBits(dy) : 0)) +
            (radius != +0.0f ? Float.floatToIntBits(radius) : 0)) +
            color;
    }
    @Override public String toString() {
        return appendColor(new StringBuilder("ShadowSpec")
            .append("(dx=").append(dx)
            .append(", dy=").append(dy)
            .append(", radius=").append(radius)
            .append(", color="), color)
            .append(')').toString();
    }
}
