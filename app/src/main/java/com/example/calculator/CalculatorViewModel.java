package com.example.calculator;

import androidx.lifecycle.ViewModel;

import com.example.calculator.engine.CalculatorEngine;

/**
 * Holds the calculator engine (display, pending operation, history, memory)
 * and which panel is open, so nothing is lost when the activity is recreated
 * (for example after a system theme or font-size change).
 */
public class CalculatorViewModel extends ViewModel {

    public static final int PANEL_NONE = 0;
    public static final int PANEL_HISTORY = 1;
    public static final int PANEL_MEMORY = 2;

    private final CalculatorEngine engine = new CalculatorEngine();
    private int openPanel = PANEL_NONE;

    public CalculatorEngine getEngine() {
        return engine;
    }

    public int getOpenPanel() {
        return openPanel;
    }

    public void setOpenPanel(int openPanel) {
        this.openPanel = openPanel;
    }
}
