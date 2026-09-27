package ci553.happyshop.storageAccess;

import ci553.happyshop.catalogue.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * The DatabaseRW interface defines the contract for interacting with the product database.
 * It is currently implemented by the DerbyRW class, which provides the actual functionality.
 *
 * Responsibilities:
 * - Searching for products by keyword or product ID.
 * - Performing stock updates and validations during purchases.
 * - Updating, deleting, or inserting products.
 * - Checking whether a product exists.
 *
 * Why use this interface:
 * Introducing this interface allows for better separation of specification and implementation,
 * making the system more adaptable and maintainable.
 *
 * 1. **Ease of Substitution**: If the application switches to a different database system
 *    (e.g., from Derby to MySQL or SQLite), a new implementation can be provided without affecting other components.
 *
 * 2. **Improved Testability**: During testing, mock implementations can simulate database behavior,
 *    enabling effective testing without requiring a live database connection.
 */

public interface DatabaseRW {


    /**
     * Searches for products using a query string.
     * First attempts to find a product by exact ID match.
     * If no match is found, performs a keyword search on the product description.
     *
     * @param query the product ID or keyword
     * @return a list of matching products (empty if none found)
     */
    ArrayList<Product> searchProducts(String query);


    /**
     * Searches for a product by its unique product ID.
     * @param id the product ID
     * @return the matching product, or null if not found
     */
    Product searchProductById(String id);


    /**
     * Searches for products whose description contains the given keyword.
     *
     * @param keyword  the search term to match within product descriptions
     * @return a list of matching products; empty list if none found
     */
    ArrayList<Product> searchProductByKeyword(String keyword);


    /**
     * Attempts to purchase (reduce stocks of) the given list of products.
     * Behavior:
     * - If all requested quantities are available, stocks are reduced and an empty list is returned.
     * - If any product does not have sufficient stock, no stock is updated and a list of all insufficient products is returned.
     *
     * @param proList the list of products with requested quantities to purchase
     */
    ArrayList<Product> purchaseStocks(ArrayList<Product> proList);


    /**
     * Updates the details of a product identified by its ID,
     * and adjusts stock by a relative amount.
     *
     * @param id            the product ID (used to identify the product; not changeable)
     * @param des           the new description
     * @param price         the new price
     * @param imageName     the new image file name
     * @param stockChangeBy the amount to adjust stock (can be negative, zero, or positive)
     * @return true if the update was successful;
     *         false if the product was not found, the stock change is invalid (e.g. results in negative stock),
     *         or the update failed
     *
     * Note:
     * This method applies a relative stock change (adjust semantics).
     * If absolute stock setting is required, a separate method should be introduced,
     * or this method can be adapted by changing the parameter to an absolute stock value
     * and updating the SQL accordingly (e.g., "inStock = ?" instead of "inStock = inStock + ?").
     */
    //boolean updateProductOld(String id, String des, double price, String imageName, int stockChangeBy);

    /**
     * Updates the details of a product identified by its ID,
     * and adjusts stock by a relative amount.
     *
     * @param id            the product ID (used to identify the product; not changeable)
     * @param des           the new description
     * @param price         the new price
     * @param imageName     the new image file name
     * @param stockChangeBy the amount to adjust stock (can be negative, zero, or positive)
     * @return true if the update was successful;
     *         false if the product was not found, the stock change is invalid (e.g. results in negative stock),
     *         or the update failed
     *
     * @return the result of the operation:
     *         SUCCESS if the update is applied,
     *         NOT_FOUND if the product does not exist,
     *         INVALID_STOCK if the stock change is not allowed
     *         FAILED if the update failed
     *
     * Note:
     * This method applies a relative stock change (adjust semantics).
     * If absolute stock setting is required, a separate method should be introduced,
     * or this method can be adapted by changing the parameter to an absolute stock value
     * and updating the SQL accordingly (e.g., "inStock = ?" instead of "inStock = inStock + ?").
     */
    DbOperationResult updateProduct(String id, String des, double price, String imageName, int stockChangeBy);



    /**
     * Inserts a new product into the database.
     * @param id      the product ID
     * @param des     the product description
     * @param price   the product price
     * @param imageName   the image file name
     * @param stock   the initial stock quantity
     * @return true if the product was successfully inserted;
     *         false if a product with the same ID already exists or the insert failed
     */
    boolean insertNewProduct(String id, String des, double price, String imageName, int stock);


    /**
     * Deletes a product identified by its ID.
     * @param id the product ID
     * @return true if the product existed and was successfully deleted;
     *         false if the product was not found or the deletion failed
     */
    boolean deleteProduct(String id);


    /**
     * Checks whether a product exists for the given ID.
     * @param productId the product ID
     * @return true if the product exists; false otherwise
     */
    boolean productExists(String productId);

}


