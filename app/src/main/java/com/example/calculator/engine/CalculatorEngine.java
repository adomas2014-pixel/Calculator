package com.example.calculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculation engine of the Standard calculator, written from scratch.
 *
 * <p>Works like the Windows 11 calculator: immediate execution, so operations are
 * applied left to right as operators are pressed (2 + 3 x 4 = 20). It keeps the
 * expression line, a runtime history and a memory list. It has no Android
 * dependencies, so it can be unit-tested on the JVM.</p>
 */
public final class CalculatorEngine {

    /** Internal precision: 34 significant digits (IEEE 754 decimal128). */
    static final MathContext MATH = new MathContext(34, RoundingMode.HALF_EVEN);

    private static final int MAX_INPUT_DIGITS = 16;
    private static final int MAX_EXPONENT = 9999;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private enum Unary { SQUARE, SQUARE_ROOT, RECIPROCAL, NEGATE }

    /** Thrown internally when an operation ends in an error state. */
    private static final class CalculationException extends Exception {
        private static final long serialVersionUID = 1L;
        final ErrorType type;

        CalculationException(ErrorType type) {
            this.type = type;
        }
    }

    private NumberFormatter formatter = new NumberFormatter();
    private ExpressionLabels labels = ExpressionLabels.DEFAULT;

    // The current operand (what the big display shows).
    private BigDecimal current = BigDecimal.ZERO;
    private StringBuilder entryDigits;      // non-null while the user is typing a number
    private boolean entryNegative;
    private String currentText;             // expression text of current, e.g. "sqr(5)", or null
    private boolean currentIsNew;           // a right-hand operand was supplied after the operator

    // The pending binary operation ("12 +").
    private BigDecimal left;
    private String leftText;
    private Operator pending;

    // Pressing "=" again repeats the last operation.
    private Operator lastOperator;
    private BigDecimal lastOperand;
    private boolean evaluated;

    private String expression = "";
    private ErrorType error = ErrorType.NONE;

    private final List<HistoryEntry> history = new ArrayList<>();   // newest first
    private final List<BigDecimal> memory = new ArrayList<>();      // newest first

    public void setLabels(ExpressionLabels labels) {
        if (labels == null) {
            throw new IllegalArgumentException("labels");
        }
        this.labels = labels;
    }

    /**
     * Sets the decimal and grouping separators used for the display, the expression
     * line, history and memory. The Android layer reads them from strings.xml.
     */
    public void setSeparators(char decimalSeparator, char groupingSeparator) {
        this.formatter = new NumberFormatter(decimalSeparator, groupingSeparator);
    }

    // ------------------------------------------------------------------ number entry

    public void inputDigit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("digit: " + digit);
        }
        prepareForInput();
        if (countEntryDigits() >= MAX_INPUT_DIGITS) {
            return;
        }
        if (entryDigits.length() == 1 && entryDigits.charAt(0) == '0') {
            entryDigits.setLength(0);
        }
        entryDigits.append((char) ('0' + digit));
        current = entryValue();
    }

    public void inputDecimalPoint() {
        prepareForInput();
        if (entryDigits.indexOf(".") >= 0 || countEntryDigits() >= MAX_INPUT_DIGITS) {
            return;
        }
        entryDigits.append('.');
    }

    /** Backspace: deletes the last typed character. */
    public void backspace() {
        if (isError()) {
            clearAll();
            return;
        }
        if (entryDigits != null) {
            entryDigits.setLength(entryDigits.length() - 1);
            if (entryDigits.length() == 0) {
                entryDigits.append('0');
            }
            if ("0".contentEquals(entryDigits)) {
                entryNegative = false;
            }
            current = entryValue();
        } else if (evaluated) {
            expression = ""; // Windows clears the expression line after "="
        }
    }

    /** CE: clears the current entry, keeps the pending operation. */
    public void clearEntry() {
        if (isError() || evaluated) {
            clearAll();
            return;
        }
        entryDigits = null;
        entryNegative = false;
        current = BigDecimal.ZERO;
        currentText = null;
        currentIsNew = true;
        updateExpression();
    }

    /** C: resets the calculation. History and memory are kept. */
    public void clearAll() {
        current = BigDecimal.ZERO;
        entryDigits = null;
        entryNegative = false;
        currentText = null;
        currentIsNew = false;
        left = null;
        leftText = null;
        pending = null;
        lastOperator = null;
        lastOperand = null;
        evaluated = false;
        expression = "";
        error = ErrorType.NONE;
    }

    /** +/-: flips the sign of the number being typed, or applies negate(x) to a result. */
    public void toggleSign() {
        if (isError()) {
            return;
        }
        if (entryDigits != null) {
            if (current.signum() == 0) {
                return;
            }
            entryNegative = !entryNegative;
            current = entryValue();
            return;
        }
        applyUnary(Unary.NEGATE);
    }

    // ------------------------------------------------------------------ operations

    public void operator(Operator operator) {
        if (isError()) {
            return;
        }
        commitEntry();

        if (evaluated) {
            evaluated = false;
            left = current;
            leftText = formatter.format(current);
        } else if (pending != null && currentIsNew) {
            String failedExpression = leftText + " " + labels.symbol(pending) + " " + operandText()
                    + " " + labels.symbol(operator);
            try {
                left = compute(left, pending, current);
            } catch (CalculationException e) {
                fail(e.type, failedExpression);
                return;
            }
            current = left;
            leftText = formatter.format(left);
        } else if (pending == null) {
            left = current;
            leftText = operandText();
        }
        // Otherwise the operator was pressed twice in a row: just replace it.

        pending = operator;
        currentIsNew = false;
        currentText = null;
        updateExpression();
    }

    /** "=": finishes the calculation and adds it to the history. */
    public void calculateResult() {
        if (isError()) {
            clearAll();
            return;
        }
        commitEntry();

        BigDecimal a = current;
        Operator op = null;
        BigDecimal b = null;
        String text;
        String equalsSign = labels.equalsSign();

        if (pending != null) {
            a = left;
            op = pending;
            b = currentIsNew ? current : left;               // "5 + =" means 5 + 5
            String rightText = currentIsNew ? operandText() : formatter.format(left);
            text = leftText + " " + labels.symbol(op) + " " + rightText + " " + equalsSign;
        } else if (lastOperator != null) {
            op = lastOperator;                               // repeat: 20 = -> 20 + 8 =
            b = lastOperand;
            text = operandText() + " " + labels.symbol(op) + " " + formatter.format(b)
                    + " " + equalsSign;
        } else {
            text = operandText() + " " + equalsSign;
        }

        BigDecimal result;
        try {
            result = op == null ? a : compute(a, op, b);
        } catch (CalculationException e) {
            fail(e.type, text);
            return;
        }
        if (op != null) {
            lastOperator = op;
            lastOperand = b;
        }
        current = result;
        expression = text;
        history.add(0, new HistoryEntry(text, result));
        left = null;
        leftText = null;
        pending = null;
        currentText = null;
        currentIsNew = false;
        evaluated = true;
    }

    /**
     * %: with + or - pending, x% means x percent of the left operand (200 + 10% = 200 + 20).
     * With x or / pending it means x / 100. Without a pending operation the result is 0.
     */
    public void percent() {
        if (isError()) {
            return;
        }
        commitEntry();
        BigDecimal result;
        if (pending == null) {
            result = BigDecimal.ZERO;
        } else if (pending == Operator.ADD || pending == Operator.SUBTRACT) {
            result = left.multiply(current, MATH).divide(HUNDRED, MATH);
        } else {
            result = current.divide(HUNDRED, MATH);
        }
        try {
            result = checkRange(result);
        } catch (CalculationException e) {
            fail(e.type, expression);
            return;
        }
        evaluated = false;
        current = result;
        currentText = formatter.format(result);
        currentIsNew = true;
        updateExpression();
    }

    public void square() {
        applyUnary(Unary.SQUARE);
    }

    public void squareRoot() {
        applyUnary(Unary.SQUARE_ROOT);
    }

    public void reciprocal() {
        applyUnary(Unary.RECIPROCAL);
    }

    // ------------------------------------------------------------------ memory

    /** MS: saves the current value as a new memory item. */
    public void memoryStore() {
        if (isError()) {
            return;
        }
        commitEntry();
        memory.add(0, current);
    }

    /** M+: adds the current value to the newest memory item (or creates one). */
    public void memoryAdd() {
        if (memory.isEmpty()) {
            memoryStore();
        } else {
            changeMemoryAt(0, false);
        }
    }

    /** M-: subtracts the current value from the newest memory item (or creates one). */
    public void memorySubtract() {
        if (isError()) {
            return;
        }
        if (memory.isEmpty()) {
            commitEntry();
            memory.add(0, current.negate());
        } else {
            changeMemoryAt(0, true);
        }
    }

    /** MR: recalls the newest memory item. */
    public void memoryRecall() {
        if (!memory.isEmpty()) {
            recallValue(memory.get(0));
        }
    }

    /** MC: clears all memory. */
    public void memoryClear() {
        memory.clear();
    }

    public void memoryAddAt(int index) {
        changeMemoryAt(index, false);
    }

    public void memorySubtractAt(int index) {
        changeMemoryAt(index, true);
    }

    public void memoryRecallAt(int index) {
        recallValue(memory.get(index));
    }

    public void memoryClearAt(int index) {
        memory.remove(index);
    }

    // ------------------------------------------------------------------ history

    /** Loads a history entry's result into the display as the current number. */
    public void recallHistory(int index) {
        recallValue(history.get(index).getValue());
    }

    public void clearHistory() {
        history.clear();
    }

    // ------------------------------------------------------------------ state for the UI

    /** Text for the big display. Empty while in an error state (the UI shows the message). */
    public String getDisplayText() {
        if (isError()) {
            return "";
        }
        if (entryDigits != null) {
            return formatter.formatEntry(entryNegative, entryDigits.toString());
        }
        return formatter.format(current);
    }

    /** Text for the small line above the display, e.g. "12 + 8 =". */
    public String getExpressionText() {
        return expression;
    }

    public ErrorType getError() {
        return error;
    }

    public boolean isError() {
        return error != ErrorType.NONE;
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public List<BigDecimal> getMemory() {
        return Collections.unmodifiableList(new ArrayList<>(memory));
    }

    public boolean hasMemory() {
        return !memory.isEmpty();
    }

    /** Formats a value the same way the display does (used for history and memory lists). */
    public String format(BigDecimal value) {
        return formatter.format(value);
    }

    // ------------------------------------------------------------------ internals

    private void prepareForInput() {
        if (isError()) {
            clearAll();
        }
        if (evaluated) {
            startNewCalculation();
        }
        if (entryDigits == null) {
            entryDigits = new StringBuilder("0");
            entryNegative = false;
            current = BigDecimal.ZERO;
            currentText = null;
            currentIsNew = true;
            updateExpression();
        }
    }

    /** After "=", typing starts a new calculation; the repeat-equals operation is kept. */
    private void startNewCalculation() {
        left = null;
        leftText = null;
        pending = null;
        currentText = null;
        evaluated = false;
        expression = "";
    }

    private void commitEntry() {
        if (entryDigits != null) {
            current = entryValue();
            entryDigits = null;
            entryNegative = false;
        }
    }

    private void recallValue(BigDecimal value) {
        if (isError()) {
            clearAll();
        }
        if (evaluated) {
            startNewCalculation();
        }
        entryDigits = null;
        entryNegative = false;
        current = value;
        currentText = null;
        currentIsNew = true;
        updateExpression();
    }

    private void changeMemoryAt(int index, boolean subtract) {
        if (isError()) {
            return;
        }
        commitEntry();
        BigDecimal item = memory.get(index);
        try {
            memory.set(index, checkRange(subtract ? item.subtract(current, MATH) : item.add(current, MATH)));
        } catch (CalculationException e) {
            fail(e.type, expression);
        }
    }

    private void applyUnary(Unary unary) {
        if (isError()) {
            return;
        }
        commitEntry();
        String operand = operandText();
        String text;
        switch (unary) {
            case SQUARE:
                text = labels.square(operand);
                break;
            case SQUARE_ROOT:
                text = labels.squareRoot(operand);
                break;
            case RECIPROCAL:
                text = labels.reciprocal(operand);
                break;
            default:
                text = labels.negate(operand);
                break;
        }

        BigDecimal result;
        try {
            result = evaluateUnary(unary, current);
        } catch (CalculationException e) {
            fail(e.type, pending != null ? leftText + " " + labels.symbol(pending) + " " + text : text);
            return;
        }

        evaluated = false;
        current = result;
        currentText = text;
        currentIsNew = true;
        updateExpression();
        // No history entry here: like Windows, only "=" records a calculation.
        // "9 √ =" therefore gives one entry, "√(9) = 3", instead of two.
    }

    private BigDecimal evaluateUnary(Unary unary, BigDecimal x) throws CalculationException {
        switch (unary) {
            case SQUARE:
                return checkRange(x.multiply(x, MATH));
            case SQUARE_ROOT:
                if (x.signum() < 0) {
                    throw new CalculationException(ErrorType.INVALID_INPUT);
                }
                return checkRange(DecimalMath.sqrt(x, MATH));
            case RECIPROCAL:
                if (x.signum() == 0) {
                    throw new CalculationException(ErrorType.DIVIDE_BY_ZERO);
                }
                return checkRange(BigDecimal.ONE.divide(x, MATH));
            default:
                return x.negate();
        }
    }

    private BigDecimal compute(BigDecimal a, Operator op, BigDecimal b) throws CalculationException {
        BigDecimal result;
        switch (op) {
            case ADD:
                result = a.add(b, MATH);
                break;
            case SUBTRACT:
                result = a.subtract(b, MATH);
                break;
            case MULTIPLY:
                result = a.multiply(b, MATH);
                break;
            case DIVIDE:
                if (b.signum() == 0) {
                    throw new CalculationException(a.signum() == 0
                            ? ErrorType.RESULT_UNDEFINED
                            : ErrorType.DIVIDE_BY_ZERO);
                }
                result = a.divide(b, MATH);
                break;
            default:
                throw new IllegalArgumentException("Unknown operator: " + op);
        }
        return checkRange(result);
    }

    /** Overflow above 10^9999; values below 10^-9999 become 0. */
    private static BigDecimal checkRange(BigDecimal value) throws CalculationException {
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        int exponent = value.precision() - value.scale() - 1;
        if (exponent > MAX_EXPONENT) {
            throw new CalculationException(ErrorType.OVERFLOW);
        }
        if (exponent < -MAX_EXPONENT) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private void fail(ErrorType type, String failedExpression) {
        error = type;
        expression = failedExpression;
        current = BigDecimal.ZERO;
        entryDigits = null;
        entryNegative = false;
        currentText = null;
        currentIsNew = false;
        left = null;
        leftText = null;
        pending = null;
        lastOperator = null;
        lastOperand = null;
        evaluated = false;
    }

    private String operandText() {
        return currentText != null ? currentText : formatter.format(current);
    }

    private void updateExpression() {
        if (pending != null) {
            String text = leftText + " " + labels.symbol(pending);
            if (currentIsNew && currentText != null) {
                text = text + " " + currentText;
            }
            expression = text;
        } else if (currentText != null) {
            expression = currentText;
        } else if (!evaluated) {
            expression = "";
        }
    }

    private int countEntryDigits() {
        int count = 0;
        for (int i = 0; i < entryDigits.length(); i++) {
            if (Character.isDigit(entryDigits.charAt(i))) {
                count++;
            }
        }
        if (entryDigits.length() > 1 && entryDigits.charAt(0) == '0' && entryDigits.charAt(1) == '.') {
            count--; // the leading zero of "0.xxx" does not count
        }
        return count;
    }

    private BigDecimal entryValue() {
        BigDecimal value = new BigDecimal(entryDigits.toString());
        return entryNegative ? value.negate() : value;
    }
}
