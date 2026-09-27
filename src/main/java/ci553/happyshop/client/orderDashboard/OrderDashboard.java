package ci553.happyshop.client.orderDashboard;

import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.orderManagement.OrderState;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;
import java.util.TreeMap;

/**
 * OrderDashboard displays active orders and their status for customer tracking.
 * It shows the orderMap (a list of orders with their current status) in a TextArea.
 * The orderMap data is received from the OrderHub, which this dashboard observes.
 */
public class OrderDashboard {
    private final int WIDTH = UIStyle.orderDashBoardWinWidth;
    private final int HEIGHT = UIStyle.orderDashBoardWinHeight;

    // Map of order IDs to their current states, received from OrderHub
    private static final TreeMap<Integer, OrderState> orderMap = new TreeMap<>();
    private final TextArea taOrderMap; //area to show orderMap on the GUI

     //Constructor initializes the UI, a title Label, and a TextArea for displaying the orderMap.
    public OrderDashboard() {
        Label laTitle = new Label("Order_ID,  Status");
        laTitle.setStyle(UIStyle.labelTitleStyle);

        taOrderMap = new TextArea();
        taOrderMap.setEditable(false);
        taOrderMap.setStyle(UIStyle.textFieldStyle);

        VBox vbox = new VBox(10,laTitle, taOrderMap);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setStyle(UIStyle. rootStyleGray);

        Scene scene = new Scene(vbox, WIDTH, HEIGHT);
        Stage window = new Stage();
        window.setScene(scene);
        window.setTitle("🛒Order Dashboard");

        // Registers the window's position with WinPosManager.
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
    }

    /**
     * Registers this OrderDashboard instance with the OrderHub.
     * This allows the OrderDashboard to receive updates on order state changes.
     */
    public void registerWithOrderHub(){
        OrderHub orderHub = OrderHub.getOrderHub();
        orderHub.registerOrderDashboard(this);
    }

    /**
     * Sets the order map with new data and refreshes the display.
     * This method is called by OrderHub when order states are updated.
     */
    public void setOrderMap(TreeMap<Integer, OrderState> om) {
        orderMap.clear(); // Clears the current map to replace it with the new data.
        orderMap.putAll(om);// Adds all new order data to the map.
        displayOrderMap();// Updates the display with the new order map.
    }

     //Displays the current order map in the TextArea.
    private void displayOrderMap() {
        String orderMapText = buildOrderMapText();
        taOrderMap.setText(orderMapText);
    }

    //Builds a formatted string representing the current order map.
    //Iterates over the orderMap and formats each order ID and state.
    //Each line contains the order ID followed by its state, aligned with spacing.
    private String buildOrderMapText() {
        if (orderMap.isEmpty()) {
            return "No orders found";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            sb.append(entry.getKey())
                    .append(" ".repeat(5))
                    .append(entry.getValue())
                    .append("\n");
        }
        return sb.toString();
    }

}
