package ci553.happyshop.client.orderDashboard;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone OrderDashboard client that can be run independently without launching the full system.
 * Designed for early-stage testing, though full functionality may require other clients to be active.
 *
 * This client is simple and does not follow the MVC pattern, as it only registers with the OrderHub
 * to receive order status notifications. All logic is handled internally within the OrderDashboard.
 */

public class OrderDashboardClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage window) {
        OrderDashboard orderDashboard = new OrderDashboard();
        orderDashboard.registerWithOrderHub();
    }
}
