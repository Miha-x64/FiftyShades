package net.aquadc.fiftyshades;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import androidx.annotation.NonNull;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static net.aquadc.fiftyshades.Numbers.ceil;
import static net.aquadc.fiftyshades.Numbers.putLe;

/**
 * Enumerates all possible corner and edge combinations.
 */
public enum CornerSet {
    /** ⌜ */
    TOP_LEFT(1, 1, 0, 0, 0, 0, 0, 1),
    /** ⌝ */
    TOP_RIGHT(0, 1, 1, 1, 0, 0, 0, 0),
    /** ⌞ */
    BOTTOM_LEFT(0, 0, 0, 0, 0, 1, 1, 1),
    /** ⌟ */
    BOTTOM_RIGHT(0, 0, 0, 1, 1, 1, 0, 0),

    /** ┌─
     *  │
     *  └─ */
    BOTH_LEFT(1, 1, 0, 0, 0, 1, 1, 1),
    /** ┌─┐ */
    BOTH_TOP(1, 1, 1, 1, 0, 0, 0, 1),
    /** ─┐
     *   │
     *  ─┘ */
    BOTH_RIGHT(0, 1, 1, 1, 1, 1, 0, 0),
    /** └─┘ */
    BOTH_BOTTOM(0, 0, 0, 1, 1, 1, 1, 1),

    /** = */
    HORIZONTAL(0, 1, 0, 0, 0, 1, 0, 0),
    /** || */
    VERTICAL(0, 0, 0, 1, 0, 0, 0, 1),

    /** ┌─┐
     *  │ │
     *  └─┘ */
    ALL(1, 1, 1, 1, 1, 1, 1, 1),

    /** └─┘
     *      < stretchable space
     *  ┌─┐ */
    BETWEEN_BOTTOM_AND_TOP(1, 1, 1, 0, 1, 1, 1, 0),
    /** ┐ ┌
     *  │ │
     *  ┘ └
     *   ^ stretchable space
     */
    BETWEEN_RIGHT_AND_LEFT(1, 0, 1, 1, 1, 0, 1, 1),
    ;

    // enumerates all present corners and edges
    final int cornersAndEdges;
    CornerSet(int tl, int t, int tr, int r, int br, int b, int bl, int l) {
        cornersAndEdges = tl | (t << 1) | (tr << 2) | (r << 3) | (br << 4) | (b << 5) | (bl << 6) | (l << 7);
    }

    private static final int ANY_LEFT = (1 | (1 << 6) | (1 << 7)); // tl || bl || l
    private static final int ANY_RIGHT = ((1 << 2) | (1 << 3) | (1 << 4)); // tr || r || br
    private static final int ANY_TOP = (1 | (1 << 1) | (1 << 2)); // tl || t || tr
    private static final int ANY_BOTTOM = ((1 << 4) | (1 << 5) | (1 << 6)); // br || b || bl
    int measureWidth(@NonNull Rect paddings, int corner, ShadowSpec shadow) {
        boolean anyLeft = (cornersAndEdges & ANY_LEFT) != 0;
        boolean anyRight = (cornersAndEdges & ANY_RIGHT) != 0;
        return measure(corner, shadow.dx, shadow.radius, anyLeft, anyRight, paddings.left, paddings.right);
    }
    int measureHeight(@NonNull Rect paddings, int corner, ShadowSpec shadow) {
        boolean anyTop = (cornersAndEdges & ANY_TOP) != 0;
        boolean anyBottom = (cornersAndEdges & ANY_BOTTOM) != 0;
        return measure(corner, shadow.dy, shadow.radius, anyTop, anyBottom, paddings.top, paddings.bottom);
    }
    private static int measure(int cornerRadius, float d, float r, boolean hasStart, boolean hasEnd, int start, int end) {
        int dPos = max(0, ceil(d + r)), dNeg = min(0, ceil(d - r));
        return (hasStart ? start + cornerRadius + dPos : 0) + 1 + (hasEnd ? -dNeg + cornerRadius + end : 0);
    }

    @NonNull RectF layout(@NonNull Rect paddings, int cornerX, int cornerY, ShadowSpec shadow) {
        int dxPos = max(0, ceil(shadow.dx + shadow.radius)), dxNeg = min(0, ceil(shadow.dx - shadow.radius)),
            dyPos = max(0, ceil(shadow.dy + shadow.radius)), dyNeg = min(0, ceil(shadow.dy - shadow.radius));
        RectF shape = new RectF(
            0, 0, //  vvvvv       vvvvv basically we add abs(dx) unconditionally
            cornerX + dxPos + 1 - dxNeg + cornerX,
            cornerY + dyPos + 1 - dyNeg + cornerY
        );
        shape.offset(
            (cornersAndEdges & ANY_LEFT) != 0 ? paddings.left : -cornerX - dxPos,
            (cornersAndEdges & ANY_TOP) != 0 ? paddings.top : -cornerY - dyPos
        );
        if (this == BETWEEN_BOTTOM_AND_TOP) {
            shape.bottom = cornerY - dyNeg;
            shape.top = shape.bottom + paddings.bottom + 1 + paddings.top;
        } else if (this == BETWEEN_RIGHT_AND_LEFT) {
            shape.right = cornerX - dxNeg;
            shape.left = shape.right + paddings.right + 1 + paddings.left;
        }
        return shape;
    }
    @NonNull byte[] chunk(@NonNull Rect paddings, int cornerRadiusX, int cornerRadiusY, ShadowSpec shadow, int bgColor, int fillColor) {
        int dxPos = max(0, ceil(shadow.dx + shadow.radius)), dyPos = max(0, ceil(shadow.dy + shadow.radius));
        int left = paddings.left + cornerRadiusX + dxPos;
        int top = paddings.top + cornerRadiusY + dyPos;
        switch (this) {
            case TOP_LEFT:
                return chunk(paddings, left, top, 4, 1<<3, fillColor);
            case TOP_RIGHT:
                return chunk(paddings, 0, top, 4, 1<<2, fillColor);
            case BOTTOM_LEFT:
                return chunk(paddings, left, 0, 4, 1<<1, fillColor);
            case BOTTOM_RIGHT:
                return chunk(paddings, 0, 0, 4, 1, fillColor);
            case BOTH_LEFT:
                return chunk(paddings, left, top, 6, 1<<3, fillColor);
            case BOTH_TOP:
                return chunk(paddings, left, top, 6, 1<<4, fillColor);
            case BOTH_RIGHT:
                return chunk(paddings, 0, top, 6, 1<<2, fillColor);
            case BOTH_BOTTOM:
                return chunk(paddings, left, 0, 6, 1<<1, fillColor);
            case HORIZONTAL:
                return chunk(paddings, 0, top, 3, 1<<1, fillColor);
            case VERTICAL:
                return chunk(paddings, left, 0, 3, 1<<1, fillColor);
            case ALL:
                return chunk(paddings, left, top, 9, 1<<4, fillColor);
            case BETWEEN_BOTTOM_AND_TOP:
                int dyNeg = min(0, ceil(shadow.dy - shadow.radius));
                return chunk(paddings, left, -dyNeg + cornerRadiusY + paddings.bottom, 9, 1<<3|1<<4|1<<5, bgColor);
            case BETWEEN_RIGHT_AND_LEFT:
                int dxNeg = min(0, ceil(shadow.dx - shadow.radius));
                return chunk(paddings, -dxNeg + cornerRadiusX + paddings.right, top, 9, 1<<1|1<<4|1<<7, bgColor);
            default:
                throw new AssertionError();
        }
    }
    private byte[] chunk(Rect paddings, int xDiv, int yDiv, int colorCount, int colorsAt, int color) {
        //int xdl = xDivs.length, ydl = yDivs.length, colorCount = colors.length;
        //if (xdl > 255 || ydl > 255 || colorCount > 255) throw new ArithmeticException();
        byte[] chunk = new byte[32 + 4 * (4/*xdl + ydl*/ + colorCount)];
        chunk[0] = 1;
        chunk[1] = 2; //(byte) xdl;
        chunk[2] = 2; //(byte) ydl;
        chunk[3] = (byte) colorCount;
        putLe(chunk, 12, (cornersAndEdges & ANY_LEFT) != 0 ? paddings.left : 0);
        putLe(chunk, 16, (cornersAndEdges & ANY_RIGHT) != 0 ? paddings.right : 0);
        putLe(chunk, 20, (cornersAndEdges & ANY_TOP) != 0 ? paddings.top : 0);
        putLe(chunk, 24, (cornersAndEdges & ANY_BOTTOM) != 0 ? paddings.bottom : 0);
        // for (int j = 0; j < xdl; j++) putLe(chunk, i+=4, xDivs[j]); unrolled:
        putLe(chunk, 32, xDiv);
        putLe(chunk, 36, xDiv + 1);
        // for (int j = 0; j < ydl; j++) putLe(chunk, i+=4, yDivs[j]); unrolled:
        putLe(chunk, 40, yDiv);
        putLe(chunk, 44, yDiv + 1);
        for (int i = 44, j = 0; j < colorCount; j++)
            putLe(chunk, i += 4, (colorsAt & (1 << j)) == 0 ? 1 : color);
        return chunk;
    }

    @NonNull Drawable inset(@NonNull Drawable d, @NonNull Rect paddings) {
        boolean nbrl = this != BETWEEN_RIGHT_AND_LEFT;
        boolean nbbt = this != BETWEEN_BOTTOM_AND_TOP;
        return new InsetDrawable(d,
            nbrl && (cornersAndEdges & ANY_LEFT) != 0 ? -paddings.left : 0,
            nbbt && (cornersAndEdges & ANY_TOP) != 0 ? -paddings.top : 0,
            nbrl && (cornersAndEdges & ANY_RIGHT) != 0 ? -paddings.right : 0,
            nbbt && (cornersAndEdges & ANY_BOTTOM) != 0 ? -paddings.bottom : 0
        );
    }

    public static final List<CornerSet> VALUES = unmodifiableList(asList(values()));
}
