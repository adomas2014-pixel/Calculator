package com.example.calculator.engine;

/**
 * Error states, matching the Windows calculator messages.
 * The UI maps each value to a string resource, so the messages stay localizable.
 */
public enum ErrorType {
    NONE,
    /** x ÷ 0 or 1/(0): "Cannot divide by zero". */
    DIVIDE_BY_ZERO,
    /** 0 ÷ 0: "Result is undefined". */
    RESULT_UNDEFINED,
    /** Square root of a negative number: "Invalid input". */
    INVALID_INPUT,
    /** Result too large to represent: "Overflow". */
    OVERFLOW
}
