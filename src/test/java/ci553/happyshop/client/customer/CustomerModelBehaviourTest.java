package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.Trolley;
import ci553.happyshop.storageAccess.DatabaseRW;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Behaviour-based testing examples used in the topic:
 * "What Extra to Test in OOP".
 *
 * These tests are written based on the provided HappyShop codebase.
 *
 * Notes:
 * - The examples shown in lectures may evolve as the teaching progresses.
 * - If you use these tests with your own implementation (e.g. after completing
 *   tasks such as OrganizedTrolley or modifying checkOut logic), you may
 *   need to adjust the tests accordingly.
 */

// When running tests that use Mockito, you may see a warning about dynamic agent loading.
// This is expected on newer Java versions and does not affect your test results.
// Mockito uses bytecode instrumentation, which can trigger JVM agent warnings.
// These warnings can be safely ignored or suppressed using VM options if needed.

class CustomerModelBehaviourTest {
    @Test
    public void verifyCheckoutCallsPurchaseStocks() {
        System.out.println("-------Test_1, verify------------");

        // 1. Create a mock for DatabaseRW
        DatabaseRW mockDatabase = mock(DatabaseRW.class);

        // 2. Inject the mock into the CustomerModel
        CustomerModel model = new CustomerModel(mockDatabase);

        //prepare trolley
        Product p1 = new Product("0001", "toast", "0003.jpg", 15.19, 100);
        model.getTrolley().addProduct(p1);

        //mock CustomerView cause checkOut() internally calls updateView() → cusView.update()
        CustomerView mockCusView = mock(CustomerView.class);
        model.cusView = mockCusView;

        // 3. Call the method you want to test
        model.checkOut();

        // 4. Verify that purchaseStocks() was called
        verify(mockDatabase).purchaseStocks(any());
        // We only care that the method was invoked, not the exact argument.
        // The list passed to purchaseStocks() may vary depending on the checkout logic
        // (e.g. whether the trolley is organized or grouped before purchasing),
        // so we use any() to match any argument safely.
    }

    @Test
    void testCheckOutWithStubPurchaseStocks() {
        System.out.println("-------Test_2, Stub------------");

        // Create a mock for DatabaseRW
        DatabaseRW mockDatabase = mock(DatabaseRW.class);
        CustomerModel model = new CustomerModel(mockDatabase);
        // Mock CustomerView
        CustomerView mockCusView = mock(CustomerView.class);
        model.cusView = mockCusView;

        model.checkOut(); //empty trolley, prints "Your trolley is empty" to the console

        //prepare trolley
        Product p1 = new Product("0001", "toast", "0001.jpg", 15.19, 100);
        Trolley trolley = model.getTrolley();
        trolley.addProduct(p1);

        //Using stubs to control program flow:
        //Case 1: stock sufficient
        //When purchaseStocks() is called, returning an empty list → checkout succeeds
        when(mockDatabase.purchaseStocks(trolley.getProducts()))
                .thenReturn(new ArrayList<>());
        model.checkOut(); // checkOut succeeds, a receipt with orderID was printed in the console

        //Case 2: stock insufficient
        trolley.addProduct(p1); // add product again because the trolley was cleared after the successful checkout in Case 1
        ArrayList<Product> insufficient = new ArrayList<>();
        insufficient.add(p1);
        when(mockDatabase.purchaseStocks(any()))
                .thenReturn(insufficient);
        model.checkOut(); // checkOut fails,prints "stock is not enough" to the console

        // No assertions. Here the behavior is primarily observable via print statements.
    }

    @Test
    void testCheckOutWithFakeLikePurchaseStocks() {
        System.out.println("-------Test_3, FakeLike------------");
        DatabaseRW mockDatabase = mock(DatabaseRW.class);
        CustomerModel model = new CustomerModel(mockDatabase);

        CustomerView mockCusView = mock(CustomerView.class);
        model.cusView = mockCusView;

        model.checkOut(); //empty trolley, prints "Your trolley is empty" to the console

        Trolley trolley = model.getTrolley();
        ArrayList<Product> products= trolley.getProducts();
        //Using simplified logic to control program flow:any product was ordered more than 50 is insufficient
        when(mockDatabase.purchaseStocks(any())).thenAnswer(invocationOnMock -> {
            ArrayList<Product> insufficientProducts = new ArrayList<>();
            for(Product p: products) {
                int qty = p.getOrderedQuantity();
                if(qty > 50) insufficientProducts.add(p);
            }
            return insufficientProducts;
        });

        //prepare trolley
        Product p1= new Product("0001", "toast", "0003.jpg", 15.19, 100);
        products.add(p1);
        model.checkOut(); // checkOut succeeds, only 1 product ordered 1, a receipt with orderID was printed in the console

        // prepare trolley again because it was cleared after the successful checkout in Case 1
        p1.setOrderedQuantity(60); //insufficient
        trolley.addProduct(p1);
        model.checkOut(); // checkOut fails,prints "stock is not enough" to the console

        // No assertions. Here the behavior is primarily observable via print statements.
    }

    @Test
    void testCheckOutWithFakeDatabaseRW() {
        System.out.println("-------Test_1, Fake ------------");
        DatabaseRW fakeDatabaseRW = new FakeDatabaseRW();
        CustomerModel model = new CustomerModel(fakeDatabaseRW);

        CustomerView mockCusView = mock(CustomerView.class);
        model.cusView = mockCusView;

        model.checkOut(); //empty trolley, prints "Your trolley is empty" to the console

        Trolley trolley = model.getTrolley();

        //prepare trolley
        Product p1 = new Product("0001", "toast", "0003.jpg", 15.19, 100);
        trolley.addProduct(p1);
        model.checkOut(); // checkOut succeeds, only 1 product ordered 1, a receipt with orderID was printed in the console

        //prepare trolley again because it was cleared after the successful checkout in Case 1
        p1.setOrderedQuantity(60); //insufficient
        trolley.addProduct(p1);
        model.checkOut(); // checkOut fails,prints "stock is not enough" to the console

        // No assertions. Here the behavior is primarily observable via print statements.
    }
}

