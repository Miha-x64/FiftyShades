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
import static java.lang.Math.round;
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

    public RectInnerShadow cornerRadius(int cornerRadius) { return (RectInnerShadow) super.cornerRadius(cornerRadius); }
    public RectInnerShadow shadowColor(@ColorInt int color) { return (RectInnerShadow) super.shadowColor(color); }
    public RectInnerShadow shadowDx(@Px float dx) { return (RectInnerShadow) super.shadowDx(dx); }
    public RectInnerShadow shadowDy(@Px float dy) { return (RectInnerShadow) super.shadowDy(dy); }
    public RectInnerShadow shadowRadius(@Px float radius) { return (RectInnerShadow) super.shadowRadius(radius); }
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
        canvas.translate(round(bounds.left + shadow.dx), round(bounds.top + shadow.dy));

        int cornerRadius = boundedCornerRadius();
        draw(canvas, cornerRadius, width, height);
        canvas.restore();
    }

    private final RectF arcBounds = new RectF(Integer.MIN_VALUE, 0, 0, 0);
    private void draw(Canvas canvas, int cornerRadius, int width, int height) {
        ShadowSpec shadow = state.shadow;
        int r = max(cornerRadius, ceil(shadow.radius));
        int d = 2 * r;

        if (cornerShader == null) buildCornerShader(cornerRadius);
        if (arcBounds.left == Integer.MIN_VALUE) buildCornerPaths(cornerRadius + cornerRadius, d, shadow);
        if (edgeShaders[0] == null) buildEdgeShaders(d);

        paint.setShader(edgeShaders[0]);
        int dxInt = round(shadow.dx);
        int dyInt = round(shadow.dy);
        int cx = -dxInt + r, cy = -dyInt + r;
        int cyp = cy + max(0, dyInt), cyn = cy + min(0, dyInt);
        canvas.drawRect(-dxInt, cyp, shadow.radius, height - d + cyn, paint);

        int cxd = cx + dxInt, cyd = cy + dyInt;
        boolean negDx = dxInt < 0f, negDy = dyInt < 0f;
        drawCorner(canvas,
            0, -dxInt, -dyInt, cxd, cyd,
            negDy, -dxInt, cyd, Integer.MAX_VALUE, Integer.MAX_VALUE, 0,
            negDx, cxd, -dyInt, Integer.MAX_VALUE, Integer.MAX_VALUE, 1
        );

        paint.setShader(edgeShaders[1]);
        int cxp = cx + max(0, dxInt), cxn = cx + min(0, dxInt);
        canvas.drawRect(cxp, -dyInt, width - d + cxn, shadow.radius, paint);

        canvas.translate(width - d, 0f);
        boolean posDx = dxInt > 0f;
        drawCorner(canvas,
            1, cxd, -dyInt, d - dxInt, cyd,
            posDx, Integer.MIN_VALUE, -dyInt, cxd, Integer.MAX_VALUE, 1,
            negDy, Integer.MIN_VALUE, cyd, d - dxInt, Integer.MAX_VALUE, 2
        );

        paint.setShader(edgeShaders[2]);
        canvas.drawRect(d - cxp - shadow.radius, cyp, d - dxInt, height - d + cyn, paint);

        canvas.translate(0f, height - d);
        boolean posDy = dyInt > 0f;
        drawCorner(canvas,
            2, cxd, cyd, d - dxInt, d - dyInt,
            posDy, Integer.MIN_VALUE, Integer.MIN_VALUE, d - dxInt, cyd, 2,
            posDx, Integer.MIN_VALUE, Integer.MIN_VALUE, cxd, d - dyInt, 3
        );

        canvas.translate(-width + d, 0f);
        paint.setShader(edgeShaders[3]);
        canvas.drawRect(cxp, d - cyp - shadow.radius, width - d + cxn, d - dyInt, paint);
        drawCorner(canvas,
            3, -dxInt, cyd, cxd, d - dyInt,
            negDx, cxd, Integer.MIN_VALUE, Integer.MAX_VALUE, d - dyInt, 3,
            posDy, -dxInt, Integer.MIN_VALUE, Integer.MAX_VALUE, cyd, 0
        );
    }
    private void drawCorner(
        Canvas canvas, int which, int cl, int ct, int cr, int cb,
        boolean before, int bl, int bt, int br, int bb, int bs,
        boolean after, int al, int at, int ar, int ab, int as
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
        int dxInt = round(shadow.dx);
        int dyInt = round(shadow.dy);
        arcBounds.set(-dxInt, -dyInt, -dxInt + cornerD, -dyInt + cornerD); // we're already at shadow(dx,dy), shape is at (-dx,-dy)

        int cx = -dxInt + d/2;
        int cxp = cx + max(0, dxInt);
        int cy = -dyInt + d/2;
        int cyp = cy + max(0, dyInt);
        buildCornerPath(0, -dxInt, cyp, 180f, cxp, -dyInt, cxp, cyp);
        int cxn = cx + min(0, dxInt);
        arcBounds.offset(d - arcBounds.width(), 0f);
        buildCornerPath(1, cxn, -dyInt, -90f, -dxInt + cornerD, cyp, cxn, cyp);
        int cyn = cy + min(0, dyInt);
        arcBounds.offset(0f, d - arcBounds.height());
        buildCornerPath(2, -dxInt + cornerD, cyn, 0f, cxn, -dyInt + cornerD, cxn, cyn);
        arcBounds.offset(-d + arcBounds.width(), 0f);
        buildCornerPath(3, cxp, -dyInt + cornerD, 90f, -dxInt, cyn, cxp, cyn);
        arcBounds.offset(0f, -d + arcBounds.height());
    }
    // TODO sometimes we don't need a Path, a quarter of circle is enough
    private void buildCornerPath(int which, int sx, int sy, float sta, int nx, int ny, int cx, int cy) {
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
