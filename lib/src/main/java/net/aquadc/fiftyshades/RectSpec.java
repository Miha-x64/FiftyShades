package net.aquadc.fiftyshades;


import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.Px;

import static net.aquadc.fiftyshades.Numbers.appendColor;
import static net.aquadc.fiftyshades.Numbers.requireNonNegative;


/**
 * Rounded rectangle properties.
 */
public final class RectSpec {
    @ColorInt int fillColor;
    @Px int cornerRadius;
    @ColorInt int strokeColor;
    @Px float strokeWidth;

    /**
     * Constructs new RectSpec.
     * @param fillColor     round rect fill colour
     * @param cornerRadius  round rect corner radius
     * @param strokeColor   round rect stroke colour
     * @param strokeWidth   round rect stroke width
     */
    public RectSpec(@ColorInt int fillColor, @Px int cornerRadius, @ColorInt int strokeColor, @Px float strokeWidth) {
        this.fillColor = fillColor;
        this.cornerRadius = requireNonNegative(cornerRadius, "cornerRadius");
        this.strokeColor = strokeColor;
        this.strokeWidth = requireNonNegative(strokeWidth, "strokeWidth");
    }

    /**
     * Constructs new RectSpec with no stroke.
     * @param fillColor     round rect fill colour
     * @param cornerRadius  round rect corner radius
     */
    public RectSpec(@ColorInt int fillColor, @Px int cornerRadius) {
        this(fillColor, cornerRadius, Color.TRANSPARENT, 0f);
    }

    @ColorInt public int fillColor() { return fillColor; }
    @Px public int cornerRadius() { return cornerRadius; }
    @ColorInt public int strokeColor() { return strokeColor; }
    @Px public float strokeWidth() { return strokeWidth; }

    boolean hasFill() { return (fillColor >>> 24) != 0; }
    boolean hasStroke() { return (strokeColor >>> 24) != 0 && strokeWidth > 0f; }

    @Override public boolean equals(Object o) {
        RectSpec that;
        return this == o || (o instanceof RectSpec &&
            fillColor == (that = (RectSpec) o).fillColor &&
            cornerRadius == that.cornerRadius &&
            strokeColor == that.strokeColor &&
            Float.compare(strokeWidth, that.strokeWidth) == 0
        );
    }
    @Override public int hashCode() {
        return 31 * (31 * (31 * fillColor +
            cornerRadius) +
            strokeColor) +
            (strokeWidth != +0.0f ? Float.floatToIntBits(strokeWidth) : 0);
    }
    @Override public String toString() {
        StringBuilder sb = appendColor(new StringBuilder("RectSpec")
            .append("(fillColor="), fillColor)
            .append(", cornerRadius=").append(cornerRadius);
        if (strokeColor != Color.TRANSPARENT) appendColor(sb.append(", strokeColor="), strokeColor);
        if (strokeWidth != 0f) sb.append(", strokeWidth=").append(strokeWidth);
        return sb.append(')').toString();
    }
}
