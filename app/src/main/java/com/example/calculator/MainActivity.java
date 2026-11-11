package com.example.calculator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.calculator.engine.CalculatorEngine;
import com.example.calculator.engine.ErrorType;
import com.example.calculator.engine.ExpressionLabels;
import com.example.calculator.engine.HistoryEntry;
import com.example.calculator.engine.Operator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Windows 11 style Standard calculator screen. All logic lives in {@link CalculatorEngine}. */
public class MainActivity extends AppCompatActivity {

    private CalculatorViewModel viewModel;
    private CalculatorEngine engine;

    private View root;
    private View memoryRow;
    private TextView expressionView;
    private TextView displayView;
    private View memoryClearButton;
    private View memoryRecallButton;
    private View memoryListButton;
    private final List<View> disabledOnError = new ArrayList<>();

    private View overlay;
    private View panel;
    private ScrollView panelScroll;
    private LinearLayout panelList;
    private TextView panelEmpty;
    private ImageButton panelClear;
    private OnBackPressedCallback closePanelCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);
        engine = viewModel.getEngine();
        engine.setLabels(createExpressionLabels());
        try {
            engine.setSeparators(
                    singleChar(R.string.btn_decimal, '.'),
                    singleChar(R.string.number_grouping_separator, ','));
        } catch (IllegalArgumentException e) {
            engine.setSeparators('.', ','); // bad translation: keep the English format
        }

        bindViews();
        applyWindowInsets();
        setUpKeypad();
        setUpMemoryRow();
        setUpPanel();

        render();
        if (viewModel.getOpenPanel() != CalculatorViewModel.PANEL_NONE) {
            showPanel(viewModel.getOpenPanel());
        }
    }

    // ------------------------------------------------------------------ setup

    private void bindViews() {
        root = findViewById(R.id.main);
        memoryRow = findViewById(R.id.memory_row);
        expressionView = findViewById(R.id.expression);
        displayView = findViewById(R.id.display);
        overlay = findViewById(R.id.overlay);
        panel = findViewById(R.id.panel);
        panelScroll = findViewById(R.id.panel_scroll);
        panelList = findViewById(R.id.panel_list);
        panelEmpty = findViewById(R.id.panel_empty);
        panelClear = findViewById(R.id.panel_clear);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        // Light background, so the status and navigation bar icons must be dark.
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private ExpressionLabels createExpressionLabels() {
        return new ExpressionLabels(
                getString(R.string.btn_add),
                getString(R.string.btn_subtract),
                getString(R.string.btn_multiply),
                getString(R.string.btn_divide),
                getString(R.string.btn_equals),
                getString(R.string.expr_square),
                getString(R.string.expr_square_root),
                getString(R.string.expr_reciprocal),
                getString(R.string.expr_negate));
    }

    /**
     * Reads a one-character string resource (a number separator). The decimal separator
     * is the label of the "." key itself, so the key and the display always match.
     */
    private char singleChar(int resId, char fallback) {
        String value = getString(resId);
        return value.length() == 1 ? value.charAt(0) : fallback;
    }

    private void setUpKeypad() {
        int[] digitIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        for (int digit = 0; digit < digitIds.length; digit++) {
            final int value = digit;
            onClick(digitIds[digit], () -> engine.inputDigit(value));
        }
        onClick(R.id.btn_decimal, engine::inputDecimalPoint);

        onClick(R.id.btn_percent, engine::percent);
        onClick(R.id.btn_clear_entry, engine::clearEntry);
        onClick(R.id.btn_clear, engine::clearAll);
        onClick(R.id.btn_backspace, engine::backspace);

        onClick(R.id.btn_reciprocal, engine::reciprocal);
        onClick(R.id.btn_square, engine::square);
        onClick(R.id.btn_square_root, engine::squareRoot);

        onClick(R.id.btn_divide, () -> engine.operator(Operator.DIVIDE));
        onClick(R.id.btn_multiply, () -> engine.operator(Operator.MULTIPLY));
        onClick(R.id.btn_subtract, () -> engine.operator(Operator.SUBTRACT));
        onClick(R.id.btn_add, () -> engine.operator(Operator.ADD));

        onClick(R.id.btn_negate, engine::toggleSign);
        onClick(R.id.btn_equals, engine::calculateResult);

        // Windows greys these out while an error message is shown.
        int[] errorDisabledIds = {
                R.id.btn_percent, R.id.btn_reciprocal, R.id.btn_square, R.id.btn_square_root,
                R.id.btn_divide, R.id.btn_multiply, R.id.btn_subtract, R.id.btn_add, R.id.btn_negate
        };
        for (int id : errorDisabledIds) {
            disabledOnError.add(findViewById(id));
        }

        findViewById(R.id.btn_history).setOnClickListener(
                v -> togglePanel(CalculatorViewModel.PANEL_HISTORY));
    }

    private void setUpMemoryRow() {
        memoryClearButton = findViewById(R.id.btn_mc);
        memoryRecallButton = findViewById(R.id.btn_mr);
        memoryListButton = findViewById(R.id.btn_m_list);

        onClick(R.id.btn_mc, engine::memoryClear);
        onClick(R.id.btn_mr, engine::memoryRecall);
        onClick(R.id.btn_m_plus, engine::memoryAdd);
        onClick(R.id.btn_m_minus, engine::memorySubtract);
        onClick(R.id.btn_ms, engine::memoryStore);
        memoryListButton.setOnClickListener(v -> togglePanel(CalculatorViewModel.PANEL_MEMORY));

        disabledOnError.add(findViewById(R.id.btn_m_plus));
        disabledOnError.add(findViewById(R.id.btn_m_minus));
        disabledOnError.add(findViewById(R.id.btn_ms));
    }

    private void setUpPanel() {
        findViewById(R.id.overlay_scrim).setOnClickListener(v -> hidePanel());
        panelClear.setOnClickListener(v -> {
            if (viewModel.getOpenPanel() == CalculatorViewModel.PANEL_HISTORY) {
                engine.clearHistory();
            } else {
                engine.memoryClear();
            }
            render();
        });
        closePanelCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                hidePanel();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closePanelCallback);
    }

    private void onClick(int viewId, Runnable action) {
        findViewById(viewId).setOnClickListener(v -> {
            action.run();
            render();
        });
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        expressionView.setText(engine.getExpressionText());
        displayView.setText(engine.isError() ? errorMessage(engine.getError()) : engine.getDisplayText());

        boolean enabled = !engine.isError();
        for (View view : disabledOnError) {
            view.setEnabled(enabled);
        }
        boolean hasMemory = engine.hasMemory();
        memoryClearButton.setEnabled(enabled && hasMemory);
        memoryRecallButton.setEnabled(enabled && hasMemory);
        memoryListButton.setEnabled(enabled && hasMemory);

        if (overlay.getVisibility() == View.VISIBLE) {
            refreshPanel();
        }
    }

    private String errorMessage(ErrorType error) {
        switch (error) {
            case DIVIDE_BY_ZERO:
                return getString(R.string.error_divide_by_zero);
            case RESULT_UNDEFINED:
                return getString(R.string.error_result_undefined);
            case INVALID_INPUT:
                return getString(R.string.error_invalid_input);
            case OVERFLOW:
                return getString(R.string.error_overflow);
            default:
                return "";
        }
    }

    // ------------------------------------------------------------------ history and memory panel

    private void togglePanel(int type) {
        if (overlay.getVisibility() == View.VISIBLE && viewModel.getOpenPanel() == type) {
            hidePanel();
        } else {
            showPanel(type);
        }
    }

    private void showPanel(int type) {
        if (root.getHeight() == 0) {
            root.post(() -> showPanel(type)); // not laid out yet
            return;
        }
        viewModel.setOpenPanel(type);
        // Like Windows, the panel covers everything from the memory row down.
        ViewGroup.LayoutParams params = panel.getLayoutParams();
        params.height = root.getHeight() - root.getPaddingBottom() - topWithin(memoryRow, root);
        panel.setLayoutParams(params);
        overlay.setVisibility(View.VISIBLE);
        closePanelCallback.setEnabled(true);
        refreshPanel();
    }

    private void hidePanel() {
        overlay.setVisibility(View.GONE);
        viewModel.setOpenPanel(CalculatorViewModel.PANEL_NONE);
        closePanelCallback.setEnabled(false);
    }

    private void refreshPanel() {
        int type = viewModel.getOpenPanel();
        if (type == CalculatorViewModel.PANEL_NONE) {
            return;
        }
        panelList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        boolean empty;

        if (type == CalculatorViewModel.PANEL_HISTORY) {
            List<HistoryEntry> history = engine.getHistory();
            empty = history.isEmpty();
            for (int i = 0; i < history.size(); i++) {
                final int index = i;
                HistoryEntry entry = history.get(i);
                View row = inflater.inflate(R.layout.item_history, panelList, false);
                ((TextView) row.findViewById(R.id.history_expression)).setText(entry.getExpression());
                ((TextView) row.findViewById(R.id.history_result)).setText(engine.format(entry.getValue()));
                row.setOnClickListener(v -> {
                    engine.recallHistory(index);
                    hidePanel();
                    render();
                });
                panelList.addView(row);
            }
            panelEmpty.setText(R.string.history_empty);
            panelClear.setContentDescription(getString(R.string.cd_clear_history));
        } else {
            List<BigDecimal> memory = engine.getMemory();
            empty = memory.isEmpty();
            boolean enabled = !engine.isError();
            for (int i = 0; i < memory.size(); i++) {
                final int index = i;
                View row = inflater.inflate(R.layout.item_memory, panelList, false);
                ((TextView) row.findViewById(R.id.memory_value)).setText(engine.format(memory.get(i)));
                View clear = row.findViewById(R.id.memory_item_clear);
                View add = row.findViewById(R.id.memory_item_add);
                View subtract = row.findViewById(R.id.memory_item_subtract);
                clear.setOnClickListener(v -> {
                    engine.memoryClearAt(index);
                    render();
                });
                add.setOnClickListener(v -> {
                    engine.memoryAddAt(index);
                    render();
                });
                subtract.setOnClickListener(v -> {
                    engine.memorySubtractAt(index);
                    render();
                });
                add.setEnabled(enabled);
                subtract.setEnabled(enabled);
                row.setOnClickListener(v -> {
                    engine.memoryRecallAt(index);
                    hidePanel();
                    render();
                });
                panelList.addView(row);
            }
            panelEmpty.setText(R.string.memory_empty);
            panelClear.setContentDescription(getString(R.string.cd_clear_memory));
        }

        panelEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        panelScroll.setVisibility(empty ? View.GONE : View.VISIBLE);
        panelClear.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
    }

    private static int topWithin(View view, View ancestor) {
        int top = 0;
        View currentView = view;
        while (currentView != null && currentView != ancestor) {
            top += currentView.getTop();
            ViewParent parent = currentView.getParent();
            currentView = parent instanceof View ? (View) parent : null;
        }
        return top;
    }
}
