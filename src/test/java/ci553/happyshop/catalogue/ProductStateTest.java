package ci553.happyshop.catalogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State-based testing example used in the topic:
 * "What Extra to Test in OOP".
 *
 * These tests are written based on the provided HappyShop codebase.
 *
 * Notes:
 * - The examples shown in lectures may evolve as the teaching progresses.
 * - If you use these tests with your own implementation, you may
 *   need to adjust the tests accordingly.
 */

class ProductStateTest {

    @Test
    void productState_afterSetOrderedQuantity() {
        // Arrange: create product with initial state
        Product product = new Product(
                "0001", "toast", "0003.jpg", 15.19, 100);

        // initial state
        assertEquals(1, product.getOrderedQuantity());

        // method under test changes its own state
        product.setOrderedQuantity(5);

        // verify observable state
        assertEquals(5, product.getOrderedQuantity());
    }
}