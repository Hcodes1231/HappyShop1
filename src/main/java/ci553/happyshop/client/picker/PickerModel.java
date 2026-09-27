package ci553.happyshop.client.picker;

import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.orderManagement.OrderState;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * PickerModel represents the logic for an order picker.
 *
 * It has two main responsibilities:
 * 1. Observing OrderHub (data source)
 * 2. Sending data to PickerView for updating the UI (MVC interaction)
 *
 * ─────────────────────────────
 * 1. Observing OrderHub
 * ─────────────────────────────
 * PickerModel acts as an observer of OrderHub and receives the shared orderMap.
 *
 * When a picker requests to prepare an order:
 * - Retrieves the first available (unlocked) order from orderMap
 * - Locks the order to prevent other pickers from accessing it
 * - Requests OrderHub to update the order state
 * - Waits for OrderHub to broadcast the updated orderMap
 * - Receives the assigned order detail and begins preparation
 *
 * When an order is collected by the customer:
 * - Requests OrderHub to update the order state
 * - Waits for OrderHub to broadcast the updated orderMap
 * - Unlocks the order
 * - Requests to prepare next one if available.
 *
 * ─────────────────────────────
 * Centralised State Management
 * ─────────────────────────────
 *  All order state changes are centralized through OrderHub to ensure synchronization.
 *  PickerModel never updates its view directly before OrderHub updates the shared orderMap;
 *  Instead, each PickerModel waits for OrderHub's notification to refresh its state.
 * This guarantees that all pickers remain synchronized.
 *
 * ─────────────────────────────
 * Interaction Flow (Simplified)
 * ─────────────────────────────
 * Claiming an order:
 * PickerModel → "Hey OrderHub, I found this order that needs to be prepared."
 * OrderHub    → updates orderMap
 * OrderHub    → notifies all PickerModels of the updated OrderMap
 * OrderHub    → sends the order detail to the requesting picker
 *
 * Completing (collected):
 * PickerModel → "This order has been collected."
 * OrderHub    → updates orderMap
 * OrderHub    → notifies all PickerModels of the updated OrderMap
 * OrderHub    → clears the assigned order detail for this picker
 *
 * After receiving notifications from OrderHub, PickerModel guides its View to update the UI (MVC flow).
 *
 * ─────────────────────────────
 * Key Design Principle
 * ─────────────────────────────
 * PickerModel does not directly control shared state.
 * All updates flow through OrderHub, ensuring a single source of truth
 * and consistent behaviour across all pickers.
 */

public class PickerModel {
    public PickerView pickerView; //part of MVC
    private OrderHub orderHub = OrderHub.getOrderHub(); //picker observes OrderHub

    // Map of order IDs to their current states, received from OrderHub
   // Used as the data source to build the textual representation for PickerView
    private static TreeMap<Integer, OrderState> orderMap = new TreeMap<>();

    private int assignedOrderId=0; // ID of the order assigned to this picker
                                   // 0 means no order is currently assigned
    private OrderState assignedOrderState; // Current state of the order assigned to this picker.
                              // When this state needs to change, the picker notifies OrderHub
    private String assignedOrderDetailText =""; // Received from OrderHub
                      // Textual representation of the order assigned to this picker,
                     // showing the items that need to be prepared
                     // Also used as the second UI data passed to PickerView (alongside orderMap text).

    private static TreeSet<Integer> lockedOrderIds = new TreeSet<>();
               // Track locked orders by orderId

    /**
     * Attempts to find an unlocked order for this picker and mark it as progressing.
     * The order will be locked to prevent other pickers from accessing it.
     * Only the first unlocked order found will be processed.
     */
    public void doPickNextOrder() {
        for (Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            int orderId = entry.getKey();
            if (!isOrderLocked(orderId)) { // Find the first unlocked order
                lockOrder(orderId);// Lock the order to prevent other pickers from taking it
                assignedOrderId = orderId; // Save the assigned orderId to this picker and update its state
                assignedOrderState = OrderState.PROGRESSING;
                notifyOrderHub();// Notify the OrderHub about the state change
                return; // Exit after handling one order
            }
        }
        // ✅ No order found
        System.out.println("No orders found for picking at the moment.");
    }

    public void doCollected() {
        if(assignedOrderId !=0 && isOrderLocked(assignedOrderId)){
            assignedOrderState = OrderState.COLLECTED;
            notifyOrderHub(); // Notify the OrderHub about the state change
            unlockOrder(assignedOrderId);//remove the order from locked orderId set
            assignedOrderId =0;  //reset to no order is with the picker
        }
    }

    // Registers this PickerModel instance with the OrderHub
    //so it can receive updates from OrderHub.
    public void registerWithOrderHub(){
        orderHub = OrderHub.getOrderHub();
        orderHub.registerPickerModel(this);
    }

    //Notifies the OrderHub of a change in the order state.
    private void notifyOrderHub(){
        orderHub.requestOrderStateChange(assignedOrderId, assignedOrderState,this);
    }

    // Sets the order map with new data and refreshes the display.
    // This method is called by OrderHub to set orderMap for all pickers.
    public void setOrderMap(TreeMap<Integer,OrderState> om) {
        orderMap.clear();
        orderMap.putAll(om);
        updatePickerView(); // Refresh picker view
    }

    // Sets the order detail for picker and refreshes the display.
    // This method is called by OrderHub to set orderDetail for this picker assigned this order to pick .
    public void setOrderDetail(String orderDetail){
        assignedOrderDetailText = orderDetail;
        updatePickerView(); // Refresh picker view
    }

    // Uses data from OrderHub and determines how it is presented in the PickerView
    private void updatePickerView() {
        String orderMapText = buildOrderMapText();
        if(assignedOrderDetailText==null || assignedOrderDetailText.isEmpty()){
            assignedOrderDetailText = "No orders to pick right now.\n" +
                             " - Either there are no active orders,\n"+
                             "- or all orders are currently being prepared by other pickers.\n" +
                            "Click 'Customer Collected' to view orders in progress.\n";
        }
        pickerView.update(orderMapText, assignedOrderDetailText);
    }

    //Builds a formatted string representing the current order map.
    //Each line contains the order ID followed by its state, aligned with spacing.
    private String buildOrderMapText() {
        if (orderMap.isEmpty()) {
            return "No orders are available";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            sb.append(entry.getKey())
                    .append(" ".repeat(8))
                    .append(entry.getValue())
                    .append("\n");
        }
        return sb.toString();
    }

    // Lock an order
    private boolean lockOrder(int orderId) {
        if (lockedOrderIds.contains(orderId)) {
            return false; // Order is already locked
        } else {
            lockedOrderIds.add(orderId);
            return true; // Successfully locked the order
        }
    }

    // Unlock an order
    private void unlockOrder(int orderId) {
        lockedOrderIds.remove(orderId);
    }

    // Check if an order is locked
    private boolean isOrderLocked(int orderId) {
        return lockedOrderIds.contains(orderId);
    }

}
