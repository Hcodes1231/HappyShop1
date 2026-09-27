package ci553.happyshop.storageAccess;

import ci553.happyshop.catalogue.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Exception handling note:
 * This class is part of the main application workflow and is called by other components.
 *
 * Exceptions are handled locally using try–catch blocks to prevent
 * database/file exceptions from propagating to higher-level classes.
 * This keeps the public API cleaner and avoids spreading exception
 * handling responsibilities across the system.
 *
 * Other file access classes (e.g., ImageFileManager, OrderFileManager, OrderCounter)
 * follow the same exception handling approach as this class: exceptions
 * are handled locally rather than propagated to callers.
 */

/** ProductTable definition
 * "CREATE TABLE ProductTable(" +
 *         "productID CHAR(4) PRIMARY KEY," +
 *         "description VARCHAR(100)," +
 *         "unitPrice DOUBLE," +
 *         "image VARCHAR(100)," +
 *         "inStock INT," +
 *         "CHECK (inStock >= 0)" +
 *           ")",
 */

public class DerbyRW implements DatabaseRW {
    private static String dbURL = DatabaseRWCreator.dbURL; // Shared by all instances

    @Override
    // Searches for products using a query string.
    // First attempts to find a product by exact ID match.
    // If no match is found, performs a keyword search on the product description.
    // Currently used by warehouseModel.
    // Try to use this method to upgrade customer client so that user can search by id or keyword
    public ArrayList<Product> searchProducts(String query) {
        ArrayList<Product> products = new ArrayList<>();

        // searching by ID at first
        Product product = searchProductById(query);
        if (product != null) {
            products.add(product);
        } else { // If no products found by ID, searching by keyword (part of the product description)
            products = searchProductByKeyword(query);
        }

        // If still no products found, print a message
        if (products.isEmpty()) {
            System.out.println("Product " + query + " not found.");
        }
        return products;
    }

    @Override
    // Searches for product using unique id, returns a product or null if none found.
    public Product searchProductById(String id) {
        Product product = null;
        String query = "SELECT * FROM ProductTable WHERE productID = ?";

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            // Set the productId parameter
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()){
                    product = mapRowToProduct(rs);
                    printProduct(product); //Optional printing for debugging purposes
                }else{
                    System.out.println("Product " + id + " not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database query error");
            e.printStackTrace();
        }
        return product;
    }

    @Override
    // Searches for products whose description contains the given keyword.
    // Returns a list of matching products or empty list if none found
    public ArrayList<Product> searchProductByKeyword(String keyword) {
        ArrayList<Product> productList = new ArrayList<>();
        String query = "SELECT * FROM ProductTable WHERE LOWER(description) LIKE LOWER(?)";

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + keyword.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product product = mapRowToProduct(rs);
                    productList.add(product);// Add all matching products to list
                    printProduct(product); //Optional printing for debugging purposes
                }

                if (productList.isEmpty()) {
                    System.out.println("Product " + keyword + " not found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database query error, search by name: " + keyword + " " + e.getMessage());
        }

        return productList; // could be empty if no matches
    }

    @Override
    public ArrayList<Product> purchaseStocks(ArrayList<Product> proList)  {

        ArrayList<Product> insufficientProducts = new ArrayList<>();

        String checkSql = "SELECT inStock FROM ProductTable WHERE productId = ?";
        String updateSql = "UPDATE ProductTable SET inStock = inStock - ? WHERE productId = ?";

        // Use try-with-resources for Connection and PreparedStatements
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            conn.setAutoCommit(false); // Turn off auto-commit for transaction

            // Use a second try-with-resources for the PreparedStatements
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

                boolean allSufficient = true; // Flag to track if all products have sufficient stock

                for (Product product : proList) {
                    checkStmt.setString(1, product.getProductId());
                    try(ResultSet rs = checkStmt.executeQuery()){
                        if (rs.next()) {
                            int currentStock = rs.getInt("inStock");
                            int newStock = currentStock - product.getOrderedQuantity();

                            // Debugging: Print values before update
                            System.out.println("Product ID: " + product.getProductId());
                            System.out.println("Before change: " + currentStock);
                            System.out.println("Quantity Ordered: " + product.getOrderedQuantity());

                            if (newStock >= 0) { // Ensure stock doesn't go negative
                                updateStmt.setInt(1, product.getOrderedQuantity());
                                updateStmt.setString(2, product.getProductId());
                                updateStmt.addBatch();

                                // Debugging: Print values after update
                                System.out.println("After change: " + newStock);
                                System.out.println("Update successful for Product ID: " + product.getProductId());
                            } else {
                                // Update the product stock to the latest value from the database.
                                // The stock may have changed while the customer was shopping
                                // (e.g. reduced by other customers' purchases or warehouse updates).
                                // This ensures the customer sees the correct available quantity at checkout,
                                // instead of an outdated value stored in memory.
                                product.setStockQuantity(currentStock);
                                insufficientProducts.add(product);
                                allSufficient = false; // Mark that there's at least one insufficient product
                                System.out.println("Not enough stock for Product ID: " + product.getProductId());
                            }
                            System.out.println("--------------------------------");
                        }else{
                            // The product was added to the trolley earlier, but has since been deleted
                            // from the warehouse (database) during the customer's shopping process.
                            // Therefore, it is treated as insufficient at checkout.
                            product.setStockQuantity(0);
                            insufficientProducts.add(product);
                            allSufficient = false;
                        }
                    }
                }

                if (allSufficient) {
                    // If all products have sufficient stock, execute the batch and commit
                    updateStmt.executeBatch();
                    conn.commit();  // Commit all updates if all updates succeed
                    System.out.println("Database update successful.");
                } else {
                    // If there's insufficient stock for any product, rollback the entire transaction
                    conn.rollback();
                    System.out.println("Insufficient stock for some products, all updates rolled back.");
                }

            } catch (SQLException e) {
                conn.rollback();  // Rollback if anything failed inside
                System.out.println("Database update error, update failed");
            }
        } catch (SQLException e){
            System.out.println("Database error, update failed");
        }

        return insufficientProducts;
    }


    // Warehouse edits an existing product identified by ID:
    // - Updates product attributes (description, price, image).
    // - Adjusts stock by a relative amount (e.g., 10, +10, -5, or 0).
    //  (see interface for design note).
    public DbOperationResult updateProduct(String id, String des, double price, String imageName, int stockChangeBy) {
        String selectSql = "SELECT * FROM ProductTable WHERE productID = ?";
        String updateSql = "UPDATE ProductTable SET " +
                "description = ?, " +
                "unitPrice = ?, " +
                "image = ?, " +
                "inStock = inStock + ? " +
                "WHERE productID = ?";

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            // Pre-check before update
            selectStmt.setString(1,id);
            try(ResultSet rs = selectStmt.executeQuery()){
                if (!rs.next()) {
                    System.out.println("Product not found: " + id);
                    return DbOperationResult.NOT_FOUND;
                    // The product no longer exists.
                }
            }

            //update
            updateStmt.setString(1, des);
            updateStmt.setDouble(2, price);
            updateStmt.setString(3, imageName);
            updateStmt.setInt(4, stockChangeBy);
            updateStmt.setString(5, id);

            try {
                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected == 0) {
                    // Re-check existence (handle concurrency case)
                    selectStmt.setString(1, id); // ⚠️ IMPORTANT: reset parameter
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            return DbOperationResult.NOT_FOUND;
                        }
                    }
                }
                return DbOperationResult.SUCCESS;
            } catch (SQLException e) {
                if ("23513".equals(e.getSQLState())) {
                    // SQLState 23513 = CHECK constraint violation
                    // In this context, it means the update would make inStock < 0
                    return DbOperationResult.INVALID_STOCK;
                }
                return DbOperationResult.FAILED;
            }
        } catch (SQLException e) {
            System.out.println("Database update error, updating failed");
            return DbOperationResult.FAILED;
        }
    }

    @Override
    //warehouse deletes an existing product
    public boolean deleteProduct(String proId)  {
        String selectSql = "SELECT * FROM ProductTable WHERE productID = ?";
        String deleteSql = "DELETE FROM ProductTable WHERE productID = ?";

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            conn.setAutoCommit(true); // Set auto-commit to true immediately

            // Query and print product details before deletion
            selectStmt.setString(1, proId);
            try(ResultSet rs = selectStmt.executeQuery()){
                if (rs.next()) {
                    System.out.println("Before delete:");
                    printProduct(mapRowToProduct(rs)); //printing
                } else {
                    System.out.println("Product not found: " + proId);
                    return false;
                    // The product no longer exists due to a concurrent deletion
                    // by another warehouse staff member.
                }
            }

            // delete from database
            deleteStmt.setString(1, proId);
            deleteStmt.executeUpdate();
            System.out.println("Product " + proId + " deleted from database.");
            return true;
        } catch (SQLException e){
            System.out.println("Database delete product error, deleting failed");
            return false; // should not happen
        }
    }


    @Override
    //   /images/0001.jpg
    //warehouse adds a new product to database
    public boolean insertNewProduct(String id, String des, double price, String image, int stock) {

        String insertSql = "INSERT INTO ProductTable VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            insertStmt.setString(1, id);
            insertStmt.setString(2, des);
            insertStmt.setDouble(3, price);
            insertStmt.setString(4, image);
            insertStmt.setInt(5, stock);

            try{
                insertStmt.executeUpdate();
                System.out.println("Insert successful for Product ID:" + id);
                return true;
            }catch(SQLException e){
                System.out.println("Insert failed (possibly duplicate ID)");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Database insert error, insert failed");
            return false;
        }
    }
    @Override
    //Checks whether a product exists for the given ID.
    public boolean productExists(String productId) {
        String query = "SELECT COUNT(*) FROM ProductTable WHERE productID = ?";
        //COUNT: the count of records that match the given ID.

        try (Connection conn = DriverManager.getConnection(dbURL);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, productId);

            // the rs is the COUNT(*) result (a single number): how many records that match the given productId.
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { // Move cursor to the first (and only) row
                    int count = rs.getInt(1); // Get the first column value (the count)
                    return count > 0;
                }
            }

        } catch (SQLException e) {
            System.out.println("Database query product ID error");
        }

        return false; // fallback (unlikely)
    }

    /*
     * ----Maps a database record to a Product object----
     * ----Exception handling note:----
     * This is a private helper method used only within this class.
     * It declares exceptions using 'throws' and relies on its caller to
     * handle them. This avoids duplicated try–catch blocks and keeps the
     * helper focused on its core responsibility.
     */
    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        String productId = rs.getString("productID");
        String description = rs.getString("description");
        String imagePath = rs.getString("image");
        double unitPrice = rs.getDouble("unitPrice");
        int inStock = rs.getInt("inStock");

        return new Product(productId, description, imagePath, unitPrice, inStock);
    }

    // Prints product details.
    private void printProduct(Product product) {
        System.out.println("Product ID: " + product.getProductId());
        System.out.println("Description: " + product.getProductDescription());
        System.out.println("Image: " + product.getProductImageName());
        System.out.println("Unit Price: " + product.getUnitPrice());

        int stock = product.getStockQuantity();

        if (stock <= 0) {
            System.out.println("Product " + product.getProductId() + " is NOT in stock");
        } else if (stock < 10) {
            System.out.println("Product " + product.getProductId() + " low stock warning! " + stock + " units left.");
        } else {
            System.out.println("Product " + product.getProductId() + " is available");
        }

        System.out.println("-----"); // Divider for readability
    }

}
