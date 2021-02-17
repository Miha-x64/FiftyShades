package net.aquadc.fiftyshades;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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
        this(0, 0f, 0f, 0f, Color.TRANSPARENT);
    }
    public RectShadow(@Px int cornerRadius, @NonNull ShadowSpec shadow) {
        this(cornerRadius, shadow.dx, shadow.dy, shadow.radius, shadow.color);
    }
    public RectShadow(@Px int cornerRadius, @Px float dx, @Px float dy, @Px float radius, @ColorInt int color) {
        super(new ShadowState(cornerRadius, dx, dy, radius, color, false), Paint.DITHER_FLAG);
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
        int corners = boundedCornerRadius(), width = getBounds().width(), height = getBounds().height();
        super.setBounds(left, top, right, bottom);

        if (width != right - left) horizontalEdge.rewind();
        if (height != bottom - top) verticalEdge.rewind();

        // quite rare cases when we're extremely small
        if (corners != boundedCornerRadius()) radiusInvalidated();
    }

    @Override void radiusInvalidated() { cornerShader = null; verticalEdge.rewind(); horizontalEdge.rewind(); }
    @Override void shadowOffsetInvalidated() { cornerShader = null; }
    @Override void shadowRadiusInvalidated() { radiusInvalidated(); shadowInvalidated(); }
    @Override void shadowColorInvalidated() { shadowInvalidated(); }
    private void shadowInvalidated() { cornerShader = null; edgeShader = null; }

    // drawing

    private final int[] radialColors = new int[3];
    private final float[] radialPositions = { 0f, Float.NaN, 1f };
    private RadialGradient cornerShader;
    private LinearGradient edgeShader;
    private final Path horizontalEdge = new Path(), verticalEdge = new Path();
    @Override public void draw(@NonNull Canvas canvas) {
        if (Color.alpha(state.shadow.color) == 0) return;

        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();

        canvas.save();
        canvas.translate(
            round((width < 0 ? bounds.centerX() : bounds.left) + state.shadow.dx),
            round((height < 0 ? bounds.centerY() : bounds.top) + state.shadow.dy)
        ); //       ^^^ guard against drawing ugly shadow when we're squeezed. This looks quite possible in InsetDrawable
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        int cornerRadius = boundedCornerRadius();
        if (cornerShader == null) buildCornerShader(cornerRadius, cornerRadius + state.shadow.radius/2f);
        drawCorners(canvas, cornerRadius, width, height);
        if (edgeShader == null) buildEdgeShader();
        int shRadInt = round(state.shadow.radius / 2f);
        if (horizontalEdge.isEmpty()) buildEdgePath(cornerRadius, width, height, shRadInt, horizontalEdge);
        if (verticalEdge.isEmpty()) buildEdgePath(cornerRadius, height, width, shRadInt, verticalEdge);
        drawEdges(canvas, width, height);
        canvas.restore();
    }

    private void drawCorners(Canvas canvas, int cornerRadius, int width, int height) {
        float shRad = state.shadow.radius/2f;
        float gRad = cornerRadius + shRad;
        paint.setShader(cornerShader);
        int shadowRadInt = round(shRad);
        drawCorner(canvas, cornerRadius, gRad, -shadowRadInt, -shadowRadInt, cornerRadius, cornerRadius);
        int cornerDiameter = cornerRadius + cornerRadius;
        canvas.translate(width - cornerDiameter, 0f);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius, -shadowRadInt, cornerDiameter + shadowRadInt, cornerRadius);
        canvas.translate(0f, height - cornerDiameter);
        drawCorner(canvas, cornerRadius, gRad, cornerRadius, cornerRadius, cornerDiameter + shadowRadInt, cornerDiameter + shadowRadInt);
        canvas.translate(-width + cornerDiameter, 0f);
        drawCorner(canvas, cornerRadius, gRad, -shadowRadInt, cornerRadius, cornerRadius, cornerDiameter + shadowRadInt);
        canvas.translate(0f, -height + cornerDiameter);
    }
    private void buildCornerShader(int cornerRadius, float gRad) {
        int shCol = state.shadow.color;
        float shRad = state.shadow.radius;
        int transparent = 0xFFFFFF & shCol;
        radialColors[0] = radialColors[1] = shCol;
        radialColors[2] = transparent;
        cornerShader = (radialPositions[1] = (cornerRadius - shRad/2f) / gRad) <= 0f
            ? new RadialGradient(cornerRadius, cornerRadius, gRad, multiplyAlpha(shCol, (cornerRadius+shRad/2f)/shRad), transparent, Shader.TileMode.CLAMP)
            : new RadialGradient(cornerRadius, cornerRadius, gRad, radialColors, radialPositions, Shader.TileMode.CLAMP);
    }
    private static int multiplyAlpha(int color, float alpha) {
        System.out.println(alpha);
        return ((int) (Color.alpha(color) * alpha)) << 24 | (0xFFFFFF & color);
    }
    private void drawCorner(
        Canvas canvas, int cornerRadius, float gradientRadius,
        int clipL, int clipT, int clipR, int clipB
    ) {
        canvas.save();
        canvas.clipRect(clipL, clipT, clipR, clipB);
        canvas.drawCircle(cornerRadius, cornerRadius, gradientRadius, paint);
        canvas.restore();
    }

    private void drawEdges(Canvas canvas, int width, int height) {
        paint.setShader(edgeShader);
        canvas.drawPath(horizontalEdge, paint);
        float angle = width > height ? -90f : 90f;
        float halfMinSize = min(width, height) / 2f;
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawPath(verticalEdge, paint);
        float halfMaxSize = max(width, height) / 2f;
        canvas.rotate(angle, halfMaxSize, halfMaxSize);
        canvas.drawPath(horizontalEdge, paint);
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawPath(verticalEdge, paint);
    }
    private static void buildEdgePath(int cornerRadius, int size1, int size2, int shRadInt, Path path) {
        path.moveTo(size1 / 2f, size2 / 2f);
        path.lineTo(cornerRadius, cornerRadius);
        path.lineTo(cornerRadius, -shRadInt);
        path.lineTo(size1 - cornerRadius, -shRadInt);
        path.lineTo(size1 - cornerRadius, cornerRadius);
        path.close();
    }
    private void buildEdgeShader() {
        int shCol = state.shadow.color;
        float shRad = state.shadow.radius/2f;
        edgeShader = new LinearGradient(0f, -shRad, 0f, shRad, 0xFFFFFF & shCol, shCol, Shader.TileMode.CLAMP);
    }
}
