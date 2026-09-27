package ci553.happyshop.client.warehouse;

import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWCreator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone Warehouse client that can be run independently without launching the full system.
 * It is fully functional on its own.
 */

public class WarehouseClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes the Warehouse client's Model, View, and Controller,and links them together for communication.
     * It also creates the DatabaseRW instance via the DatabaseRWCreator and passed into the warehouse Model using constructor injection.
     *
     * Once the components are linked, the warehouse interface (view) is started.
     *
     * Also creates the dependent HistoryWindow and AlertSimulator,
     * which track the position of the Warehouse window and are triggered by the Model when needed.
     * These components are linked after launching the Warehouse interface.
     */
    @Override
    public void start(Stage window) {
        DatabaseRW databaseRW = DatabaseRWCreator.createDatabaseRW();

        WarehouseView view = new WarehouseView();
        WarehouseController controller = new WarehouseController();
        WarehouseModel model = new WarehouseModel(databaseRW);

        view.controller = controller;
        controller.model = model;
        model.view = view;
        view.start(window);

        HistoryWindow historyWindow = new HistoryWindow();
        AlertSimulator alertSimulator = new AlertSimulator();

        // Link after start warehouse interface
        model.successHistoryWindow = historyWindow;
        model.alertSimulator = alertSimulator;
        historyWindow.warehouseView = view;
        alertSimulator.warehouseView = view;
    }
}
