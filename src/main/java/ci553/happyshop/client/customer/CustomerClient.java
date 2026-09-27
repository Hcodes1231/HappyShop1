package ci553.happyshop.client.customer;

import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWCreator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone Customer Client that can be run independently without launching the full system.
 * Designed for early-stage testing, though full functionality may require other clients to be active.
 */

public class CustomerClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Responsible for setting up the MVC structure:
     * creates the Model, View, and Controller and links them together.
     *
     * It also creates the DatabaseRW instance via the DatabaseRWCreator
     * and passed into the CustomerModel using constructor injection.
     *
     * Once the components are linked, the customer interface (view) is started.
     *
     * Also creates the RemoveProductNotifier, which tracks the position of the Customer View
     * and is triggered by the Customer Model when needed (used by the extension helper).
     *
     */
    @Override
    public void start(Stage window) {
        DatabaseRW databaseRW = DatabaseRWCreator.createDatabaseRW();

        CustomerView cusView = new CustomerView();
        CustomerController cusController = new CustomerController();
        CustomerModel cusModel = new CustomerModel(databaseRW);

        cusView.cusController = cusController;
        cusController.cusModel = cusModel;
        cusModel.cusView = cusView;
        cusView.start(window);

        // Optional feature: Handling Stock Shortages at Checkout.
        // Code is commented out for now; students may explore this as an extension.
        //RemoveProductNotifier removeProductNotifier = new RemoveProductNotifier();
        //removeProductNotifier.cusView = cusView;
        //cusModel.removeProductNotifier = removeProductNotifier;
    }
}
