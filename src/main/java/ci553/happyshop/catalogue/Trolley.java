
package ci553.happyshop.catalogue;

import java.util.ArrayList;

/**
 * Represents a customer's shopping trolley in HappyShop.
 *
 * Responsibilities:
 * - Stores products added to the trolley.
 * - Merges products with the same product ID by combining quantities.
 * - Sorts products by product ID.
 * - Allows products to be retrieved and the trolley to be cleared.
 *
 * Implementation:
 * Approach 1 (simplest): The addProduct() method handles
 * merging and sorting directly.
 *
 * Other possible approaches include:
 * - Approach 2: Create a separate addProductOrganised() method.
 * - Approach 3: Create an OrganizedTrolley subclass.
 * - Approach 4: Use a factory method or Factory Pattern.
 */
public class Trolley {

    private ArrayList<Product> products = new ArrayList<>();

    /**
     * Adds a product to the trolley.
     *
     * If a product with the same ID already exists, its quantity
     * is increased. Otherwise, the product is added to the list.
     * Products are then sorted by product ID.
     *
     * @param product the product to add to the trolley
     * @throws IllegalArgumentException if product is null
     */
    public void addProduct(Product product) {

        if (product == null) {
            throw new IllegalArgumentException(
                    "Product cannot be null"
            );
        }

        for (Product existingProduct : products) {
            if (existingProduct.getProductId()
                    .equals(product.getProductId())) {

                existingProduct.setOrderedQuantity(
                        existingProduct.getOrderedQuantity()
                                + product.getOrderedQuantity()
                );

                products.sort(null);
                return;
            }
        }

        products.add(product);
        products.sort(null);
    }

    /**
     * Returns the products currently in the trolley.
     *
     * @return the list of trolley products
     */
    public ArrayList<Product> getProducts() {
        return products;
    }

    /**
     * Removes all products from the trolley.
     */
    public void clear() {
        products.clear();
    }
}
