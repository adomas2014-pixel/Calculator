package com.example.calculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Math helpers written from scratch for the engine. */
final class DecimalMath {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final int MAX_ITERATIONS = 200;

    private DecimalMath() {
    }

    /**
     * Square root by Newton's method: next = (guess + value / guess) / 2.
     * The starting guess is 3 x 10^(exponent / 2), already within a small factor
     * of the answer, so it converges in a handful of steps.
     */
    static BigDecimal sqrt(BigDecimal value, MathContext precision) {
        if (value.signum() < 0) {
            throw new ArithmeticException("Square root of a negative number");
        }
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        MathContext work = new MathContext(precision.getPrecision() + 6, RoundingMode.HALF_EVEN);

        int exponent = value.precision() - value.scale() - 1; // floor(log10(value))
        int halfExponent = exponent >= 0 ? exponent / 2 : -((-exponent + 1) / 2);
        BigDecimal guess = THREE.scaleByPowerOfTen(halfExponent);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            BigDecimal next = guess.add(value.divide(guess, work), work).divide(TWO, work);
            if (next.compareTo(guess) == 0) {
                break;
            }
            guess = next;
        }
        return guess.round(precision);
    }
}
