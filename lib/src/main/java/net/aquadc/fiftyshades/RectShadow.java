package net.aquadc.fiftyshades;

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
import static net.aquadc.fiftyshades.Numbers.ceil;

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

    public RectShadow cornerRadius(int cornerRadius) { return (RectShadow) i(0, cornerRadius); } // TODO fix artifacts for radius==0
    public RectShadow shadowColor(@ColorInt int color) { return (RectShadow) i(1, color); }
    public RectShadow shadowDx(@Px float dx) { return (RectShadow) f(1, dx); }
    public RectShadow shadowDy(@Px float dy) { return (RectShadow) f(2, dy); }
    public RectShadow shadowRadius(@Px float radius) { return (RectShadow) f(3, radius); }
    @Override public RectShadow shadow(@NonNull ShadowSpec shadow) { return (RectShadow) super.shadow(shadow); }

    // invalidation

    @Override public void setBounds(@NonNull Rect bounds) {
        int corners = boundedCornerRadius();
        float sd = shadowDistance();
        super.setBounds(bounds);

        // quite rare cases when we're extremely small
        boolean sdInvalid = sd != shadowDistance();
        if (sdInvalid) edgeShader = null;
        if (sdInvalid || corners != boundedCornerRadius()) cornerShader = null;
    }

    @Override void radiusInvalidated() { cornerShader = null; }
    @Override void shadowOffsetInvalidated() { shadowInvalidated(); }
    @Override void shadowRadiusInvalidated() { shadowInvalidated(); }
    @Override void shadowColorInvalidated() { shadowInvalidated(); }
    private void shadowInvalidated() {
        cornerShader = null;
        edgeShader = null;
    }

    // drawing

    private final int[] radialColors = new int[5];
    private final float[] radialPositions = { 0f, Float.NaN, Float.NaN, Float.NaN, 1f };
    private RadialGradient cornerShader;
    private final int[] linearColors = new int[3];
    private final float[] linearPositions = { 0f, Float.NaN, 1f };
    private LinearGradient edgeShader;
    @Override public void draw(@NonNull Canvas canvas) {
        if (Color.alpha(state.shadow.color) == 0) return;

        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();

        canvas.save();
        canvas.translate(
            (width < 0 ? bounds.centerX() : bounds.left) + state.shadow.dx,
            (height < 0 ? bounds.centerY() : bounds.top) + state.shadow.dy
        ); //       ^^^ guard against drawing ugly shadow when we're squeezed. This looks quite possible in InsetDrawable
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        float shadowDistance = shadowDistance();
        int cornerRadius = boundedCornerRadius();
        drawCorners(canvas, cornerRadius, width, height, shadowDistance);
        drawEdges(canvas, cornerRadius, width, height, shadowDistance);
        canvas.restore();
    }
    private float shadowDistance() {
        ShadowSpec shadow = state.shadow;
        Rect bounds = getBounds();
        return min(
            (float) Math.sqrt(shadow.dx * shadow.dx + shadow.dy * shadow.dy) + 1, // <-- extra pixel for nicer look
            //      ^^^^^^^^^ Math.hypot is way slower but we don't need extreme accuracy here
            min(bounds.width(), bounds.height()) / 2f // don't overlap self
        );
    }

    private void drawCorners(Canvas canvas, int cornerRadius, int width, int height, float shadowDistance) {
        float shRad = state.shadow.radius;
        float gRad = cornerRadius + shRad;
        if (cornerShader == null) buildCornerShader(cornerRadius, shadowDistance, gRad);
        paint.setShader(cornerShader);
        int shadowRadInt = ceil(shRad);
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
    private void buildCornerShader(int cornerRadius, float shadowDistance, float gRad) {
        int shCol = state.shadow.color;
        radialColors[0] = radialColors[1] = radialColors[4] = 0xFFFFFF & shCol;
        radialColors[2] = radialColors[3] = shCol;
        radialPositions[1] = (cornerRadius - shadowDistance - .66f) / gRad;
        radialPositions[2] = (cornerRadius - shadowDistance + .34f) / gRad; // this gives us nice inner edge even without anti-alias
        radialPositions[3] = cornerRadius / gRad;
        cornerShader = new RadialGradient(cornerRadius, cornerRadius, gRad, radialColors, radialPositions, Shader.TileMode.CLAMP);
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

    private void drawEdges(Canvas canvas, int cornerRadius, int width, int height, float shadowDistance) {
        if (edgeShader == null) buildEdgeShader(shadowDistance);
        paint.setShader(edgeShader);

        float shRad = state.shadow.radius;
        canvas.drawRect(cornerRadius, -shRad, width - cornerRadius, shadowDistance, paint);
        float angle = width > height ? -90f : 90f;
        float halfMinSize = min(width, height) / 2f;
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawRect(cornerRadius, -shRad, height - cornerRadius, shadowDistance, paint);
        float halfMaxSize = max(width, height) / 2f;
        canvas.rotate(angle, halfMaxSize, halfMaxSize);
        canvas.drawRect(cornerRadius, -shRad, width - cornerRadius, shadowDistance, paint);
        canvas.rotate(angle, halfMinSize, halfMinSize);
        canvas.drawRect(cornerRadius, -shRad, height - cornerRadius, shadowDistance, paint);
    }
    private void buildEdgeShader(float shadowDistance) {
        int shCol = state.shadow.color;
        linearColors[0] = 0xFFFFFF & shCol;
        linearColors[1] = linearColors[2] = shCol;
        float shRad = state.shadow.radius;
        float gSize = shRad + shadowDistance;
        linearPositions[1] = shRad / gSize;
        edgeShader = new LinearGradient(0f, -shRad, 0f, shadowDistance, linearColors, linearPositions, Shader.TileMode.CLAMP);
    }
}
