package net.aquadc.fiftyshades;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.NinePatchDrawable;
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
        return createPatch(
            Color.TRANSPARENT, fillColor, Color.TRANSPARENT, 0f, cornerRadius, cornerRadius, shadow, null, CornerSet.ALL
        );
    }

    /**
     * Create a 9-patch containing a round rect with shadow.
     * @param bgColor       colour under the sheet and shadow, effectively colour of paddings
     * @param fillColor     round rect fill colour
     * @param strokeColor   round rect stroke colour
     * @param strokeWidth   round rect stroke width
     * @param cornerRadiusX round rect corner radius
     * @param cornerRadiusY round rect corner radius
     * @param shadow        shadow spec
     * @param paddings      spaces between edges of shape and edges of 9-patch. Will be inferred if null
     * @param corners       which corners should be drawn
     * @return a 9-patch
     * @throws IllegalArgumentException if strokeWidth or cornerRadius is negative, infinite, or NaN, or stroke is wider than corner radius, or paddings are way too negative
     * @throws NullPointerException if shadow or corners are null
     */
    @NonNull public static NinePatch createPatch(
        @ColorInt int bgColor,
        @ColorInt int fillColor,
        @ColorInt int strokeColor, @Px float strokeWidth,
        @Px int cornerRadiusX, @Px int cornerRadiusY,
        @NonNull ShadowSpec shadow, @Nullable Rect paddings,
        @NonNull CornerSet corners
    ) {
        if (paddings == null) paddings = shadow.inferPaddings();
        return new NinePatch(
            bitmap(bgColor, fillColor, strokeColor, strokeWidth, cornerRadiusX, cornerRadiusY, shadow, paddings, corners),
            corners.chunk(paddings, cornerRadiusX, cornerRadiusY, bgColor, fillColor),
            null
        );
    }

    /**
     * Create a drawable containing a stretchable shape
     * from {@link #createPatch(int, int, ShadowSpec)}
     * with negative insets to draw shadow out of bounds.
     */
    @NonNull public static Drawable createDrawable(
        @ColorInt int fillColor, @Px int cornerRadius, @NonNull ShadowSpec shadow
    ) {
        return createDrawable(
            Color.TRANSPARENT, fillColor, Color.TRANSPARENT, 0f, cornerRadius, cornerRadius, shadow, null, CornerSet.ALL
        );
    }

    /**
     * Create a drawable containing a stretchable shape
     * from {@link #createPatch(int, int, int, float, int, int, ShadowSpec, Rect, CornerSet)}
     * with negative insets to draw shadow out of bounds.
     */
    @NonNull public static Drawable createDrawable(
        @ColorInt int bgColor,
        @ColorInt int fillColor,
        @ColorInt int strokeColor, @Px float strokeWidth,
        @Px int cornerRadiusX, @Px int cornerRadiusY,
        @NonNull ShadowSpec shadow, @Nullable Rect paddings,
        @NonNull CornerSet corners
    ) {
        if (paddings == null) paddings = shadow.inferPaddings();
        return new InsetDrawable(new NinePatchDrawable(
            null,
            createPatch(
                bgColor, fillColor, strokeColor, strokeWidth, cornerRadiusX, cornerRadiusY, shadow, paddings, corners
            )
        ), -paddings.left, -paddings.top, -paddings.right, -paddings.bottom);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static Bitmap bitmap(
        @ColorInt int bgColor,
        @ColorInt int fillColor,
        @ColorInt int strokeColor, @Px float strokeWidth,
        @Px int cornerRadiusX, @Px int cornerRadiusY,
        @NonNull ShadowSpec shadow, @NonNull Rect paddings,
        @NonNull CornerSet corners
    ) {
        requireNonNegative(strokeWidth, "strokeWidth");
        requireNonNegative(cornerRadiusX, "cornerRadiusX");
        requireNonNegative(cornerRadiusY, "cornerRadiusY");
        if (strokeWidth > cornerRadiusX || strokeWidth > cornerRadiusY)
            throw new IllegalArgumentException("strokeWidth must be <= cornerRadius");
        if (paddings.left < -cornerRadiusX || paddings.top < -cornerRadiusY ||
            paddings.right < -cornerRadiusX || paddings.bottom < -cornerRadiusY)
            throw new IllegalArgumentException("negative paddings (" + paddings.flattenToString() +
                ") are eating corners (" + cornerRadiusX + ", " + cornerRadiusY + ')');

        Bitmap bitmap = Bitmap.createBitmap(
            corners.measureWidth(paddings, cornerRadiusX), // implicit null-check for corners
            corners.measureHeight(paddings, cornerRadiusY),
            Bitmap.Config.ARGB_8888);

        if (bgColor != Color.TRANSPARENT) bitmap.eraseColor(bgColor);
        // I could check for (bgColor >>> 24 != 0) but I assume you have really good reason to redraw transparent pixels

        RectF shape = corners.layout(paddings, cornerRadiusX, cornerRadiusY);
        final Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fillColor);
        paint.setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color);
        drawRR(canvas, shape, cornerRadiusX, cornerRadiusY, paint);
        if ((strokeColor >>> 24) != 0 && strokeWidth > 0f)
            andDrawStroke(canvas, paint, strokeColor, strokeWidth, shape, cornerRadiusX, cornerRadiusY);
        return bitmap;
    }
    private static void andDrawStroke(Canvas canvas, Paint paint, int color, float width, RectF bounds, int rx, int ry) {
        paint.setShadowLayer(0f, 0f, 0f, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        drawRR(canvas, bounds, rx, ry, paint);
    }
    private static void drawRR(Canvas canvas, RectF bounds, int rx, int ry, Paint paint) {
        if (bounds.left < bounds.right && bounds.top < bounds.bottom) {
            canvas.drawRoundRect(bounds, rx, ry, paint);
        } else if (bounds.left > bounds.right) {
            float tmp = bounds.left;
            bounds.left = -rx;
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.left = tmp;

            tmp = bounds.right;
            bounds.right = canvas.getWidth() + rx;
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.right = tmp;
        } else { // top > bottom
            float tmp = bounds.top;
            bounds.top = -ry;
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.top = tmp;

            tmp = bounds.bottom;
            bounds.bottom = canvas.getHeight() + ry;
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.bottom = tmp;
        }
    }

}
