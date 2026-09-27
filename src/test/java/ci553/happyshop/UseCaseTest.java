package ci553.happyshop;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.Trolley;
import ci553.happyshop.client.customer.CustomerModel;
import ci553.happyshop.client.customer.CustomerView;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.orderManagement.OrderState;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWCreator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Use Case Testing example used in the topic:
 * "What Extra to Test in OOP".
 *
 * Notes:
 * - The test is written based on the provided HappyShop codebase.
 * - If you use this test with your own implementation,
 *   you may need to adjust it accordingly.
 */

public class UseCaseTest {
    @Test
    void testAddProductAndPlaceOrder() {
        DatabaseRW databaseRW = DatabaseRWCreator.createDatabaseRW();
        CustomerModel cusModel = new CustomerModel(databaseRW);

        Product p = new Product("0001", "toast", "0003.jpg", 15.19, 100);

        // Add to trolley and verify
        Trolley trolley = cusModel.getTrolley();
        trolley.addProduct(p);

        assertTrue(trolley.getProducts().contains(p));

        //mock CustomerView cause checkOut() internally calls updateView() → cusView.update()
        CustomerView mockCusView = mock(CustomerView.class);
        cusModel.cusView = mockCusView;
        // Perform checkout
        cusModel.checkOut();

        // Verify order state was recorded in OrderHub
        OrderHub hub = OrderHub.getOrderHub();
        TreeMap<Integer, OrderState> orderMap = hub.getOrderMap();

        // Check that an order has been added
        assertFalse(orderMap.isEmpty());

        // Get latest entry (highest orderId)
        Map.Entry<Integer, OrderState> latestEntry = orderMap.lastEntry();
        assertEquals(OrderState.ORDERED, latestEntry.getValue());
    }

}
