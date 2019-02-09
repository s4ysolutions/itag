package solutions.s4y.waytoday.utils;

public class FConv {
    public static final int FLOAT_MULT = 10000000;

    public static int i(double f) {
        return ((int) Math.round(f * FLOAT_MULT));
    }

    public static float f(int i) {
        return (float) i / FLOAT_MULT;
    }
}
