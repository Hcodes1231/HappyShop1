package ci553.happyshop.client.warehouse;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DbOperationResult;
import ci553.happyshop.storageAccess.ImageFileManager;
import ci553.happyshop.utility.StorageLocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class WarehouseModel {

    public WarehouseView view; // part of MVC
    private  DatabaseRW databaseRW; //Interface type, not specific implementation
    //Benefits: Flexibility: Easily change the database implementation.

    private ArrayList<Product> searchResults = new ArrayList<>();
    // Latest search results from the database,
    // also used to update the UI-side copy in WarehouseView.

    // Operation-specific state:
    // Each user action (delete, edit, create) maintains its own reference
    // to avoid unintended interference between concurrent UI workflows.
    private Product productPendingDeletion; // The product currently selected for deletion.
    // It is captured at the moment the user initiates a delete action (clicks the delete button).
    private Product productBeingEdited; // The product currently being edited.
    // It is captured at the moment the user initiates an edit action,
    // It represents the product used to populate the edit form,
    // and remains stable even if the user selects another product in the list.
    private String newProductId;// The ID for a new product being created.

    //Data used to populate the edit product form in WarehouseView
    String productIdEdit ="";
    String productPriceEdit ="";
    String productDescriptionEdit ="";
    String productImageUrlEdit ="WarehouseImageHolder.jpg";

    private ProductFormInputValidator validator = new ProductFormInputValidator();
    // An inner class ProductFormInputValidator:
    // Validates and parses product form input (edit and new), storing parsed values and errors.
    // Any user input errors are displayed via alertSimulator.
    public AlertSimulator alertSimulator;

    /** alertMsg:
     * Holds the current message to be displayed in the alert simulator.
     * This message can originate from:
     * - input validation (via ProductFormInputValidator)
     * - database operation results (e.g. update/delete failure)
     * It acts as a unified message source for the UI, allowing different error sources to be presented consistently to the user.
     */
    private String alertMsg;

    public HistoryWindow successHistoryWindow;
    private ArrayList<String> successHistoryRecords = new ArrayList<>();
    // Stores Manage Product history entries (edit, delete or new), displayed in the HistoryWindow.

    //Identifies the type of product operation performed,
    //used to construct a corresponding success record message.
    private enum ProductOperationType {
        PRODUCT_UPDATED,
        PRODUCT_DELETED,
        PRODUCT_CREATED
    }

    /**
     * Note:
     * These values describe the intended UI update (what the view should display),
     * not the user action that triggered it.
     * A single user action may involve multiple internal operations,
     * but only one final view update type is passed to updateView().
     */
    private enum ViewUpdateType {
        DISPLAY_SEARCH_RESULTS,  //Search page updates after search
        //actually its updating the Observable ProductList
        LOAD_PRODUCT_FOR_EDIT, //action in search page, populate edit form/page

        // Successful operations (trigger history + reset UI)
        PRODUCT_DELETED, //action in search page, refresh search page
        PRODUCT_UPDATED, //action in editing product page
        PRODUCT_CREATED, //action in adding new product page

        // Form reset actions
        CANCEL_EDIT, //action in Editing product page
        CANCEL_NEW, //action in adding new product page

        // Alert simulator display (user input error and failure database operation)
        SHOW_ALERT_MESSAGE
    }


    public WarehouseModel(DatabaseRW databaseRW){
        this.databaseRW=databaseRW;
    }

    void doSearch() {
        String keyword = view.tfSearchKeyword.getText().trim();
        if (!keyword.isEmpty()) {
            searchResults = databaseRW.searchProducts(keyword);
        }
        else{
            searchResults.clear();
            System.out.println("please type product ID or a keyword to search");
        }
        updateView(ViewUpdateType.DISPLAY_SEARCH_RESULTS);
    }

    void doDelete() {
        System.out.println("Delete gets called in model");
        Product pro  = view.lvSearchResults.getSelectionModel().getSelectedItem();
        if (pro != null ) {
            productPendingDeletion = pro;
            //update database: delete the product from database
            if(databaseRW.deleteProduct(productPendingDeletion.getProductId())){ // if delete successes
                searchResults.remove(productPendingDeletion); //remove the product from product List

                //delete this product's image from imageFolder "images/"
                String imageName = productPendingDeletion.getProductImageName(); //eg 0011.jpg;
                ImageFileManager.deleteImageFile(StorageLocation.imageFolder, imageName);
                updateView(ViewUpdateType.PRODUCT_DELETED);
                productPendingDeletion = null;
            }
            else{
                alertMsg = "• Delete failed:\n" +
                        " - Product " + productPendingDeletion.getProductId()+ " no longer exists (it may have been deleted by another warehouse staff)\n" +
                        " - Use 'Search' to locate the product\n";
                updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
            }
        }
        else{
            System.out.println("No product was selected");
        }
    }

    void doEdit() {
        System.out.println("Edit gets called in model");
        Product pro = view.lvSearchResults.getSelectionModel().getSelectedItem();
        if (pro != null) {
            if(databaseRW.productExists(pro.getProductId())){
                // Get the product data to populate edit form in WarehouseView
                productBeingEdited = pro;
                productIdEdit = productBeingEdited.getProductId();
                productPriceEdit = String.format("%.2f", productBeingEdited.getUnitPrice());
                productDescriptionEdit = productBeingEdited.getProductDescription();

                String relativeImageUri = StorageLocation.imageFolder + productBeingEdited.getProductImageName();
                Path imageFullPath = Paths.get(relativeImageUri).toAbsolutePath();
                productImageUrlEdit = imageFullPath.toUri().toString();//build the full path Uri
                updateView(ViewUpdateType.LOAD_PRODUCT_FOR_EDIT);

                System.out.println("get new product image name: " + productImageUrlEdit); //optional, debug image path
            }
            else{
                alertMsg = "• Product " + pro.getProductId() +" no longer exists:\n" +
                        " - It may have been deleted by another warehouse staff\n" +
                        " - Use 'Search' to locate the product\n";
                updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
            }
        }
        else{
            System.out.println("No product was selected");
        }
    }

    void doCancel(){
        if(view.productFormMode.equals("EDIT")){
            updateView(ViewUpdateType.CANCEL_EDIT);
            productBeingEdited = null;
        }
        if(view.productFormMode.equals("NEW")){
            updateView(ViewUpdateType.CANCEL_NEW);
            newProductId = null;
        }
    }

    void doSummit() {
        if(view.productFormMode.equals("EDIT")){
            doSubmitEdit();
        }
        if(view.productFormMode.equals("NEW")){
            doSubmitNew();
        }
    }

    private void doSubmitEdit() {
        System.out.println("Submit edit is called");
        if(productBeingEdited !=null) {
            String id= productBeingEdited.getProductId();
            String textPrice =view.tfPriceEdit.getText().trim();
            String textStockChangeBy =view.tfStockChangeByEdit.getText().trim();
            String description = view.taDescriptionEdit.getText().trim();
            String imageName = productBeingEdited.getProductImageName(); //old image

            // Optional early existence check for better UX (may be outdated due to concurrency)
            if (!databaseRW.productExists(id)) {
                alertMsg = "\u2022 Product " + id + " no longer exists (it may have been deleted).\n" +
                        " - Use 'Search' to locate the product\n" +
                        " - Or click 'Cancel' to return.";
                updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
                return;
            }

            // Validate & parse
            validator.validateAndParseEditProduct(textPrice, textStockChangeBy, description);
            if(validator.valid){
                String oldImageName = imageName;
                if(view.isUserSelectedImageEdit){
                    //Builds an image file name for the product using its ID and the user selected image file extension.
                    imageName =ImageFileManager.buildImageFileName(view.userSelectedImageUriEdit,id);
                }
                //update database
                DbOperationResult result = databaseRW.updateProduct(id, validator.description, validator.price, imageName, validator.stockChangeBy);
                switch (result){
                    case SUCCESS:
                        if(view.isUserSelectedImageEdit){
                            ImageFileManager.deleteImageFile(StorageLocation.imageFolder, oldImageName); //delete the old image
                            ImageFileManager.copyFileToDestination(view.userSelectedImageUriEdit, StorageLocation.imageFolder,imageName);
                            //copy the new image to project image folder
                        }
                        updateView(ViewUpdateType.PRODUCT_UPDATED);
                        productBeingEdited =null;
                        break;
                    case NOT_FOUND:
                        alertMsg = "• Update failed.\n" +
                                " - Product " + id + " no longer exists (it may have been deleted by another warehouse staff)\n" +
                                " - Use 'Search' to locate the product.\n" +
                                " - Or click 'Cancel' to return.\n";
                        updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
                        break;
                    case INVALID_STOCK:
                        alertMsg = "• Update failed.\n" +
                                " - Stock cannot go below 0.\n";
                        updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
                        break;
                    case FAILED: //should not happen
                        alertMsg = "• Update failed.\n" +
                                " - Please try again or click 'Cancel' to return.\n";
                        updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
                        break;
                }
            }
            else{
                alertMsg= validator.errorMessage;
                updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
            }
        }
        else{
            System.out.println("No Product Selected");
        }
    }

    private void doSubmitNew() {
        System.out.println("Adding new Pro in model");

        //all info(input from user) about the new product
        newProductId = view.tfIdNewPro.getText().trim();
        String textPrice = view.tfPriceNewPro.getText().trim();
        String textStock = view.tfStockNewPro.getText().trim();
        String description = view.taDescriptionNewPro.getText().trim();
        String iPath = view.imageUriNewPro; //image Path from the imageChooser in View class

        // Optional early existence check for better UX (duplicate prevention)
        if (databaseRW.productExists(newProductId)) {
            alertMsg = "\u2022 Product " + newProductId + " already exists.\n" +
                    " - Use 'Search' to locate the product, then 'Edit'\n" +
                    " - Or click 'Cancel' to return.";
            updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
            return;
        }

        //validate input
        validator.validateAndParseNewProduct(newProductId, textPrice, textStock,
                description, iPath);
        if(validator.valid){
            //Builds an image file name for the product using its ID and the user selected image file extension.
            String imageName= ImageFileManager.buildImageFileName(view.imageUriNewPro,validator.id);
            //insertNewProduct to database (String id, String des,double price,String image,int stock)
            // a record in database looks like ('0001', '40-inch TV', 269.00,'0001.jpg',100)
            // if insert succeeded, copy the user selected image to project image folder
            if(databaseRW.insertNewProduct(validator.id, validator.description, validator.price,
                    imageName,validator.stock)){
                if(ImageFileManager.copyFileToDestination(view.imageUriNewPro, StorageLocation.imageFolder,imageName)){
                    updateView(ViewUpdateType.PRODUCT_CREATED);
                    newProductId = null;
                }
                else{ //copy image failed, should not happen
                    alertMsg = "• Copy image failed: try again.\n";
                    databaseRW.deleteProduct(validator.id);
                }
            }
            else{
                alertMsg = "• Insert failed: Product ID " + validator.id + " already exists.\n" +
                        " - Use 'Search' to locate the product, then choose 'Edit'.\n" +
                        " - Or click 'Cancel' to return.\n";
                updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
            }
        }
        else{
            alertMsg= validator.errorMessage;
            updateView(ViewUpdateType.SHOW_ALERT_MESSAGE);
        }
    }

    /**
     * Updates the UI based on the specified action type.
     * This method centralises all UI updates, ensuring consistent behaviour
     * after user actions or database operations.
     */
    private void updateView(ViewUpdateType updateFor){
        switch (updateFor) {
            case ViewUpdateType.DISPLAY_SEARCH_RESULTS:
                view.updateObservableProductList(searchResults);
                alertSimulator.closeAlertSimulatorWindow();// Close any existing alert window from previous failed operation
                // If search returns no results, clear the edit panel.
                // Prevents stale data when a previously selected product
                // has been deleted by another user.
                if(searchResults.isEmpty()){
                    view.resetEditChild();
                }
                break;
            case ViewUpdateType.LOAD_PRODUCT_FOR_EDIT:
                view.updateEditProductChild(productIdEdit, productPriceEdit, productDescriptionEdit, productImageUrlEdit);
                break;
            case ViewUpdateType.PRODUCT_DELETED:
                view.updateObservableProductList(searchResults); //update search page in view
                showSuccessHistory(ProductOperationType.PRODUCT_DELETED);
                if (productPendingDeletion != null && productBeingEdited != null
                        && productPendingDeletion.equals(productBeingEdited)) {
                    view.resetEditChild();
                }
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case ViewUpdateType.CANCEL_EDIT:
                view.resetEditChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case ViewUpdateType.PRODUCT_UPDATED:
                showSuccessHistory(ProductOperationType.PRODUCT_UPDATED);
                view.resetEditChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case ViewUpdateType.CANCEL_NEW:
                view.resetNewProChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case ViewUpdateType.PRODUCT_CREATED:
                showSuccessHistory(ProductOperationType.PRODUCT_CREATED);
                view.resetNewProChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case ViewUpdateType.SHOW_ALERT_MESSAGE:
                alertSimulator.showErrorMsg(alertMsg);
        }
    }


    //Displays a record of successful product management operations
    //(create, update, delete) in the history window.
    private void showSuccessHistory(ProductOperationType type){
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String record="";
        switch (type) {
            case ProductOperationType.PRODUCT_UPDATED:
                record = productBeingEdited.getProductId() + " edited successfully, " + dateTime;
                break;
            case ProductOperationType.PRODUCT_DELETED:
                record = productPendingDeletion.getProductId() + " deleted successfully, " + dateTime;
                break;
            case ProductOperationType.PRODUCT_CREATED:
                record = newProductId + " added to database successfully, " + dateTime;
        }
        if(!record.isEmpty())
            successHistoryRecords.add(record);
        successHistoryWindow.showManageHistory(successHistoryRecords);
    }

    /**
     * Inner class responsible for validating and parsing user input from product forms.
     *
     * This class handles both "edit product" and "new product" forms:
     * - For edit products, it parses and validates price, stock change, and description.
     * - For new products, it parses and validates product ID, price, stock, description, and image.
     *
     * After validation, it stores the parsed values and any error messages in its fields:
     * - 'valid' indicates whether all inputs passed validation.
     * - 'errorMessage' contains detailed feedback if validation fails.
     *
     * The outer class can use these parsed values directly and display errors via
     * the alertSimulator, keeping input handling and validation centralized and consistent.
     */
    private class ProductFormInputValidator {

        boolean valid; // Indicates whether the validation passed
        String errorMessage; // error messages if validation failed

        // --- Common fields (used in both edit and new product)---
        double price; //Parsed price of the product
        String description; //Parsed description of the product

        // --- Edit-only fields---
        //Parsed stock change value for editing an existing product (can be empty, zero, positive or negative whole numbers)
        int stockChangeBy;

        // --- New product-only fields ---
        String id;//Parsed product ID (exactly 4 digits, unique)
        int stock; //Parsed absolute stock quantity for a new product
        String imageUri; //Parsed image path for a new product (cannot be empty)

        /**
         * Validates and parses input for editing an existing product.
         * @param txPrice User input for price
         * @param txStockChangeBy User input for stock change (can be empty)
         * @param description User input for product description
         */
        void validateAndParseEditProduct(String txPrice,
                                         String txStockChangeBy,
                                         String description) {
            StringBuilder errorMsgBuilder = new StringBuilder();

            // Validate price (must be positive, at most 2 decimal places)
            try {
                validator.price = Double.parseDouble(txPrice);

                if (!txPrice.matches("^[0-9]+(\\.[0-9]{0,2})?$")) {
                    errorMsgBuilder.append("\u2022 Price can have at most two decimal places.\n");
                }
                if (validator.price <= 0) {
                    errorMsgBuilder.append("\u2022 Price must be positive.\n");
                }

            } catch (NumberFormatException e) {
                errorMsgBuilder.append("\u2022 Invalid price format.\n");
            }

            // Validate stock change (empty = 0, or a whole number, e.g., 10, +10, -10)
            if (txStockChangeBy.isEmpty()) {
                validator.stockChangeBy = 0;
            } else {
                try {
                    validator.stockChangeBy = Integer.parseInt(txStockChangeBy);
                } catch (NumberFormatException e) {
                    errorMsgBuilder.append("\u2022 Stock change must be a whole number.\n");
                }
            }

            // Validate description
            if (description.isEmpty()) {
                errorMsgBuilder.append("\u2022 Description cannot be empty.\n");
            } else {
                validator.description = description;
            }

            // Final result
            if (errorMsgBuilder.length() > 0) {
                validator.valid = false;
                validator.errorMessage = errorMsgBuilder.toString();
            } else {
                validator.valid = true;
            }
        }

        /**
         * Validates and parses input for adding a new product.
         * @param id User input for product ID
         * @param txPrice User input for price
         * @param txStock User input for stock quantity
         * @param description User input for description
         * @param imageUri User input for image path or filename
         */
        void validateAndParseNewProduct(String id,
                                        String txPrice,
                                        String txStock,
                                        String description,
                                        String imageUri) {
            StringBuilder errorMsgBuilder = new StringBuilder();

            // Validate ID (must be 4 digits)
            if (id == null || !id.matches("\\d{4}")) {
                errorMsgBuilder.append("\u2022 Product ID must be exactly 4 digits.\n");
            } else {
                validator.id = id;
            }

            // Validate price (must be positive, at most 2 decimal places)
            try {
                validator.price = Double.parseDouble(txPrice);

                // Validate: Ensure at most two decimal places
                if (!txPrice.matches("^[0-9]+(\\.[0-9]{0,2})?$")) {
                    errorMsgBuilder.append("\u2022 Price can have at most two decimal places.\n");
                }
                if (validator.price <= 0) {
                    errorMsgBuilder.append("\u2022 Price must be a positive number.\n");
                }

            } catch (NumberFormatException e) {
                errorMsgBuilder.append("\u2022 Invalid price format.\n");
            }

            // Validate stock (must be non-negative integer)
            try {
                validator.stock = Integer.parseInt(txStock);
                if (validator.stock < 0) {
                    errorMsgBuilder.append("\u2022 Stock quantity cannot be negative.\n");
                }
            } catch (NumberFormatException e) {
                errorMsgBuilder.append("\u2022 Invalid stock quantity format.\n");
            }

            // Validate description
            if (description == null || description.isEmpty()) {
                errorMsgBuilder.append("\u2022 Product description cannot be empty.\n");
            } else {
                validator.description = description;
            }

            // Validate image
            if (imageUri == null) {
                errorMsgBuilder.append("\u2022 An image must be selected.\n");
            } else {
                validator.imageUri = imageUri;
            }

            // Finalize result
            if (errorMsgBuilder.length() > 0) {
                validator.valid = false;
                validator.errorMessage = errorMsgBuilder.toString();
            } else {
                validator.valid = true;
            }
        }
    }
}
