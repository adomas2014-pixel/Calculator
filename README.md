# Calculator

A Standard calculator for Android, modelled on the Windows 11 Calculator. It supports the
four basic operations, percentages, squares, square roots and reciprocals, and keeps a
history of your calculations and a list of saved memory values. All text on screen comes from
string resources, so the app can be translated, including the number format.

The calculation engine is written from scratch in plain Java with `BigDecimal` arithmetic
(34 significant digits internally), so results like `0.1 + 0.2` come out exactly as `0.3`.

---

## Features

### Operations

| Key | What it does |
|-----|--------------|
| `0`–`9`, `.` | Enter a number (up to 16 digits) |
| `+` `−` `×` `÷` | Basic arithmetic |
| `=` | Shows the result. Press it again to repeat the last operation (`20 + 8 = =` → 36) |
| `%` | Percent. With `+` or `−` it takes a percentage of the first number (`200 + 10%` → `200 + 20`). With `×` or `÷` it divides by 100 |
| `x²` | Square, shown as `sqr(x)` |
| `²√x` | Square root, shown as `√(x)` |
| `¹⁄ₓ` | Reciprocal, shown as `1/(x)` |
| `⁺⁄₋` | Changes the sign of the number you are typing, or applies `negate(x)` to a result |
| `CE` | Clears the current entry and keeps the pending operation |
| `C` | Clears the whole calculation (history and memory are kept) |
| `⌫` | Deletes the last digit you typed |

**Immediate execution:** like the Windows Standard calculator, operations are applied left to
right as you press them, without operator precedence. `2 + 3 × 4 =` gives **20**, not 14.

**Number display:**

- up to 16 significant digits, with thousands grouping (`1,234,567`)
- very large or very small numbers use scientific notation (`1.e+16`)
- a small line above the result shows the expression being built (`12 + 8 =`)

**Errors.** When an error message is shown, the operation keys are greyed out until you press
`C`, `CE`, a digit or `⌫`:

| Message | Cause |
|---------|-------|
| Cannot divide by zero | `x ÷ 0` or `1/(0)` |
| Result is undefined | `0 ÷ 0` |
| Invalid input | Square root of a negative number |
| Overflow | Result larger than 10^9999 |

### History

- Each calculation finished with `=` is added to the history, with the newest at the top.
- Tap the **history icon** (top right) to open the history panel.
- Tap an entry to load its result back into the display.
- Tap the **trash icon** in the panel to clear all history.
- History is kept in memory while the app runs. It survives screen rotation and theme or
  font-size changes, but it is not saved after the app is closed.

### Memory

| Key | What it does |
|-----|--------------|
| `MS` | Saves the current value as a new memory item |
| `M+` | Adds the current value to the newest memory item (or creates one) |
| `M−` | Subtracts the current value from the newest memory item (or creates one) |
| `MR` | Loads the newest memory item into the display |
| `MC` | Clears all memory |
| `M˅` | Opens the memory list |

In the memory list you can tap an item to load it, or use its own `MC`, `M+` and `M−` buttons
to change only that item. `MC`, `MR` and `M˅` are disabled while memory is empty.

### Localization

The app ships in English. Everything the user sees is in `res/values/strings.xml`, and no text
is hard-coded in Java:

- button labels and operator symbols
- the expression line formats (`sqr(%1$s)`, `√(%1$s)`, `1/(%1$s)`, `negate(%1$s)`)
- error messages, empty-panel messages and accessibility descriptions
- **number separators**: the decimal separator is the label of the `.` key
  (`btn_decimal`), and the thousands separator is `number_grouping_separator`

To add a language, create a folder such as `res/values-lt/` with its own `strings.xml` and
translate the strings. For example, Lithuanian number formatting (`1 234,5`) needs only:

```xml
<resources>
    <string name="btn_decimal">,</string>
    <!-- no-break space -->
    <string name="number_grouping_separator"> </string>
    <string name="error_divide_by_zero">Dalyba iš nulio negalima</string>
    <!-- ...other strings... -->
</resources>
```

Each separator must be exactly one character, and the two separators must be different. If a
translation breaks this rule, the app falls back to the English format (`1,234.5`) instead of
crashing.

---

## Requirements

| | |
|---|---|
| Android Studio | A recent version that supports `compileSdk 37` (Android Gradle Plugin with the `compileSdk { version = release(...) }` and `optimization { }` DSL) |
| Android SDK | Platform 37 installed through the SDK Manager |
| JDK | The JDK bundled with Android Studio (the code targets Java 11) |
| Device | Android 7.0 (API 24) or newer, phone or emulator |

Dependencies (from the version catalog `gradle/libs.versions.toml`): AndroidX AppCompat,
Activity, ConstraintLayout and Material Components. JUnit and Espresso are used for tests.

---

## Building and running in Android Studio

1. **Open the project.** In Android Studio choose **File → Open** and select the project root
   folder (the one that contains `settings.gradle.kts`, not the `app` folder).
2. **Sync Gradle.** Android Studio syncs automatically. If it does not, click
   **Sync Project with Gradle Files** in the toolbar. Install the SDK platform or build tools
   if Android Studio asks for them.
3. **Choose a device.**
   - *Emulator:* open **Tools → Device Manager**, create a virtual phone with API 24 or newer
     and start it.
   - *Physical phone:* turn on **Developer options → USB debugging** on the phone and connect
     it by USB (or pair it with Wi-Fi debugging).
4. **Run.** Select the `app` configuration and the device in the toolbar, then click **Run ▶**
   (`Shift+F10`). The app installs and opens as **Calculator**.

### Command line

From the project root:

```bash
# Debug APK -> app/build/outputs/apk/debug/
./gradlew assembleDebug

# Build and install on a connected device or running emulator
./gradlew installDebug

# Unit tests (JVM)
./gradlew test

# Instrumented tests (needs a device or emulator)
./gradlew connectedAndroidTest
```

On Windows use `gradlew.bat` instead of `./gradlew`.

### Release build

`./gradlew assembleRelease` builds a minified release APK with R8 optimization turned on.
Custom keep rules go in `app/src/main/keepRules/rules.keep`. To install a release build, set
up signing in **Build → Generate Signed App Bundle / APK**.

---

## How to use the calculator

The app opens in portrait mode with a light theme.

```
 ☰  Standard  ⧉                      🕓    <- history
                                12 + 8 =   <- expression line
                                     20    <- display
 MC   MR   M+   M−   MS   M˅               <- memory row
 %    CE   C    ⌫
 ¹⁄ₓ  x²   ²√x  ÷
 7    8    9    ×
 4    5    6    −
 1    2    3    +
 ⁺⁄₋  0    .    =
```

The menu (☰) and keep-on-top (⧉) icons are decorative, to match the Windows layout.

**Basic calculation:** `12` `+` `8` `=` → **20**

**Chained calculation:** `5` `×` `4` `−` `6` `=` → **14** (the result of `5 × 4` appears as
soon as you press `−`)

**Repeat the last operation:** `20` `+` `8` `=` `=` `=` → **28**, **36**, **44**

**Use the same number twice:** `5` `+` `=` → **10** (`5 + 5`)

**Percent:** `200` `+` `10` `%` `=` → **220**

**Square root of a sum:** `9` `+` `16` `=` `²√x` → **5**

**Fix a mistake:**

- `⌫` deletes the last digit
- `CE` clears only the number you are typing (`12 + 99` `CE` `8 =` → 20)
- `C` starts again

**Change an operator:** if you press the wrong operator, press the right one straight away.
The new one replaces the old one.

**Reuse a result:**

- Open the history (🕓) and tap a calculation to load its result.
- Or press `MS` to save the value, then `MR` later to load it back.

**Close a panel:** tap the history icon or `M˅` again, tap the darkened area above the panel,
or press the system **Back** button.

---

## Project structure

```
app/
├── build.gradle.kts                  Module build config (SDK levels, dependencies)
└── src/
    ├── main/
    │   ├── AndroidManifest.xml       Single launcher activity, portrait only
    │   ├── java/com/example/calculator/
    │   │   ├── MainActivity.java         UI: binds keys to the engine, renders the
    │   │   │                             display, and runs the history/memory panel
    │   │   ├── CalculatorViewModel.java  Keeps the engine and open panel across
    │   │   │                             configuration changes
    │   │   └── engine/                   Pure Java, no Android dependencies
    │   │       ├── CalculatorEngine.java   Calculator state and logic: entry, operators,
    │   │       │                           =, %, CE/C, memory, history, errors
    │   │       ├── DecimalMath.java        Square root with Newton's method on BigDecimal
    │   │       ├── NumberFormatter.java    Display formatting: grouping, 16 digits,
    │   │       │                           scientific notation, custom separators
    │   │       ├── ExpressionLabels.java   Localizable symbols and expression formats
    │   │       ├── HistoryEntry.java       One history item (expression + value)
    │   │       ├── Operator.java           ADD, SUBTRACT, MULTIPLY, DIVIDE
    │   │       └── ErrorType.java          Error states mapped to string resources
    │   ├── keepRules/rules.keep      R8 keep rules for release builds
    │   └── res/
    │       ├── layout/
    │       │   ├── activity_main.xml     Header, display, memory row, keypad, panel
    │       │   ├── item_history.xml      Row in the history panel
    │       │   └── item_memory.xml       Row in the memory panel (with MC / M+ / M−)
    │       ├── values/
    │       │   ├── strings.xml           All user-visible text and number separators
    │       │   ├── colors.xml            Windows 11-style palette
    │       │   ├── styles.xml            Key, header and display styles
    │       │   └── themes.xml            Light theme (Theme.Calculator)
    │       ├── values-night/themes.xml   Keeps the light theme in system dark mode
    │       ├── color/calc_key_text.xml   Key text colour, including the disabled state
    │       ├── drawable/                 Key backgrounds and vector icons
    │       ├── font/                     Selawik fonts (open-source Segoe UI alternative)
    │       ├── mipmap-*/                 Launcher icons
    │       └── xml/                      Backup and data-extraction rules
    ├── test/                         JVM unit tests (JUnit)
    └── androidTest/                  Instrumented tests (Espresso)
```

### Architecture

- **`engine/`** contains all the calculator logic and does not use Android classes. It can be
  unit-tested on the JVM and reused outside Android.
- **`CalculatorViewModel`** owns one `CalculatorEngine`, so the display, pending operation,
  history and memory survive configuration changes.
- **`MainActivity`** is a thin view layer. Every key press calls one engine method and then
  `render()`, which redraws the screen from the engine's state. Strings and separators are
  passed to the engine at startup, which keeps localization out of the engine code.
