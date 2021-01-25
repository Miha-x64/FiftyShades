package net.aquadc.fiftyshades;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import static net.aquadc.fiftyshades.Numbers.requireNonNegative;

public final class RectWithShadow {
    private RectWithShadow() {}

    /**
     * Create a 9-patch containing a round rect with shadow.
     * @param fillColor    round rect fill colour
     * @param cornerRadius round rect corner radius
     * @param shadow       shadow spec
     * @return a 9-patch
     * @throws IllegalArgumentException if strokeWidth or cornerRadius is negative, infinite, or NaN, or stroke is wider than corner radius
     * @throws NullPointerException if shadow is null
     */
    @NonNull public static NinePatch createPatch(
        @ColorInt int fillColor, @Px int cornerRadius, @NonNull ShadowSpec shadow
    ) {
        return createPatch(fillColor, Color.TRANSPARENT, 0f, cornerRadius, cornerRadius, shadow, null);
    }

    /**
     * Create a 9-patch containing a round rect with shadow.
     * @param fillColor     round rect fill colour
     * @param strokeColor   round rect stroke colour
     * @param strokeWidth   round rect stroke width
     * @param cornerRadiusX round rect corner radius
     * @param cornerRadiusY round rect corner radius
     * @param shadow        shadow spec
     * @param paddings      spaces between edges of shape and edges of 9-patch. Will be inferred if null
     * @return a 9-patch
     * @throws IllegalArgumentException if strokeWidth or cornerRadius is negative, infinite, or NaN, or stroke is wider than corner radius
     * @throws NullPointerException if shadow is null
     */
    @NonNull public static NinePatch createPatch(
        @ColorInt int fillColor,
        @ColorInt int strokeColor, @Px float strokeWidth,
        @Px int cornerRadiusX, @Px int cornerRadiusY,
        @NonNull ShadowSpec shadow, @Nullable Rect paddings
    ) {
        requireNonNegative(strokeWidth, "strokeWidth");
        requireNonNegative(cornerRadiusX, "cornerRadiusX");
        requireNonNegative(cornerRadiusY, "cornerRadiusY");
        if (strokeWidth > cornerRadiusX || strokeWidth > cornerRadiusY)
            throw new IllegalArgumentException("strokeWidth must be <= cornerRadius");
        if (shadow == null) throw new NullPointerException("shadow must not be null");
        if (paddings == null) paddings = shadow.inferPaddings();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fillColor);
        paint.setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color);

        int right = paddings.left + cornerRadiusX + 1 + cornerRadiusX;
        int bottom = paddings.top + cornerRadiusY + 1 + cornerRadiusY;
        Bitmap bitmap = Bitmap.createBitmap(right + paddings.right, bottom + paddings.bottom, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawRoundRect(paddings.left, paddings.top, right, bottom, cornerRadiusX, cornerRadiusY, paint);
        if ((strokeColor >>> 24) != 0 && strokeWidth > 0f)
            andDrawStroke(canvas, paint, strokeColor, strokeWidth, paddings.left, paddings.top, right, bottom, cornerRadiusX, cornerRadiusY);

        byte[] chunk = chunkProto.clone();
        putLe(chunk, 12, paddings.left);
        putLe(chunk, 16, paddings.right);
        putLe(chunk, 20, paddings.top);
        putLe(chunk, 24, paddings.bottom);
        putLe(chunk, 32, paddings.left + cornerRadiusX);
        putLe(chunk, 36, paddings.left + cornerRadiusX + 1);
        putLe(chunk, 40, paddings.top + cornerRadiusY);
        putLe(chunk, 44, paddings.top + cornerRadiusY + 1);
        putLe(chunk, 64, fillColor);
        return new NinePatch(bitmap, chunk);
    }
    private static void andDrawStroke(
        Canvas canvas, Paint paint, int color, float width, int left, int top, int right, int bottom, int rx, int ry
    ) {
        paint.setShadowLayer(0f, 0f, 0f, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        canvas.drawRoundRect(left, top, right, bottom, rx, ry, paint);
    }
    private static final byte[] chunkProto = new byte[]{
        1, /*2*xDivs=*/2, /*2*yDivs=*/2, /*colors.length=*/9,
        0, 0, 0, 0, 0, 0, 0, 0, // unknown
        /*paddingLeft=*/0, 0, 0, 0,
        /*paddingRight=*/0, 0, 0, 0,
        /*paddingTop=*/0, 0, 0, 0,
        /*paddingBottom=*/0, 0, 0, 0,
        0, 0, 0, 0, // unknown
        /*xDiv[0].start=*/ 0, 0, 0, 0,
        /*xDiv[0].stop=*/ 0, 0, 0, 0,
        /*yDiv[0].start=*/ 0, 0, 0, 0,
        /*yDiv[0].stop=*/ 0, 0, 0, 0,
        /*colors[0]=*/ 1, 0, 0, 0, /*colors[1]=*/ 1, 0, 0, 0, /*colors[2]=*/ 1, 0, 0, 0,
        /*colors[3]=*/ 1, 0, 0, 0, /*colors[4]=*/ 1, 0, 0, 0, /*colors[5]=*/ 1, 0, 0, 0,
        /*colors[6]=*/ 1, 0, 0, 0, /*colors[7]=*/ 1, 0, 0, 0, /*colors[8]=*/ 1, 0, 0, 0,
    };
    private static void putLe(byte[] to, int at, int what) {
        to[at] = (byte) (what & 0xFF);
        to[++at] = (byte) ((what >>> 8) & 0xFF);
        to[++at] = (byte) ((what >>> 16) & 0xFF);
        to[++at] = (byte) ((what >>> 24) & 0xFF);
    }

}
