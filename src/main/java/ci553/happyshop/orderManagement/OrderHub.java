package ci553.happyshop.orderManagement;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.client.orderDashboard.OrderDashboard;
import ci553.happyshop.client.picker.PickerModel;
import ci553.happyshop.storageAccess.OrderFileManager;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * <p>{@code OrderHub} serves as the heart of the ordering system.
 * This class implements the Singleton pattern to ensure a single instance governs
 * all order-related logic across the system.</p>
 *
 * <p> It is the central coordinator responsible for managing all orders. It handles:
 *   Creating and tracking orders
 *   Maintaining and updating the internal order map, <OrderId, OrderState>
 *   Delegating file-related operations (e.g., updating state and moving files) to OrderFileManager class
 *   Loading orders in the "ordered" and "progressing" states from storage during system startup
 *
 * <p> OrderHub also follows the Observer pattern: it notifies registered observers such as OrderDashboard
 * and PickerModel whenever the order data changes, keeping the UI and business logic in sync.</p>
 *
 * <p>As the heart of the ordering system, OrderHub connects customers, pickers, and orderDashboards,
 * managementing logic into a unified workflow.</p>
 */

public class OrderHub  {
    private static OrderHub orderHub; //singleton instance

    // Three folders where order files are stored, organised by order state
    private final Path orderedPath = StorageLocation.orderedPath;
    private final Path progressingPath = StorageLocation.progressingPath;
    private final Path collectedPath = StorageLocation.collectedPath;

    // Three maps used for notifying observers (OrderDashboards and PickerModels)
    private TreeMap<Integer,OrderState> orderMap = new TreeMap<>();
               // All active orders in the system.
              // Collected orders are retained briefly ( 10 seconds after collection).
             // This map is sent to all OrderDashboards for customer tracking.
    private TreeMap<Integer,OrderState> OrderedOrderMap = new TreeMap<>();
    private TreeMap<Integer,OrderState> progressingOrderMap = new TreeMap<>();
    // These two maps are subsets of orderMap, filtered by state (as their names suggest).
   // Together, they form the data sent to PickerModels.

    /**
     * Two Lists to hold all registered OrderDashboard and PickerModel observers.
     * These observers are notified whenever the orderMap is updated,
     * but each observer is only notified of the parts of the orderMap that are relevant to them.
     * - OrderDashboards will be notified of the full orderMap, including all orders (ordered, progressing, collected),
     *   but collected orders are shown for a limited time (10 seconds).
     * - PickerModels will be notified only of orders in the "ordered" or "progressing" states, filtering out collected orders.
     */
    private ArrayList<OrderDashboard> orderDashboardList = new ArrayList<>();
    private ArrayList<PickerModel> pickerModelList = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // used by removeCollectedOrder() method to Removes collected orders
    // after they have been collected for 10 seconds.

    //Singleton pattern
    private OrderHub() {}
    public static OrderHub getOrderHub() {
        if (orderHub == null)
            orderHub = new OrderHub();
            return orderHub;
    }

    //Creates a new order using the provided list of products.
    //and also notify PickerModels and orderDashboards
    public Order newOrder(ArrayList<Product> trolley) {
        int orderId = OrderCounter.generateOrderId(); //get unique orderId
        String orderedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //make an Order Object: id, Ordered_state, orderedDateTime, and productsList(trolley)
        Order theOrder = new Order(orderId,OrderState.ORDERED,orderedDateTime,trolley);

        //write order details to file for the orderId in orderedPath (ie. orders/ordered)
        String orderDetail = theOrder.orderDetails();
        Path path = orderedPath;
        OrderFileManager.createOrderFile(path, orderId, orderDetail);

        orderMap.put(orderId, theOrder.getState()); //add the order to orderMap,state is Ordered initially
        notifyOrderDashboards(); //notify OrderDashboards
        notifyPickerModels();//notify PickerModels
        return theOrder;
    }

    //Registers an OrderDashboard.
    public void registerOrderDashboard(OrderDashboard orderDashboard){
        orderDashboardList.add(orderDashboard);
    }
     //Notifies all registered observer_OrderDashboards of the latest orderMap.
    public void notifyOrderDashboards(){
        for(OrderDashboard dashboard : orderDashboardList){
            dashboard.setOrderMap(orderMap);
        }
    }

    //Registers a PickerModel.
    public void registerPickerModel(PickerModel pickerModel){
        pickerModelList.add(pickerModel);
    }

    //notify all pickers of the latest orderMap (only ordered and progressing states orders)
    public void notifyPickerModels(){
        TreeMap<Integer,OrderState> orderMapForPicker = new TreeMap<>();
        progressingOrderMap = filterOrdersByState(OrderState.PROGRESSING);
        OrderedOrderMap = filterOrdersByState(OrderState.ORDERED);
        orderMapForPicker.putAll(progressingOrderMap);
        orderMapForPicker.putAll(OrderedOrderMap);
        for(PickerModel pickerModel : pickerModelList){
            pickerModel.setOrderMap(orderMapForPicker);
        }
    }

    // Filters orderMap that match the specified state, a helper class used by notifyPickerModel()
    private TreeMap<Integer, OrderState> filterOrdersByState(OrderState state) {
        TreeMap<Integer, OrderState> filteredOrderMap = new TreeMap<>(); // New map to hold filtered orders
        // Loop through the orderMap and add matching orders to filteredOrders
        for (Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            if (entry.getValue() == state) {
                filteredOrderMap.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredOrderMap;
    }


    // Called by PickerModel to request a state change for an order.
    // - Updates the order state
    // - Notifies all observers (OrderDashboards and PickerModels)
    // - Updates and relocates the corresponding order file
    // - If the order enters PROGRESSING state, sends order details to the requesting picker
    public void requestOrderStateChange(int orderId, OrderState newState, PickerModel requestingPicker) {
        if(orderMap.containsKey(orderId) && !orderMap.get(orderId).equals(newState)) {
            //update order state and notify all observers
            orderMap.put(orderId, newState);
            notifyOrderDashboards();
            notifyPickerModels();

            applyOrderStateTransition(orderId,newState); //Updates and relocates the corresponding order file

            // Read order detail and send only to the requesting picker
            // (read AFTER the file has been updated and moved to progressingPath)
            // (applyOrderStateTransition ensures the file is updated and in the correct location)
           if(newState==OrderState.PROGRESSING){
               // File already moved at this point
               String orderDetail = OrderFileManager.readOrderFile(progressingPath,orderId);
               if(orderDetail==null || orderDetail.isEmpty()){
                   orderDetail="No orders found for picking";
               }
               // Send order details only to the requesting picker
               requestingPicker.setOrderDetail(orderDetail);
           } else if(newState==OrderState.COLLECTED){
               removeCollectedOrder(orderId); //Scheduled removal
               requestingPicker.setOrderDetail(""); //Clear detail when order is completed
           }
        }
    }

    // Handles state-specific logic after an order state change
    // - Change orderState in order file
    // - Move the file to new state folder
    private void applyOrderStateTransition(int orderId, OrderState newState) {
        switch(newState){
            case OrderState.PROGRESSING:
                OrderFileManager.updateAndMoveOrderFile(orderId, newState,orderedPath,progressingPath);
                break;
            case OrderState.COLLECTED:
                OrderFileManager.updateAndMoveOrderFile(orderId, newState,progressingPath,collectedPath);
                break;
        }
    }

    /**
     * Removes collected orders from the system after they have been collected for 10 seconds.
     *
     * This ensures that collected orders are cleared from the active order pool and are no longer displayed
     * by the OrderDashboard after the brief period. This keeps the system focused on orders in the
     * "ordered" and "progressing" states.
     * The 10-second delay gives enough time for any final updates, and providing a short window for review of completed orders.
     */
    private void removeCollectedOrder(int orderId) {
        if (orderMap.containsKey(orderId)) {
            // Schedule removal after a few seconds
            scheduler.schedule(() -> {
                orderMap.remove(orderId); //remove collected order
                System.out.println("Order " + orderId + " removed from OrdersMap and Dashboards.");
                notifyOrderDashboards();
            }, 10, TimeUnit.SECONDS );
        }
    }



    //Initializes the internal order map by loading the uncollected orders from the file system.
    // Called during system startup by the Main class.
    public void initializeOrderMap(){
        ArrayList<Integer> orderedIds = orderIdsLoader(orderedPath);
        ArrayList<Integer> progressingIds = orderIdsLoader(progressingPath);
        if(orderedIds.size()>0){
            for(Integer orderId : orderedIds){
                orderMap.put(orderId, OrderState.ORDERED);
            }
        }
        if(progressingIds.size()>0){
            for(Integer orderId : progressingIds){
                orderMap.put(orderId, OrderState.PROGRESSING);
            }
        }
        notifyOrderDashboards();
        notifyPickerModels();
        System.out.println("orderMap initialized. "+ orderMap.size() + " orders in total, including:");
        System.out.println( orderedIds.size() + " Ordered orders, " +progressingIds.size() + " Progressing orders " );
    }

    // Loads a list of order IDs from the specified directory.
    // Used internally by initializeOrderMap().
    private ArrayList<Integer> orderIdsLoader(Path dir) {
        ArrayList<Integer> orderIds = new ArrayList<>();

        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (Stream<Path> fileStream = Files.list(dir)) {
                // Process the stream without checking it separately
                List<Path> files = fileStream.filter(Files::isRegularFile).toList();

                if (files.isEmpty()) {
                    System.out.println(dir + " is empty");
                } else {
                    for (Path file : files) {
                        String fileName = file.getFileName().toString();
                        if (fileName.endsWith(".txt")) { // Ensure it's a .txt file
                            try {
                                int orderId = Integer.parseInt(fileName.substring(0, fileName.lastIndexOf('.')));
                                orderIds.add(orderId);
                                System.out.println(orderId);
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid file name: " + fileName);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading " + dir + ", " + e.getMessage());
            }
        } else {
            System.out.println(dir + " does not exist.");
        }
        return orderIds;
    }

    public TreeMap<Integer,OrderState> getOrderMap(){
        return orderMap;
    }

}
