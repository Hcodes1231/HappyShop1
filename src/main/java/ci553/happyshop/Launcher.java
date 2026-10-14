package ci553.happyshop;

import ci553.happyshop.client.Main;
import javafx.application.Application;

/**
 * The Launcher class serves as the main entry point of the system.
 * It calls the launch() method of the Main class to start the JavaFX application.
 * This class is intentionally kept simple to isolate the bootstrapping logic.
 *
 * @author Shine Shan University of Brighton
 * @version 1.5 Haaroun was here
 */

public class Launcher  {
    /**
     * The main method to start the full system.
     * It launches the Main JavaFX application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);  // Starts the JavaFX application through Main
    }
}
// comment
// In newer Java versions (e.g., JDK 25),
// the entry point no longer requires the class or main method to be 'public'.
// It is kept here to follow the standard Java structure and ensure compatibility with older JDKs.
//
// Note: For all other classes and members, no modifier still means package-private
// (default visibility, sometimes called "friendly").

/**
 * ⚠️ JavaFX Warning (During Development)
 * When running HappyShop in IntelliJ, you may see a message like:
 * - WARNING: A restricted method in java.lang.System has been called
 * - WARNING: java.lang.System::load has been called by javafx.graphics
 *
 * 🤔What this means
 * - JavaFX loads native libraries when it starts.
 * - On newer JDKs (22+), the JVM prints a warning when native code is loaded.
 * - IntelliJ applies JVM options after JavaFX starts, so these warnings are safe to ignore during development.
 * - This is normal for JavaFX projects on modern JDKs.
 *
 * ✅No action is required.
 * The warning is harmless and does not affect your program.
 *
 * 📦Why the warning doesn’t matter?
 * This warning only appears during development inside IntelliJ.
 * It does not appear in the final packaged application.
 * When packaging the application (e.g., using jlink or jpackage), the JVM options are applied before JavaFX loads, so the warning disappears.
 */