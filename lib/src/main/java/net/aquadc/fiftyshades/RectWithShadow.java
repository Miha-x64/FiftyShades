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

import static java.lang.Math.max;
import static net.aquadc.fiftyshades.Numbers.ceil;

/**
 * A factory of patches containing round rect drawable with shadow.
 */
public final class RectWithShadow {
    private RectWithShadow() {}

    /**
     * Create a 9-patch containing a round rect with shadow.
     * @param fillColor    round rect fill colour
     * @param cornerRadius round rect corner radius
     * @param shadow       shadow spec
     * @return a 9-patch
     * @throws IllegalArgumentException if strokeWidth or cornerRadius is negative, infinite, or NaN
     * @throws NullPointerException if shadow is null
     */
    @NonNull public static NinePatch createPatch(
        @ColorInt int fillColor, @Px int cornerRadius, @NonNull ShadowSpec shadow
    ) {
        return createPatch(Color.TRANSPARENT, new RectSpec(fillColor, cornerRadius), shadow, null, CornerSet.ALL);
    }

    /**
     * Create a 9-patch containing a round rect with shadow.
     * @param bgColor       colour under the sheet and shadow, effectively colour of paddings
     * @param rect          shape appearance
     * @param shadow        shadow spec
     * @param paddings      spaces between edges of shape and edges of 9-patch. Will be inferred if null
     * @param corners       which corners should be drawn
     * @return a 9-patch
     * @throws IllegalArgumentException if strokeWidth or cornerRadius is negative, infinite, or NaN,  or paddings are way too negative
     * @throws NullPointerException if rect, shadow or corners are null
     */
    @NonNull public static NinePatch createPatch(
        @ColorInt int bgColor,
        @NonNull RectSpec rect,
        @NonNull ShadowSpec shadow,
        @Nullable Rect paddings,
        @NonNull CornerSet corners
    ) {
        if (paddings == null) paddings = shadow.inferPaddings();
        int corner = max(rect.cornerRadius, ceil(rect.strokeWidth));
        return new NinePatch(
            bitmap(bgColor, rect, shadow, paddings, corners),
            corners.chunk(paddings, corner, corner, shadow, bgColor, rect.fillColor),
            null
        );
    }

    /**
     * Create a drawable containing a stretchable shape
     * from {@link #createPatch(int, int, ShadowSpec)}
     * with negative insets to draw shadow out of bounds.
     */
    @NonNull public static Drawable createDrawable(@NonNull RectSpec rect, @NonNull ShadowSpec shadow) {
        return createDrawable(Color.TRANSPARENT, rect, shadow, null, CornerSet.ALL);
    }

    /**
     * Create a drawable containing a stretchable shape
     * from {@link #createPatch(int, RectSpec, ShadowSpec, Rect, CornerSet)}
     * with negative insets to draw shadow out of bounds.
     */
    @NonNull public static Drawable createDrawable(
        @ColorInt int bgColor,
        @NonNull RectSpec rect,
        @NonNull ShadowSpec shadow,
        @Nullable Rect paddings,
        @NonNull CornerSet corners
    ) {
        if (paddings == null) paddings = shadow.inferPaddings();
        return new InsetDrawable(
            new NinePatchDrawable(null, createPatch(bgColor, rect, shadow, paddings, corners)),
            -paddings.left, -paddings.top, -paddings.right, -paddings.bottom
        );
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static Bitmap bitmap(
        @ColorInt int bgColor,
        @NonNull RectSpec rect,
        @NonNull ShadowSpec shadow,
        @NonNull Rect paddings,
        @NonNull CornerSet corners
    ) {
        int cornerRadius = rect.cornerRadius;
        int corner = max(cornerRadius, ceil(rect.strokeWidth));
        if (paddings.left < -corner || paddings.top < -corner || paddings.right < -corner || paddings.bottom < -corner)
            throw new IllegalArgumentException(
                "negative paddings (" + paddings.flattenToString() + ") are eating corners (" + cornerRadius +
                    ") or stroke (" + rect.strokeWidth + ')');

        Bitmap bitmap = Bitmap.createBitmap(
            corners.measureWidth(paddings, corner, shadow),
            corners.measureHeight(paddings, corner, shadow),
            Bitmap.Config.ARGB_8888);

        if (bgColor != Color.TRANSPARENT) bitmap.eraseColor(bgColor);
        // I could check for (bgColor >>> 24 != 0) but I assume you have really good reason to redraw transparent pixels

        RectF shape = corners.layout(paddings, corner, corner, shadow);
        final Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(rect.fillColor);
        paint.setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color);
        drawRR(canvas, shape, cornerRadius, cornerRadius, paint);
        if (rect.hasVisibleStroke())
            andDrawStroke(canvas, paint, rect.strokeColor, rect.strokeWidth, shape, cornerRadius, cornerRadius);
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
        float width, height;
        if ((width = bounds.width()) > 0f & (height = bounds.height()) > 0f) {
            canvas.drawRoundRect(bounds, rx, ry, paint);
        } else if (width < 0) {
            float tmp = bounds.left;
            bounds.left = width; // *minus* width
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.left = tmp;

            tmp = bounds.right;
            bounds.right = canvas.getWidth() - width; // *plus* width
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.right = tmp;
        } else { // height < 0
            float tmp = bounds.top;
            bounds.top = height; // *minus* height
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.top = tmp;

            tmp = bounds.bottom;
            bounds.bottom = canvas.getHeight() - height; // *plus* height
            canvas.drawRoundRect(bounds, rx, ry, paint);
            bounds.bottom = tmp;
        }
    }

}
