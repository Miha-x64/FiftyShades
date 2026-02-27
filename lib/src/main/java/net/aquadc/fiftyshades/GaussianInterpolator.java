package net.aquadc.fiftyshades;

import android.view.animation.Interpolator;

class GaussianInterpolator implements Interpolator {
    static final GaussianInterpolator GAUSSIAN = new GaussianInterpolator();

    private GaussianInterpolator() {}

    static final float GAUSSIAN_FADE_AWAY = 1.75f;

    /** Interpolates x∈[0,1] with Gaussian, speculating that exp(-GAUSSIAN_FADE_AWAY²)≈0 */
    @Override public float getInterpolation(float v) {
        float a = GAUSSIAN_FADE_AWAY * v;
        return (float) Math.exp(-a*a);
    }
}
