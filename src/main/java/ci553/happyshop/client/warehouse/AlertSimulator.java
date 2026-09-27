package ci553.happyshop.client.warehouse;

import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WindowBounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A simple alert simulation window used to display user-facing error messages.
 *
 * It presents:
 * - input validation errors when adding or editing products
 * - operation failures from the database layer (e.g. update/delete failure)
 *
 * This class provides immediate feedback to the user when an action cannot be completed.
 */

/**
 * Why Simulate an Alert Instead of Using a Real One?
 *
 * We use a custom alert simulator instead of the built-in JavaFX Alert
 * to meet the following design requirements:
 *
 * 1. Keep the application responsive:
 *    A standard Alert is modal and blocks the entire application,
 *    preventing any other actions until it is dismissed.
 *    Our custom alert allows other parts of the system to remain usable.
 *
 * 2. Customisable appearance and behaviour:
 *    The built-in Alert provides limited control over styling and layout.
 *    By using a custom Stage, we can fully control the visual design
 *    (e.g. emojis, text styles, layout, and button positioning)
 *    to better match our UI design.
 *
 * 3. Control over window behaviour:
 *    Using a custom Stage with StageStyle.UNDECORATED removes default
 *    window controls (minimise, resize, close), ensuring the alert
 *    stays visible and must be acknowledged by the user.
 *
 * This approach provides greater control over user interaction,
 * visual presentation, and error handling behaviour than the standard Alert.
 */

/**
 * How We Built it
 * - The scene is created only once to avoid unnecessary recreation of the same layout.
 * - The window is created and shown only when needed. If the window is already open, it will be brought to the front.
 * - The `window` and `scene` are managed separately, allowing the scene to be reused while creating the window as needed.
 * - The window is closed and reset when the "Ok" button is clicked.
 * - The window is also closed when cancel or submit was clicked even the window is showing
 * This design ensures that the alert window is efficient by not recreating the scene multiple times, and the window only exists when necessary.
 */

public class AlertSimulator {
    private static int WIDTH = UIStyle.AlertSimulatorWinWidth;
    private static int HEIGHT = UIStyle.AlertSimulatorWinHeight;

    public WarehouseView warehouseView;
    private  Stage window; //window for AlertSimulator
    private  Scene scene; // Scene for AlertSimulator
    private  Label laErrorMsg;// Label to display error messages
    private TextArea taErrorMsg;// Label to display error messages

    // Create the Scene (only once)
    private  void createScene() {
        Label laTitle = new Label("\u26A0 Action cannot be completed"); // for emoji ⚠️
        laTitle.setStyle(UIStyle.alertTitleLabelStyle); //red

        taErrorMsg = new TextArea();
        taErrorMsg.setEditable(false);
        taErrorMsg.setWrapText(true);// text wraps if long
        taErrorMsg.setStyle(UIStyle.alertContentTextAreaStyle);

        VBox vbLaTaMsg = new VBox(3, laTitle, taErrorMsg); 
        vbLaTaMsg.setAlignment(Pos.CENTER_LEFT); // Wrap two labels in a VBox to align it left

        Button btnOk = new Button("Ok");
        btnOk.setStyle(UIStyle.alertBtnStyle);
        HBox hbBtnOk = new HBox(btnOk);
        hbBtnOk.setAlignment(Pos.CENTER); //aligned to right
        btnOk.setOnAction(e -> {
            window.close();
        });

        VBox vb = new VBox(2,vbLaTaMsg, hbBtnOk);
        vb.setAlignment(Pos.CENTER);
        vb.setStyle(UIStyle.rootStyleGray);
        scene = new Scene(vb, WIDTH,HEIGHT);
    }

    // Create the window if not exists
    //also recreate a window if the user closed it with error message in it
    private  void createWindow() {
        if (scene == null) {
            createScene();  //create scene if not exists
        }

        window = new Stage();
        window.initModality(Modality.NONE); //Optional: explicitly set as non-blocking, though this is the default
        window.initStyle(StageStyle.UNDECORATED); // No title bar
        //window.setTitle("\uD83C\uDFEC input error message"); // for icon 🏬
        window.setScene(scene);

        //get bounds of warehouse window which trigers the alertSimulator
        // so that we can put the alertSimulator on top of it and at a suitable position
        WindowBounds bounds = warehouseView.getWindowBounds();
        window.setX(bounds.x + bounds.width-10);
        window.setY(bounds.y + UIStyle.HistoryWinHeight+30);
        window.show();
    }

    // Show error message in the alert window
    public  void showErrorMsg(String errorMsg) {
        if (window ==null ||!window.isShowing() ) {
            createWindow(); // create window if not exists
        }

        //laErrorMsg.setText(errorMsg); // Update the error message
        taErrorMsg.setText(errorMsg); // Update the error message
        window.toFront(); // Bring the window to the front if it's already open
    }

    /**
     * Closes the alert window.
     *
     * The purpose of this method is to provide a way to close the alert window from outside the AlertSimulator class,
     * when it is no longer needed (e.g., after canceling or submitting an action while the alert window is still showing
     * from a previous error).
     */

    public  void closeAlertSimulatorWindow() {
        if (window != null && window.isShowing()) {
            window.close();
        }
    }
}
