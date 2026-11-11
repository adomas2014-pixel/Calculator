package com.example.calculator.engine;

import java.util.Locale;

/**
 * Text the engine uses to build the expression line and history entries.
 * The Android layer fills this from strings.xml, so it can be localized
 * without touching the engine. Format strings take one argument (%1$s),
 * the operand text, e.g. "sqr(%1$s)" -> "sqr(5)".
 */
public final class ExpressionLabels {

    public static final ExpressionLabels DEFAULT = new ExpressionLabels(
            "+", "\u2212", "\u00D7", "\u00F7", "=",
            "sqr(%1$s)", "\u221A(%1$s)", "1/(%1$s)", "negate(%1$s)");

    private final String add;
    private final String subtract;
    private final String multiply;
    private final String divide;
    private final String equalsSign;
    private final String squareFormat;
    private final String squareRootFormat;
    private final String reciprocalFormat;
    private final String negateFormat;

    public ExpressionLabels(String add, String subtract, String multiply, String divide,
                            String equalsSign, String squareFormat, String squareRootFormat,
                            String reciprocalFormat, String negateFormat) {
        this.add = add;
        this.subtract = subtract;
        this.multiply = multiply;
        this.divide = divide;
        this.equalsSign = equalsSign;
        this.squareFormat = squareFormat;
        this.squareRootFormat = squareRootFormat;
        this.reciprocalFormat = reciprocalFormat;
        this.negateFormat = negateFormat;
    }

    String symbol(Operator operator) {
        switch (operator) {
            case ADD:
                return add;
            case SUBTRACT:
                return subtract;
            case MULTIPLY:
                return multiply;
            case DIVIDE:
                return divide;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    String equalsSign() {
        return equalsSign;
    }

    String square(String operand) {
        return String.format(Locale.ROOT, squareFormat, operand);
    }

    String squareRoot(String operand) {
        return String.format(Locale.ROOT, squareRootFormat, operand);
    }

    String reciprocal(String operand) {
        return String.format(Locale.ROOT, reciprocalFormat, operand);
    }

    String negate(String operand) {
        return String.format(Locale.ROOT, negateFormat, operand);
    }
}
