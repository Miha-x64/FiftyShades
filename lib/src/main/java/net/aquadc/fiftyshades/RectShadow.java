package net.aquadc.fiftyshades;

import static net.aquadc.fiftyshades.Numbers.ceil;
import static net.aquadc.fiftyshades.Numbers.multiplyAlpha;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

/**
 * A shadow dropped by a rectangle with rounded corners.
 */
public final class RectShadow extends Shadow {

    RectShadow(ShadowState state) {
        super(state, Paint.DITHER_FLAG);
    }
    public RectShadow() {
        this(0, new ShadowSpec());
    }
    public RectShadow(@Px int cornerRadius, @NonNull ShadowSpec shadow) {
        super(cornerRadius, new ShadowSpec(shadow), false, Paint.DITHER_FLAG);
    }
    public RectShadow(@Px int cornerRadius, @Px float dx, @Px float dy, @Px float radius, @ColorInt int color) {
        super(cornerRadius, new ShadowSpec(dx, dy, radius, color), false, Paint.DITHER_FLAG);
    }

    // setters

    public RectShadow cornerRadius(int cornerRadius) { return (RectShadow) super.cornerRadius(cornerRadius); }
    public RectShadow shadowColor(@ColorInt int color) { return (RectShadow) super.shadowColor(color); }
    public RectShadow shadowDx(@Px float dx) { return (RectShadow) super.shadowDx(dx); }
    public RectShadow shadowDy(@Px float dy) { return (RectShadow) super.shadowDy(dy); }
    public RectShadow shadowRadius(@Px float radius) { return (RectShadow) super.shadowRadius(radius); }
    @Override public RectShadow shadow(@NonNull ShadowSpec shadow) { return (RectShadow) super.shadow(shadow); }

    // invalidation

    @Override public void setBounds(int left, int top, int right, int bottom) {
        int corners = boundedCornerRadius();
        float gradi = cornerGradientRadiusInside();
        super.setBounds(left, top, right, bottom);
        if (corners != boundedCornerRadius() || gradi != cornerGradientRadiusInside())
            radiusInvalidated();
    }

    @Override void radiusInvalidated() { paint.setShader(cornerShader = null); }
    @Override void shadowRadiusInvalidated() { radiusInvalidated(); shadowInvalidated(); }
    @Override void shadowColorInvalidated() { shadowInvalidated(); }
    private void shadowInvalidated() { paint.setShader(cornerShader = edgeShader = null); }

    // drawing

    private final int[] linearColors = new int[5];
    private final int[] radialColors = new int[5];
    private final float[] radialPositions = { Float.NaN, Float.NaN, Float.NaN, Float.NaN, 1f /* radius */ };
    private Shader cornerShader, edgeShader;
    @Override public void draw(@NonNull Canvas canvas) {
        if (Color.alpha(state.shadow.color) == 0) return;

        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();

        canvas.save();
        canvas.translate(
            round((width < 0 ? bounds.centerX() : bounds.left) + state.shadow.dx),
            round((height < 0 ? bounds.centerY() : bounds.top) + state.shadow.dy)
        ); //             ^^^ guard against drawing ugly shadow when we're squeezed. This looks quite possible in InsetDrawable
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        int cornerRadius = boundedCornerRadius();

        float shRad = state.shadow.radius * GaussianInterpolator.GAUSSIAN_FADE_AWAY;
        float shRadHalf = shRad / 2f;

        // Shadow middle is located exactly on the edge;
        // it strengthens to the inside and weakens to the outside.
        float gradientRadiusInside = cornerGradientRadiusInside();
        float gradientRadius = gradientRadiusInside + shRadHalf;
        if (cornerShader == null && shRad > 0) buildCornerShader(cornerRadius, shRad, gradientRadius);
        int inset = max(0, round(gradientRadiusInside) - cornerRadius); // move corner gradients inside when blur radius is big
        drawCorners(canvas, cornerRadius, width, height, inset, gradientRadius);
        if (edgeShader == null && shRad > 0) buildEdgeShader(shRadHalf);
        drawEdges(canvas, width, height, cornerRadius, inset, shRadHalf, gradientRadius);
        canvas.restore();
    }

    private float cornerGradientRadiusInside() {
        return min(maxCornerRadius(), max(state.cornerRadius, state.shadow.radius * GaussianInterpolator.GAUSSIAN_FADE_AWAY / 2f));
    }

    private void buildCornerShader(int cornerRadius, float shRad, float gradientRadius) {
        int shCol = state.shadow.color;
        radialColors[0] = shCol;
        radialColors[1] = multiplyAlpha(shCol, THREE_QUARTERS_MULTIPLIER);
        radialColors[2] = multiplyAlpha(shCol, MID_MULTIPLIER);
        radialColors[3] = multiplyAlpha(shCol, QUARTER_MULTIPLIER);
        radialColors[4] = 0xFFFFFF & shCol;

        // fixme: when squeezed, radialPositions = [0f, 0f, ...], corners look a bit ugly
        radialPositions[0] = max(0, gradientRadius - shRad) / gradientRadius; // shadow begins at shRad inside the shape, not at the edge!
        radialPositions[1] = max(0, gradientRadius - .75f * shRad) / gradientRadius;
        radialPositions[2] = max(0, gradientRadius - .5f * shRad) / gradientRadius;
        radialPositions[3] = max(0, gradientRadius - .25f * shRad) / gradientRadius;

        cornerShader =
                new RadialGradient(cornerRadius, cornerRadius, gradientRadius, radialColors, radialPositions, Shader.TileMode.CLAMP);
    }
    private void drawCorners(Canvas canvas, int cornerRadius, int width, int height, int inset, float gRad) {
        paint.setColor(state.shadow.color);
        paint.setShader(cornerShader);
        int cornerDiameter = cornerRadius + cornerRadius;
        int gRadInt = ceil(gRad);
        // top left:
        canvas.translate(inset, inset);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius - gRadInt, cornerRadius - gRadInt, cornerRadius, cornerRadius);
        // top right:
        canvas.translate(width - cornerDiameter - inset - inset, 0f);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius, cornerRadius - gRadInt, cornerRadius + gRadInt, cornerRadius);
        // bottom right:
        canvas.translate(0f, height - cornerDiameter - inset - inset);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius, cornerRadius, cornerRadius + gRadInt, cornerRadius + gRadInt);
        // bottom left:
        canvas.translate(-width + cornerDiameter + inset + inset, 0f);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius - gRadInt, cornerRadius, cornerRadius, cornerRadius + gRadInt);
        // restore
        canvas.translate(-inset, -height + cornerDiameter + inset);
    }
    private void drawCorner(
        Canvas canvas, int cornerRadius, float gradientRadius,
        int clipL, int clipT, int clipR, int clipB
    ) {
        canvas.save();
        canvas.clipRect(clipL, clipT, clipR, clipB);
//        canvas.drawColor(0x40_000000); // visualize clip rects
        canvas.drawCircle(cornerRadius, cornerRadius, gradientRadius, paint);
        canvas.restore();
    }

    /*
     * Drawing non-corners.
     *
     *    ┌──────────┐
     *  ┌─┼──────────┼──┐  The first implementation was drawing edges, leaving inner space empty.
     *  │ │  e       │  │  That was inappropriate for transparent foreground:
     *  │ │   m      │  │  the shadow must be solid.
     *  │ │    p     │  │
     *  │ │     t    │  │
     *  │ │      y   │  │
     *  └─┼──────────┼──┘
     *    └──────────┘
     *
     *    ┌──────────┐
     *  ┌──🮢         🮣──┐  The second version used to draw four quarters.
     *  │   \      /    │  The problem appears when width or height can't accommodate
     *  │    \ 2 /      │  for shadow radius, and thus 1 and 3 colors are different from 2 and 4.
     *  │    1\/        │
     *  │     /\3       │
     *  │    / 4 \      │
     *  │   /      \    │
     *  └──🮠        🮡───┘
     *    └──────────┘
     *
     *    ┌───────────┐
     *  ┌─┴─────┬─────┴─┐  Current option.
     *  │       │       │
     *  │       │       │
     *  │       │       │
     *  │       │       │
     *  │       │       │
     *  └─┬─────┴─────┬─┘
     *    └───────────┘
     */

    private void buildEdgeShader(float shRadHalf) {
        int shCol = state.shadow.color;
        linearColors[0] = 0xFFFFFF & shCol;
        linearColors[1] = multiplyAlpha(shCol, QUARTER_MULTIPLIER);
        linearColors[2] = multiplyAlpha(shCol, MID_MULTIPLIER);
        linearColors[3] = multiplyAlpha(shCol, THREE_QUARTERS_MULTIPLIER);
        linearColors[4] = shCol;
        edgeShader = new LinearGradient(0f, -shRadHalf, 0f, shRadHalf, linearColors, null, Shader.TileMode.CLAMP);
    }
    private void drawEdges(Canvas canvas, int width, int height, int cornerRadius, int inset, float shRadHalf, float gradientRadius) {
        paint.setColor(state.shadow.color); // shader may be null if shadowRadius == 0
        paint.setShader(edgeShader);
//        paint.setColor(0x40_000000); // visualize painting area
//        paint.setShader(null);
        int start = cornerRadius + inset;
        int lenH = width - cornerRadius - inset;
        boolean wide = width > height;
        float innerH = wide ? height / 2f : gradientRadius - shRadHalf;
        canvas.drawRect(start, -shRadHalf, lenH, innerH, paint);
        float angle = wide ? -90f : 90f;
        float halfMinSize = min(width, height) / 2f;
        canvas.rotate(angle, halfMinSize, halfMinSize);
        int lenV = height - cornerRadius - inset;
        float innerV = wide ? gradientRadius - shRadHalf : width / 2f;
        canvas.drawRect(start, -shRadHalf, lenV, innerV, paint);
        float halfMaxSize = max(width, height) / 2f;
        canvas.rotate(angle, halfMaxSize, halfMaxSize);
        canvas.drawRect(start, -shRadHalf, lenH, innerH, paint);
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawRect(start, -shRadHalf, lenV, innerV, paint);
    }
}
