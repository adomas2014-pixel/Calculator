package com.example.calculator.engine;

import java.math.BigDecimal;

/** One finished calculation in the runtime history, e.g. "12 + 8 =" with value 20. */
public final class HistoryEntry {

    private final String expression;
    private final BigDecimal value;

    public HistoryEntry(String expression, BigDecimal value) {
        if (expression == null || value == null) {
            throw new IllegalArgumentException("expression and value are required");
        }
        this.expression = expression;
        this.value = value;
    }

    public String getExpression() {
        return expression;
    }

    public BigDecimal getValue() {
        return value;
    }
}
