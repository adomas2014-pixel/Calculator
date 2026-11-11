package com.example.calculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Turns numbers into display text the way the Windows Standard calculator does:
 * up to 16 significant digits, thousands grouping ("1,234,567"),
 * and scientific notation ("1.e+16") for very large or very small values.
 */
public final class NumberFormatter {

    private static final int DISPLAY_DIGITS = 16;
    private static final int MAX_FRACTION_DIGITS = 16;
    private static final int SMALLEST_PLAIN_EXPONENT = -10;
    private static final MathContext DISPLAY = new MathContext(DISPLAY_DIGITS, RoundingMode.HALF_EVEN);

    private final char decimalSeparator;
    private final char groupingSeparator;

    /** English defaults: "1,234.5". */
    public NumberFormatter() {
        this('.', ',');
    }

    /** Custom separators, e.g. ',' and a no-break space for Lithuanian: "1 234,5". */
    public NumberFormatter(char decimalSeparator, char groupingSeparator) {
        if (decimalSeparator == groupingSeparator
                || Character.isDigit(decimalSeparator) || Character.isDigit(groupingSeparator)) {
            throw new IllegalArgumentException("Invalid separators: '"
                    + decimalSeparator + "' and '" + groupingSeparator + "'");
        }
        this.decimalSeparator = decimalSeparator;
        this.groupingSeparator = groupingSeparator;
    }

    /** Formats a calculated value. */
    public String format(BigDecimal value) {
        if (value.signum() == 0) {
            return "0";
        }
        BigDecimal rounded = value.round(DISPLAY).stripTrailingZeros();
        int exponent = rounded.precision() - rounded.scale() - 1;

        if (exponent >= DISPLAY_DIGITS || exponent < SMALLEST_PLAIN_EXPONENT) {
            return scientific(rounded, exponent);
        }
        BigDecimal plain = exponent >= 0
                ? rounded
                : value.setScale(MAX_FRACTION_DIGITS, RoundingMode.HALF_EVEN).stripTrailingZeros();
        if (plain.signum() == 0) {
            return "0";
        }
        return groupPlain(plain.toPlainString());
    }

    /** Formats the number the user is typing, keeping a trailing "." and trailing zeros. */
    public String formatEntry(boolean negative, String digits) {
        int dot = digits.indexOf('.');
        String integerPart = dot < 0 ? digits : digits.substring(0, dot);
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        out.append(groupInteger(integerPart));
        if (dot >= 0) {
            out.append(decimalSeparator).append(digits.substring(dot + 1));
        }
        return out.toString();
    }

    private String scientific(BigDecimal rounded, int exponent) {
        String mantissa = rounded.movePointLeft(exponent).stripTrailingZeros().toPlainString();
        if (mantissa.indexOf('.') < 0) {
            mantissa = mantissa + ".";
        }
        return mantissa.replace('.', decimalSeparator)
                + "e" + (exponent >= 0 ? "+" : "-") + Math.abs(exponent);
    }

    private String groupPlain(String plain) {
        boolean negative = plain.startsWith("-");
        String unsigned = negative ? plain.substring(1) : plain;
        int dot = unsigned.indexOf('.');
        String integerPart = dot < 0 ? unsigned : unsigned.substring(0, dot);
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        out.append(groupInteger(integerPart));
        if (dot >= 0) {
            out.append(decimalSeparator).append(unsigned.substring(dot + 1));
        }
        return out.toString();
    }

    private String groupInteger(String integerDigits) {
        StringBuilder out = new StringBuilder();
        int length = integerDigits.length();
        for (int i = 0; i < length; i++) {
            if (i > 0 && (length - i) % 3 == 0) {
                out.append(groupingSeparator);
            }
            out.append(integerDigits.charAt(i));
        }
        return out.toString();
    }
}
