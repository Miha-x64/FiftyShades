package net.aquadc.fiftyshades;


final class Numbers {
    private Numbers() {}

    static int ceil(float f) {
        int i = (int) f;
        if (f > 0 && f > i) i++;
        else if (f < 0 && f < i) i--;
        return i;
    }

    static float requireFinite(float f, String name) {
        if (Float.isInfinite(f) || Float.isNaN(f))
            throw new IllegalArgumentException(name + " must be finite, got " + f);
        return f;
    }

    static float requireNonNegative(float f, String name) {
        requireFinite(f, name);
        if (f < 0) throw new IllegalArgumentException(name + " must be >= 0, got " + f);
        return f;
    }
    static int requireNonNegative(int i, String name) {
        if (i < 0) throw new IllegalArgumentException(name + " must be >= 0, got " + i);
        return i;
    }

    static void putLe(byte[] to, int at, int what) {
        to[at] = (byte) (what & 0xFF);
        to[++at] = (byte) ((what >>> 8) & 0xFF);
        to[++at] = (byte) ((what >>> 16) & 0xFF);
        to[++at] = (byte) ((what >>> 24) & 0xFF);
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    static StringBuilder appendColor(StringBuilder to, int color) {
        to.append('#');
        int alpha = color >>> 24;
        if (alpha != 0xFF) {
            to.append(HEX[alpha >>> 4]).append(HEX[alpha & 15]);
        }
        return to.append(HEX[(color >>> 20) & 15]).append(HEX[(color >>> 16) & 15])
            .append(HEX[(color >>> 12) & 15]).append(HEX[(color >>> 8) & 15])
            .append(HEX[(color >>> 4) & 15]).append(HEX[color & 15]);
    }

}
