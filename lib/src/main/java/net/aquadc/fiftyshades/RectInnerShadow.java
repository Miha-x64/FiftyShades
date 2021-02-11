package net.aquadc.fiftyshades;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.aquadc.fiftyshades.Numbers.ceil;

/**
 * A shadow dropped inside a rectangle with rounded corners.
 */
public final class RectInnerShadow extends Shadow {

    RectInnerShadow(ShadowState state) {
        super(state, Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
    }
    public RectInnerShadow() {
        this(0, 0f, 0f, 0f, Color.TRANSPARENT);
    }
    public RectInnerShadow(@Px int cornerRadius, @NonNull ShadowSpec shadow) {
        this(cornerRadius, shadow.dx, shadow.dy, shadow.radius, shadow.color);
    }
    public RectInnerShadow(@Px int cornerRadius, @Px float dx, @Px float dy, @Px float radius, @ColorInt int color) {
        super(new ShadowState(cornerRadius, dx, dy, radius, color, true), Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
    }

    // setters

    public RectInnerShadow cornerRadius(int cornerRadius) { return (RectInnerShadow) i(0, cornerRadius); }
    public RectInnerShadow shadowColor(@ColorInt int color) { return (RectInnerShadow) i(1, color); }
    public RectInnerShadow shadowDx(@Px float dx) { return (RectInnerShadow) f(1, dx); }
    public RectInnerShadow shadowDy(@Px float dy) { return (RectInnerShadow) f(2, dy); }
    public RectInnerShadow shadowRadius(@Px float radius) { return (RectInnerShadow) f(3, radius); }
    @Override public RectInnerShadow shadow(@NonNull ShadowSpec shadow) { return (RectInnerShadow) super.shadow(shadow); }

    // invalidation

    @Override public void setBounds(@NonNull Rect bounds) {
        int corners = boundedCornerRadius();
        super.setBounds(bounds);

        // quite rare cases when we're extremely small
        if (corners != boundedCornerRadius()) {
            radiusInvalidated();
        }
    }

    @Override void radiusInvalidated() {
        cornerShader = null;
        arcBounds.left = Integer.MIN_VALUE;
        edgeShaders[0] = null; // offsets of right and bottom gradients will change
    }
    @Override void shadowOffsetInvalidated() {
        arcBounds.left = Integer.MIN_VALUE;
    }
    @Override void shadowRadiusInvalidated() {
        cornerShader = null;
        arcBounds.left = Integer.MIN_VALUE;
        edgeShaders[0] = null;
    }
    @Override void shadowColorInvalidated() {
        cornerShader = null;
        edgeShaders[0] = null;
    }

    // drawing

    private final int[] radialColors = new int[3];
    private final float[] radialPositions = { 0f, Float.NaN, 1f };
    private RadialGradient cornerShader;
    private final Path[] cornerPaths = { new Path(), new Path(), new Path(), new Path() };
    private final LinearGradient[] edgeShaders = new LinearGradient[4];
    @Override public void draw(@NonNull Canvas canvas) {
        Rect bounds;
        int width, height;
        ShadowSpec shadow = state.shadow;
        if (Color.alpha(shadow.color) == 0 ||
            (width = (bounds = getBounds()).width()) <= 0 ||
            (height = bounds.height()) <= 0) return; // TODO fix self-overlap for extra small dimensions

        canvas.save();
        canvas.translate(bounds.left + shadow.dx, bounds.top + shadow.dy);

        int cornerRadius = boundedCornerRadius();
        draw(canvas, cornerRadius, width, height);
        canvas.restore();
    }

    private final RectF arcBounds = new RectF(Integer.MIN_VALUE, 0, 0, 0);
    private void draw(Canvas canvas, int cornerRadius, int width, int height) {
        ShadowSpec shadow = state.shadow;
        int d = 2 * max(cornerRadius, ceil(shadow.radius));

        if (cornerShader == null) buildCornerShader(cornerRadius);
        if (arcBounds.left == Integer.MIN_VALUE) buildCornerPaths(cornerRadius + cornerRadius, d, shadow);
        if (edgeShaders[0] == null) buildEdgeShaders(d);

        paint.setShader(edgeShaders[0]);
        float cx = -shadow.dx + d/2f, cy = -shadow.dy + d/2f;
        float cyp = cy + max(0f, shadow.dy), cyn = cy + min(0f, shadow.dy);
        canvas.drawRect(-shadow.dx, cyp, shadow.radius, height - d + cyn, paint);

        float cxd = cx + shadow.dx, cyd = cy + shadow.dy;
        boolean negDx = shadow.dx < 0f, negDy = shadow.dy < 0f;
        drawCorner(canvas,
            0, -shadow.dx, -shadow.dy, cxd, cyd,
            negDy, -shadow.dx, cyd, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0,
            negDx, cxd, -shadow.dy, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1
        );

        paint.setShader(edgeShaders[1]);
        float cxp = cx + max(0f, shadow.dx), cxn = cx + min(0f, shadow.dx);
        canvas.drawRect(cxp, -shadow.dy, width - d + cxn, shadow.radius, paint);

        canvas.translate(width - d, 0f);
        boolean posDx = shadow.dx > 0f;
        drawCorner(canvas,
            1, cxd, -shadow.dy, d - shadow.dx, cyd,
            posDx, Float.NEGATIVE_INFINITY, -shadow.dy, cxd, Float.POSITIVE_INFINITY, 1,
            negDy, Float.NEGATIVE_INFINITY, cyd, d - shadow.dx, Float.POSITIVE_INFINITY, 2
        );

        paint.setShader(edgeShaders[2]);
        canvas.drawRect(d - cxp - shadow.radius, cyp, d - shadow.dx, height - d + cyn, paint);

        canvas.translate(0f, height - d);
        boolean posDy = shadow.dy > 0f;
        drawCorner(canvas,
            2, cxd, cyd, d - shadow.dx, d - shadow.dy,
            posDy, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, d - shadow.dx, cyd, 2,
            posDx, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, cxd, d - shadow.dy, 3
        );

        canvas.translate(-width + d, 0f);
        paint.setShader(edgeShaders[3]);
        canvas.drawRect(cxp, d - cyp - shadow.radius, width - d + cxn, d - shadow.dy, paint);
        drawCorner(canvas,
            3, -shadow.dx, cyd, cxd, d - shadow.dy,
            negDx, cxd, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, d - shadow.dy, 3,
            posDy, -shadow.dx, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, cyd, 0
        );
    }
    private void drawCorner(
        Canvas canvas, int which, float cl, float ct, float cr, float cb,
        boolean before, float bl, float bt, float br, float bb, int bs,
        boolean after, float al, float at, float ar, float ab, int as
    ) {
        Path corner = cornerPaths[which];
        if (before || after) {
            if (before) {
                canvas.save();
                canvas.clipRect(bl, bt, br, bb);
                paint.setShader(edgeShaders[bs]);
                canvas.drawPath(corner, paint);
                canvas.restore();
            }
            canvas.save();
            canvas.clipRect(cl, ct, cr, cb);
            paint.setShader(cornerShader);
            canvas.drawPath(corner, paint);
            canvas.restore();
            if (after) {
                canvas.save();
                canvas.clipRect(al, at, ar, ab);
                paint.setShader(edgeShaders[as]);
                canvas.drawPath(corner, paint);
                canvas.restore();
            }
        } else {
            paint.setShader(cornerShader);
            canvas.drawPath(corner, paint);
        }
    }
    private void buildCornerShader(int cornerRad) {
        ShadowSpec shadow = state.shadow;
        int shCol = shadow.color;
        float shRad = shadow.radius;
        if (cornerRad > shRad) {
            radialColors[0] = radialColors[1] = 0xFFFFFF & shCol;
            radialColors[2] = shCol;
            float gRad = cornerRad + shRad;
            radialPositions[1] = 1 - (shRad + shRad) / gRad;
            cornerShader = new RadialGradient(cornerRad, cornerRad, gRad, radialColors, radialPositions, Shader.TileMode.CLAMP);
        } else {
            cornerShader = new RadialGradient(shRad, shRad, 2*shRad, 0xFFFFFF & shCol, shCol, Shader.TileMode.CLAMP);
        }
    }
    private void buildCornerPaths(int cornerD, int d, ShadowSpec shadow) {
        arcBounds.set(0, 0, cornerD, cornerD);
        arcBounds.offset(-shadow.dx, -shadow.dy); // we're already at shadow(dx,dy), shape is at (-dx,-dy)

        float cx = arcBounds.left + d/2f;
        float cxp = cx + max(0f, shadow.dx);
        float cy = arcBounds.top + d/2f;
        float cyp = cy + max(0f, shadow.dy);
        buildCornerPath(0, arcBounds.left, cyp, 180f, cxp, arcBounds.top, cxp, cyp);
        float cxn = cx + min(0f, shadow.dx);
        arcBounds.offset(d - arcBounds.width(), 0f);
        buildCornerPath(1, cxn, arcBounds.top, -90f, arcBounds.right, cyp, cxn, cyp);
        float cyn = cy + min(0f, shadow.dy);
        arcBounds.offset(0f, d - arcBounds.height());
        buildCornerPath(2, arcBounds.right, cyn, 0f, cxn, arcBounds.bottom, cxn, cyn);
        arcBounds.offset(-d + arcBounds.width(), 0f);
        buildCornerPath(3, cxp, arcBounds.bottom, 90f, arcBounds.left, cyn, cxp, cyn);
        arcBounds.offset(0f, -d + arcBounds.height());
    }
    // TODO sometimes we don't need a Path, a quarter of circle is enough
    private void buildCornerPath(int which, float sx, float sy, float sta, float nx, float ny, float cx, float cy) {
        Path p = cornerPaths[which];
        p.rewind();
        p.moveTo(sx, sy);
        p.arcTo(arcBounds, sta, 90f, false);
        p.lineTo(nx, ny);
        p.lineTo(cx, cy);
        p.close();
    }
    private void buildEdgeShaders(int d) {
        ShadowSpec shadow = state.shadow;
        float rad = shadow.radius;
        int col = shadow.color, tra = 0xFFFFFF & col;
        edgeShaders[0] = new LinearGradient(-rad, 0f, rad, 0f, col, tra, Shader.TileMode.CLAMP);
        edgeShaders[1] = new LinearGradient(0f, -rad, 0f, rad, col, tra, Shader.TileMode.CLAMP);
        edgeShaders[2] = new LinearGradient(d - rad, 0f, d + rad, 0f, tra, col, Shader.TileMode.CLAMP);
        edgeShaders[3] = new LinearGradient(0f, d - rad, 0f, d + rad, tra, col, Shader.TileMode.CLAMP);
    }

}
