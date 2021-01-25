package net.aquadc.fiftyshades;


final class Numbers {
    private Numbers() {}

    static int ceil(float f) {
        int i = (int) f;
        if (f > 0 && f > i) i++;
        else if (f < 0 && f < i) i--;
        return i;
    }

    static void requireFinite(float f, String name) {
        if (Float.isInfinite(f) || Float.isNaN(f))
            throw new IllegalArgumentException(name + " must be finite, got " + f);
    }

    static void requireNonNegative(float f, String name) {
        requireFinite(f, name);
        if (f < 0) throw new IllegalArgumentException(name + " must be >= 0, got " + f);
    }

}
