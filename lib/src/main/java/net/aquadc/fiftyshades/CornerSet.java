package net.aquadc.fiftyshades;

import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.NonNull;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static net.aquadc.fiftyshades.Numbers.ceil;
import static net.aquadc.fiftyshades.Numbers.putLe;

/**
 * Enumerates all possible corner combinations.
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

    // if you want just an edge, set zero radius and use negative paddings to eat corners
    // with one of four following options
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

    int measureWidth(@NonNull Rect paddings, int cornerRadiusX, ShadowSpec shadow) {
        boolean anyLeftCorner = (cornersAndEdges & (1 | (1 << 6))) != 0;
        boolean anyHorizontalEdge = (cornersAndEdges & ((1 << 1) | (1 << 5))) != 0;
        boolean anyRightCorner = (cornersAndEdges & ((1 << 2) | (1 << 4))) != 0;
        return measure(cornerRadiusX, shadow.dx, shadow.radius,
            anyLeftCorner, anyHorizontalEdge || this == BETWEEN_RIGHT_AND_LEFT, anyRightCorner,
            paddings.left, paddings.right
        );
    }
    int measureHeight(@NonNull Rect paddings, int cornerRadiusY, ShadowSpec shadow) {
        boolean anyTopCorner = (cornersAndEdges & (1 | (1 << 2))) != 0;
        boolean anyVerticalEdge = (cornersAndEdges & ((1 << 3) | (1 << 7))) != 0;
        boolean anyBottomCorner = (cornersAndEdges & ((1 << 4) | (1 << 6))) != 0;
        return measure(cornerRadiusY, shadow.dx, shadow.radius,
            anyTopCorner, anyVerticalEdge || this == BETWEEN_BOTTOM_AND_TOP, anyBottomCorner,
            paddings.top, paddings.bottom
        );
    }
    private int measure(int cornerRadius, float d, float r, boolean anyStartCorner, boolean anyEdge, boolean anyEndCorner, int start, int end) {
        int dPos = max(0, ceil(d + r)), dNeg = min(0, ceil(d - r));
        return (anyStartCorner ? start + cornerRadius + dPos : 0) +
            (anyEdge ? 1 : 0) +
            (anyEndCorner ? -dNeg + cornerRadius + end : 0);
    }

    @NonNull RectF layout(@NonNull Rect paddings, int cornerRadiusX, int cornerRadiusY, ShadowSpec shadow) {
        int dxPos = max(0, ceil(shadow.dx + shadow.radius)), dxNeg = min(0, ceil(shadow.dx - shadow.radius)),
            dyPos = max(0, ceil(shadow.dy + shadow.radius)), dyNeg = min(0, ceil(shadow.dy - shadow.radius));
        RectF shape = new RectF(
            0, 0, //        vvvvv       vvvvv basically we add abs(dx) unconditionally
            cornerRadiusX + dxPos + 1 - dxNeg + cornerRadiusX,
            cornerRadiusY + dyPos + 1 - dyNeg + cornerRadiusY
        );
        shape.offset(
            /*anyLeftCorner*/(cornersAndEdges & (1 | (1 << 6))) != 0 ? paddings.left : -cornerRadiusX - dxPos,
            /*anyTopCorner*/(cornersAndEdges & (1 | (1 << 2))) != 0 ? paddings.top : -cornerRadiusY - dyPos
        );
        if (this == BETWEEN_BOTTOM_AND_TOP) {
            shape.bottom = cornerRadiusY;
            shape.top = shape.bottom + 1 + paddings.bottom + paddings.top;
        } else if (this == BETWEEN_RIGHT_AND_LEFT) {
            shape.right = 1 + cornerRadiusX;
            shape.left = shape.right + paddings.right + paddings.left;
        }
        return shape;
    }

    private static final int[] EDGE = { 0, 1 };
    @NonNull byte[] chunk(@NonNull Rect paddings, int cornerRadiusX, int cornerRadiusY, ShadowSpec shadow, int bgColor, int fillColor) {
        int dxPos = max(0, ceil(shadow.dx + shadow.radius)), dxNeg = min(0, ceil(shadow.dx - shadow.radius)),
            dyPos = max(0, ceil(shadow.dy + shadow.radius)), dyNeg = min(0, ceil(shadow.dy - shadow.radius));
        int left = paddings.left + cornerRadiusX + dxPos;
        int top = paddings.top + cornerRadiusY + dyPos;
        switch (this) {
            case TOP_LEFT:
                return chunk(paddings, plusOne(left), plusOne(top), new int[] { 1, 1, 1, fillColor });
            case TOP_RIGHT:
                return chunk(paddings, EDGE, plusOne(top), new int[] { 1, 1, fillColor, 1 });
            case BOTTOM_LEFT:
                return chunk(paddings, plusOne(left), EDGE, new int[] { 1, fillColor, 1, 1 });
            case BOTTOM_RIGHT:
                return chunk(paddings, EDGE, EDGE, new int[] { fillColor, 1, 1, 1 });
            case BOTH_LEFT:
                return chunk(paddings, plusOne(left), plusOne(top), new int[] { 1, 1, 1, fillColor, 1, 1 });
            case BOTH_TOP:
                return chunk(paddings, plusOne(left), plusOne(top), new int[] { 1, 1, 1, 1, fillColor, 1 });
            case BOTH_RIGHT:
                return chunk(paddings, EDGE, plusOne(top), new int[] { 1, 1, fillColor, 1, 1, 1 });
            case BOTH_BOTTOM:
                return chunk(paddings, plusOne(left), EDGE, new int[] { 1, fillColor, 1, 1, 1, 1 });
            case ALL:
                return chunk(paddings, plusOne(left), plusOne(top), new int[] { 1, 1, 1, 1, fillColor, 1, 1, 1, 1 });
            case BETWEEN_BOTTOM_AND_TOP:
                return chunk(paddings, plusOne(left), plusOne(-dyNeg + cornerRadiusY + paddings.bottom),
                    new int[] { 1, 1, 1, bgColor, bgColor, bgColor, 1, 1, 1 });
            case BETWEEN_RIGHT_AND_LEFT:
                return chunk(paddings, plusOne(-dxNeg + cornerRadiusX + paddings.right), plusOne(top),
                    new int[] { 1, bgColor, 1, 1, bgColor, 1, 1, bgColor, 1 });
            default:
                throw new AssertionError();
        }
    }
    private int[] plusOne(int n) {
        return new int[] { n, n + 1 };
    }
    private byte[] chunk(Rect paddings, int[] xDivs, int[] yDivs, int[] colors) {
        int xdl = xDivs.length;
        int ydl = yDivs.length;
        int cl = colors.length;
        if (xdl > 255 || ydl > 255 || cl > 255) throw new ArithmeticException();
        int colorsOffset = 32 + 4*xdl + 4*ydl;
        byte[] chunk = new byte[colorsOffset + 4*cl];
        chunk[0] = 1;
        chunk[1] = (byte) xdl;
        chunk[2] = (byte) ydl;
        chunk[3] = (byte) cl;
        putLe(chunk, 12, (cornersAndEdges & (1 | (1 << 6))) != 0 ? paddings.left : 0);
        putLe(chunk, 16, (cornersAndEdges & ((1 << 2) | (1 << 4))) != 0 ? paddings.right : 0);
        putLe(chunk, 20, (cornersAndEdges & (1 | (1 << 2))) != 0 ? paddings.top : 0);
        putLe(chunk, 24, (cornersAndEdges & ((1 << 4) | (1 << 6))) != 0 ? paddings.bottom : 0);
        int i = 32;
        for (int j = 0; j < xdl; i+=4, j++)
            putLe(chunk, i, xDivs[j]);
        for (int j = 0; j < ydl; i+=4, j++)
            putLe(chunk, i, yDivs[j]);
        for (int j = 0; j < cl; i+=4, j++)
            putLe(chunk, i, colors[j]);
        return chunk;
    }

    public static final List<CornerSet> VALUES = unmodifiableList(asList(values()));
}
