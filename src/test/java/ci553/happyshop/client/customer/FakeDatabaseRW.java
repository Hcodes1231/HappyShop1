package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DbOperationResult;

import java.util.ArrayList;

/**
 * Behaviour-based testing examples used in the topic:
 * "What Extra to Test in OOP".
 */
// Used by CustomerModelBehaviourTest
public class FakeDatabaseRW implements DatabaseRW {

    @Override
    public ArrayList<Product> purchaseStocks(ArrayList<Product> proList)  {
        ArrayList<Product> insufficientProducts = new ArrayList<>();
        for(Product p: proList) {
            int qty = p.getOrderedQuantity();
            if(qty > 50) insufficientProducts.add(p);
        }
        return insufficientProducts;
    }

    @Override
    public DbOperationResult updateProduct(String id, String des, double price, String imageName, int stockChangeBy) {
        return null;
    }

    @Override
    public ArrayList<Product> searchProducts(String query)  {
        return null;
    }

    @Override
    public Product searchProductById(String id)  {
        return null;
    }

    @Override
    public ArrayList<Product> searchProductByKeyword(String keyword) {return null;}

    @Override
    public boolean insertNewProduct(String id, String des, double price, String image, int stock)  { return false;}

    @Override
    public boolean deleteProduct(String id) {
        return false;
    }

    @Override
    public boolean productExists(String productId) {
        return false;
    }

}

