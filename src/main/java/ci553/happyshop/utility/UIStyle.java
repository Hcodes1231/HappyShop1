package ci553.happyshop.utility;

/**
 * UIStyle is a centralized Java final class that holds all JavaFX UI-related style and size constants
 * used across all client views in the system.
 *
 * These values are grouped here rather than being hardcoded throughout the codebase:
 * - improves maintainability, ensures style consistency,
 * - avoids hardcoded values scattered across the codebase.
 *
 * Example usages:
 * - UIStyle.HistoryWinHeight for setting the height of the order history window
 * - UIStyle.labelStyle for applying consistent styling to labels
 *
 * Design rationale:
 * - Declared as a final class: prevents inheritance and misuse
 * - Private constructor: prevents instantiation (this is a static-only utility class)
 * - Holds only static constants: ensures minimal memory usage and clean syntax
 *
 *  Why a Record is NOT appropriate:
 * - Records are intended for immutable instance data (e.g., DTOs), not static constants
 * - This class has no record components — everything is static
 * - We're using this as a utility container, not a data model
 *
 *  Reminder:
 *  Just because a class has no behavior and only data does NOT mean it should be a record.
 *  If all members are static constants, use a final utility class like this one.
 */

public final class UIStyle {

    //private constructor prevents instantiation
    private UIStyle() {
        throw new UnsupportedOperationException("UIStyle is a utility class");
    }

    //size of all UI windows
    public static final int customerWinWidth = 610;
    public static final int customerWinHeight = 300;
    public static final int removeProNotifierWinWidth = customerWinWidth / 2 + 160;
    public static final int removeProNotifierWinHeight = 230;

    public static final int pickerWinWidth = 310;
    public static final int pickerWinHeight = 300;

    public static final int orderDashBoardWinWidth = 210;
    public static final int orderDashBoardWinHeight = 300;

    public static final int warehouseWinWidth = 610;
    public static final int warehouseWinHeight = 300;
    public static final int AlertSimulatorWinWidth = 300;
    public static final int AlertSimulatorWinHeight = 170;
    public static final int HistoryWinWidth = 300;
    public static final int HistoryWinHeight = 140;

    public static final int EmergencyExitWinWidth = 200;
    public static final int EmergencyExitWinHeight = 300;

    //Button styles
    public static final String buttonStyle = "-fx-font-size: 15";
    public static final String greenFillBtnStyle = "-fx-background-color: green; " +
            "-fx-text-fill: white; -fx-font-size: 14px;";
    public static final String grayFillBtnStyle = "-fx-background-color: gray; " +
            "-fx-text-fill: white; -fx-font-size: 14px; ";
    public static final String blueFillBtnStyle = "-fx-background-color: blue; " +
            "-fx-text-fill: white; -fx-font-size: 14px;";
    public static final String redFillBtnStyle = "-fx-background-color: red; " +
            "-fx-text-fill: white; -fx-font-size: 14px; ";
    public static final String boldPurpleFillBtnStyle = "-fx-background-color: purple; " +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;";

    //Label styles
    public static final String labelStyle = "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: black; " +
            "-fx-background-color: lightblue;";
    public static final String labelTitleStyle = "-fx-font-weight: bold; " +
            "-fx-font-size: 16px; -fx-text-fill: purple;";
    public static final String labelMulLineStyle = "-fx-font-size: 16px; " +
            "-fx-background-color: lightpink";
    public static final String labelPriceStyle = "-fx-font-size: 16px; " +
            "-fx-background-color: lightyellow";
    public static final String labelLowStockStyle =
            "-fx-font-size: 12px; -fx-text-fill: red;";

    //TextField styles
    public static final String textFieldStyle = "-fx-font-size: 16";
    public static final String customerTextFieldStyle = "-fx-font-size: 14px; -fx-pref-width: 175px;";
    public static final String warehouseProductFormTextFieldStyle = "-fx-font-size: 14px; -fx-pref-width: 80px;";
    public static final String smallTextFieldStyle = "-fx-font-size: 14";
    public static final String tinyTextFieldStyle = "-fx-font-size: 12";

    //Styles for AlertSimulator
    public static final String alertBtnStyle = "-fx-background-color: green; " +
            "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;";
    public static final String alertTitleLabelStyle = "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: red; " + "-fx-background-color: lightblue;";
    public static final String alertContentTextAreaStyle = "-fx-font-size: 14px;" +
            "-fx-font-weight: normal;-fx-control-inner-background: lightyellow; -fx-text-fill: darkblue;";
    public static final String alertContentUserActionStyle = "-fx-font-size: 14px;" +
            "-fx-font-weight: normal; -fx-text-fill: green;";

    //ListView, used in warehouse search page, display a list of products
    public static final String listViewStyle = "-fx-border-color: #ccc; " +
            "-fx-border-width: 1px; -fx-background-color: white; -fx-font-size: 14px;";

    //combobox style, used in warehouse stock management page
    public static final String comboBoxStyle = "-fx-font-weight: bold; " +
            "-fx-font-size: 14px;";

    public static final String manageStockEditChildStyle = "-fx-background-color: lightgrey; " +
            "-fx-border-color: lightgrey; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 5px;";

    public static final String manageStockNewChildStyle = "-fx-background-color: lightyellow; " +
            "-fx-border-color: lightyellow; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 5px;";

    //Root styles
    public static final String rootStyleGreen = "-fx-padding: 8px; " +
            "-fx-background-color: lightgreen";
    public static final String rootStyleBlue = "-fx-padding: 8px; " +
            "-fx-background-color: lightblue";
    public static final String rootStyleGray = "-fx-padding: 8px; " +
            "-fx-background-color: lightgray";
    public static final String rootStyleYellow = "-fx-padding: 8px; " +
            "-fx-background-color: lightyellow";
    public static final String rootStylePink = "-fx-padding: 8px; " +
            "-fx-background-color: lightpink";

    //Other styles for future use
    public static final String tooltipStyle = "-fx-background-color: lightyellow; -fx-text-fill: red;";
    public static final String rootVipCustomerStyle = "-fx-padding: 8px; " +
            "-fx-background-color: burlywood";
    public static final String spinnerArrowStyle = "-fx-font-size: 12px; -fx-padding: 0;";
}