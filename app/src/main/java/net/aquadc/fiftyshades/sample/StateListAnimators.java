package net.aquadc.fiftyshades.sample;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.graphics.Color;
import android.util.Property;
import android.view.View;
import androidx.annotation.RequiresApi;

import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_RECT_FILL_COLOR;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_RECT_STROKE_WIDTH;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_SHADOW_COLOR;
import static net.aquadc.fiftyshades.RectItemsWithShadows.DECOR_SHADOW_RADIUS;

class StateListAnimators {

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

}
