package net.aquadc.fiftyshades.sample;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_RECT_FILL_COLOR;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_RECT_STROKE_WIDTH;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_SHADOW_COLOR;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_SHADOW_RADIUS;

class Animators {

    @RequiresApi(21) static StateListAnimator shadowAnimator(float dp) {
        StateListAnimator sla = new StateListAnimator();
        sla.addState(
            new int[] { android.R.attr.state_selected },
            ObjectAnimator.ofPropertyValuesHolder(
                (Object) null,
                PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, 24 * dp),
                PVH_ofColor(DECOR_SHADOW_COLOR, 0xFF_AAFFCC),
                PVH_ofColor(DECOR_RECT_FILL_COLOR, 0xFF_AAFFCC),
                PropertyValuesHolder.ofFloat(DECOR_RECT_STROKE_WIDTH, dp)
            )
        );
        sla.addState(
            new int[0],
            ObjectAnimator.ofPropertyValuesHolder(
                (Object) null,
                PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, 6 * dp),
                PVH_ofColor(DECOR_SHADOW_COLOR, 0x66_000000),
                PVH_ofColor(DECOR_RECT_FILL_COLOR, Color.WHITE),
                PropertyValuesHolder.ofFloat(DECOR_RECT_STROKE_WIDTH, 0f)
            )
        );
        return sla;
    }

    private static final ArgbEvaluator argb = new ArgbEvaluator();
    private static PropertyValuesHolder PVH_ofColor(Property<View, Integer> prop, int... colors) {
        PropertyValuesHolder holder = PropertyValuesHolder.ofInt(prop, colors);
        holder.setEvaluator(argb);
        return holder;
    }

    static AnimatorSet playTogether(Animator... items) {
        AnimatorSet as = new AnimatorSet();
        as.playTogether(items);
        as.start();
        return as;
    }

    static final Property<GradientDrawable, Float> SHAPE_CORNER_RADIUS =
            new Property<GradientDrawable, Float>(Float.class, "cornerRadius") {
                @Override public Float get(GradientDrawable object) {
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? object.getCornerRadius() : 0f;
                }
                @Override public void set(GradientDrawable object, Float value) {
                    object.setCornerRadius(value);
                    object.setShape(GradientDrawable.RECTANGLE); // hack to invalidate radius
                }
            };

    static final Property<View, Integer> LAYOUT_WIDTH =
            new Property<View, Integer>(Integer.class, "layout_width") {
                @Override public Integer get(View object) {
                    return object.getLayoutParams().width;
                }
                @Override public void set(View object, Integer value) {
                    ViewGroup.LayoutParams lp = object.getLayoutParams();
                    lp.width = value;
                    object.setLayoutParams(lp);
                }
            };

    static final Property<View, Integer> LAYOUT_HEIGHT =
            new Property<View, Integer>(Integer.class, "layout_height") {
                @Override public Integer get(View object) {
                    return object.getLayoutParams().height;
                }
                @Override public void set(View object, Integer value) {
                    ViewGroup.LayoutParams lp = object.getLayoutParams();
                    lp.height = value;
                    object.setLayoutParams(lp);
                }
            };

}
