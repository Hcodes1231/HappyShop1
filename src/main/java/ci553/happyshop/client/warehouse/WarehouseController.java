package ci553.happyshop.client.warehouse;

public class WarehouseController {
    public WarehouseModel model;

    void process(String action) {
        switch (action) {
            case "🔍":
                model.doSearch();
                break;
            case "Edit":
                model.doEdit();
                break;
            case "Delete":
                model.doDelete();
                break;
            case "Submit":
                model.doSummit();
                break;
            case "Cancel":
                model.doCancel();
                break;
        }
    }
}
