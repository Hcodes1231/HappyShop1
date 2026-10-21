
package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.Trolley;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// This class includes TODO comments in addToTrolley() and checkout()
// indicating possible extension ideas to explore.
public class CustomerModel {

    public CustomerView cusView; // part of MVC
    private DatabaseRW databaseRW; // Interface type, not specific implementation
    // Benefits: Flexibility: Easily change the database implementation.

    private Product searchResult = null; // product found from search
    private Trolley trolley = new Trolley(); // holds the products currently in the customer's trolley

    // Data passed to CustomerView for UI updates (including search, trolley and receipt page)
    private String searchResultImageName = "imageHolder.jpg";
    // Image file name representing the current search result.
    // - Product image if a product is found
    // - Default placeholder image otherwise

    private String searchResultProductText = "No product has been searched yet";
    // Textual representation of the product from the search result.
    // - Shows product details (e.g. "0001, 40-inch TV, ...") if found
    // - Otherwise shows a status message

    private String trolleyContentsText = "";
    // Textual representation of current trolley contents,
    // used for display in the Trolley page

    private String receiptContentsText = "";
    // Textual representation of the generated receipt after checkout,
    // used for display in the Receipt page

    // Dependency Injection via constructor
    public CustomerModel(DatabaseRW databaseRW) {
        this.databaseRW = databaseRW;
    }

    // SELECT productID, description, image, unitPrice, inStock quantity
    void search() {
        String productId = cusView.tfId.getText().trim();
        String keyword = cusView.tfKeyword.getText().trim();

        // Use the product ID if entered; otherwise, search by keyword.
        String query = !productId.isEmpty() ? productId : keyword;

        if (!query.isEmpty()) {
            // Search by exact ID first, then by description keyword.
            ArrayList<Product> products = databaseRW.searchProducts(query);

            searchResult = null;

            // Display the first matching product that is in stock.
            for (Product product : products) {
                if (product.getStockQuantity() > 0) {
                    searchResult = product;
                    break;
                }
            }

            if (searchResult != null) {
                double unitPrice = searchResult.getUnitPrice();
                String description = searchResult.getProductDescription();
                int stock = searchResult.getStockQuantity();

                String baseInfo = String.format(
                        "Product_Id: %s\n%s,\nPrice: £%.2f",
                        searchResult.getProductId(),
                        description,
                        unitPrice
                );

                String quantityInfo = stock < 100
                        ? String.format("\n%d units left.", stock)
                        : "";

                searchResultProductText = baseInfo + quantityInfo;
                System.out.println(searchResultProductText);
            } else {
                if (products.isEmpty()) {
                    searchResultProductText =
                            "No matching products found for: " + query;
                } else {
                    searchResultProductText =
                            "Matching products were found, but none are in stock.";
                }

                System.out.println(searchResultProductText);
            }
        } else {
            searchResult = null;
            searchResultProductText =
                    "Please type a Product ID or keyword.";
            System.out.println(searchResultProductText);
        }

        updateView();
    }

    void addToTrolley() {
        if (searchResult != null) {
            // Add the selected product to the trolley.
            // Trolley.addProduct() merges products with the same ID
            // and sorts the trolley by product ID.
            trolley.addProduct(searchResult);

            // Build a formatted string so the trolley contents can be displayed in the UI.
            trolleyContentsText =
                    ProductListFormatter.buildString(trolley.getProducts());
        } else {
            searchResultProductText =
                    "Please search and get an available product before adding to trolley";
            System.out.println(
                    "Must search and get an available product before adding to trolley"
            );
        }

        receiptContentsText = "";
        // Clear receipt to switch back to trolleyPage (receipt shows only when not empty).
        updateView();
    }

    // Organizes the trolley by product ID.
    // Trolley.addProduct() already merges duplicate IDs and sorts the list.
    // This method can also be called when the customer wants to organize the trolley.
    public void makeOrganizedTrolley() {
        trolley.getProducts().sort(null);

        trolleyContentsText =
                ProductListFormatter.buildString(trolley.getProducts());

        updateView();
    }

    public void checkOut() {
        if (!trolley.getProducts().isEmpty()) {
            // Group the products in the trolley by productId to optimize stock checking.
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedProducts =
                    groupProductsById(trolley.getProducts());

            ArrayList<Product> insufficientProducts =
                    databaseRW.purchaseStocks(groupedProducts);

            if (insufficientProducts.isEmpty()) {
                // If stock is sufficient for all products
                // Get OrderHub and tell it to make a new Order.
                OrderHub orderHub = OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley.getProducts());

                trolley.clear();
                trolleyContentsText = "";
                searchResult = null;
                searchResultProductText = "Thank you for shopping with us.";

                receiptContentsText = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );

                System.out.println(receiptContentsText);
            } else {
                // Some products have insufficient stock — build an error message to inform the customer.
                StringBuilder errorMsg = new StringBuilder();

                for (Product p : insufficientProducts) {
                    errorMsg.append("\u2022 ")
                            .append(p.getProductId())
                            .append(", ")
                            .append(p.getProductDescription())
                            .append(" (Only ")
                            .append(p.getStockQuantity())
                            .append(" available, ")
                            .append(p.getOrderedQuantity())
                            .append(" requested)\n");
                }

                searchResult = null;

                // TODO:
                // 1. Remove products with insufficient stock from the trolley.
                // 2. Trigger a message window to notify the customer about insufficient stock,
                // rather than directly changing searchResultProductText.
                // You can use the provided RemoveProductNotifier class and its showRemovalMsg method.
                // Remember to close the message window where appropriate using closeNotifierWindow().

                searchResultProductText =
                        "Checkout failed due to insufficient stock for the following products:\n"
                                + errorMsg;

                System.out.println("Stock is not enough");
            }
        } else {
            trolleyContentsText = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }

        // Refresh the trolley display after checkout.
        trolleyContentsText =
                trolley.getProducts().isEmpty() && !receiptContentsText.isEmpty()
                        ? ""
                        : trolleyContentsText;

        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given productId once,
     * rather than repeatedly.
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();

        for (Product p : proList) {
            String id = p.getProductId();

            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(
                        existing.getOrderedQuantity() + p.getOrderedQuantity()
                );
            } else {
                // Make a shallow copy to avoid modifying the original.
                grouped.put(
                        id,
                        new Product(
                                p.getProductId(),
                                p.getProductDescription(),
                                p.getProductImageName(),
                                p.getUnitPrice(),
                                p.getOrderedQuantity(),
                                p.getStockQuantity()
                        )
                );
            }
        }

        return new ArrayList<>(grouped.values());
    }

    void cancel() {
        trolley.clear();
        trolleyContentsText = "";
        receiptContentsText = "";
        updateView();
    }

    void closeReceipt() {
        receiptContentsText = "";
        updateView();
    }

    void updateView() {
        if (searchResult != null) {
            searchResultImageName = searchResult.getProductImageName();

            String relativeImageUrl =
                    StorageLocation.imageFolder + searchResultImageName;
            // Relative file path, e.g. images/0001.jpg.

            // Get the full absolute path to the image.
            Path imageFullPath =
                    Paths.get(relativeImageUrl).toAbsolutePath();

            searchResultImageName = imageFullPath.toUri().toString();
            // Get the image URI and convert it to a String.

            System.out.println("Image absolute path: " + imageFullPath);
            // Debugging to ensure path is correct.
        } else {
            searchResultImageName = "imageHolder.jpg";
        }

        cusView.update(
                searchResultImageName,
                searchResultProductText,
                trolleyContentsText,
                receiptContentsText
        );
    }

    // Extra notes:
    // Path.toUri(): Converts a Path object (a file or directory path) to a URI.
    // File.toURI(): Converts a File object (a file on the filesystem) to a URI.

    // For test
    public Trolley getTrolley() {
        return trolley;
    }
}
