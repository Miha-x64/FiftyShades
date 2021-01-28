package net.aquadc.fiftyshades;

import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import static net.aquadc.fiftyshades.Numbers.*;


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
    public ShadowSpec(float dx, float dy, float radius, int color) {
        requireFinite(dx, "dx");
        requireFinite(dy, "dy");
        requireNonNegative(radius, "radius");
        this.dx = dx;
        this.dy = dy;
        this.radius = radius;
        this.color = color;
    }

    @Px public float dx() { return dx; }
    @Px public float dy() { return dy; }
    @Px public float radius() { return radius; }
    @ColorInt public int color() { return color; }

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
    @Override
    public String toString() {
        return appendColor(new StringBuilder("ShadowSpec(")
            .append("dx=").append(dx)
            .append(", dy=").append(dy)
            .append(", radius=").append(radius)
            .append(", color="), color)
            .append(')').toString();
    }
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static StringBuilder appendColor(StringBuilder to, int color) {
        to.append('#');
        if ((((long) color) & 0xFF000000L) != 0xFF000000L) {
            to.append(HEX[color >>> 28 & 15]).append(HEX[color >>> 24 & 15]);
        }
        return to.append(HEX[(color >>> 20) & 15]).append(HEX[(color >>> 16) & 15])
            .append(HEX[(color >>> 12) & 15]).append(HEX[(color >>> 8) & 15])
            .append(HEX[(color >>> 4) & 15]).append(HEX[color & 15]);
    }
}