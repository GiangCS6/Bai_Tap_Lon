package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.entity.AuctionStatus;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellerHistoryController implements Navigable {

    public static class DisplayItem {
        private final String        name;
        private final String        auctionId;
        private final long          startPriceLong;
        private final AuctionStatus status;
        private final String        category;

        public DisplayItem(String name, String category,
                           long startingPrice, String auctionId,
                           AuctionStatus status) {
            this.name           = name;
            this.category       = category;
            this.startPriceLong = startingPrice;
            this.auctionId      = auctionId;
            this.status         = status;
        }

        public String        getName()           { return name; }
        public String        getCategory()       { return category; }
        public long          getStartPriceLong() { return startPriceLong; }
        public String        getAuctionId()      { return auctionId; }
        public AuctionStatus getStatus()         { return status; }
    }

    @FXML private HBox  adminNav;
    @FXML private HBox  sellerNav;
    @FXML private HBox  bidderNav;

    @FXML private Label  userNameLabel;
    @FXML private Label  notificationBadge;
    @FXML private Button walletBtn;
    @FXML private Button bellBtn;

    // ══════════════════════════════════════════════════════
    //  FXML BINDINGS — Content
    // ══════════════════════════════════════════════════════

    @FXML private TableView<DisplayItem>                  postedItemsTable;
    @FXML private TableColumn<DisplayItem, String>        colName;
    @FXML private TableColumn<DisplayItem, String>        colCategory;
    @FXML private TableColumn<DisplayItem, Long>          colStartingPrice;
    @FXML private TableColumn<DisplayItem, String>        colAuctionId;
    @FXML private TableColumn<DisplayItem, AuctionStatus> colStatus;

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════

    private final ObservableList<DisplayItem> postedList = FXCollections.observableArrayList();

    private Seller currentSeller;
    private String username = "";
    private String role     = "";

    private AuctionViewStrategy strategy;

    private Stage stage;
    private Scene previousScene;
    private boolean historyLoaded;
    private boolean historyPending;

    // ══════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        resolveSellerFromSession();

        strategy = AuctionStrategyFactory.forRole(role);
        if (userNameLabel    != null) userNameLabel.setText(username);
        NotificationStore.getInstance().bindBadge(notificationBadge);
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);

        // ── TableView columns ────────────────────────────────────
        colName         .setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory     .setCellValueFactory(new PropertyValueFactory<>("category"));
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startPriceLong"));
        colStatus       .setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colAuctionId != null) {
            colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        }

        postedItemsTable.setItems(postedList);

        postedItemsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                DisplayItem selected = postedItemsTable.getSelectionModel().getSelectedItem();
                if (selected != null) handleStatusAction(selected);
            }
        });

        loadHistoryIfNeeded();
    }

    // ══════════════════════════════════════════════════════
    //  DATA LOADING
    // ══════════════════════════════════════════════════════

    private void loadHistoryIfNeeded() {
        if (!historyLoaded && !historyPending) {
            historyPending = true;
            getMyItems();
        }
    }

    public void getMyItems() {
        resolveSellerFromSession();
        if (currentSeller == null) {
            showAlert(Alert.AlertType.WARNING, "Session", "Please login as a seller first.");
            return;
        }

        Request request = new Request.Builder()
                .action("GET_MY_ITEMS")
                .payload(new JsonObject())
                .build();
        historyPending = true;
        ServerMessageRouter.register("GET_MY_ITEMS",
                this::onGetMyItemSuccess,
                this::onGetMyItemFail);
        Client.getInstance().sendRequest(request);
    }

    public void onGetMyItemSuccess(JsonObject data) {
            historyLoaded = true;
            historyPending = false;

            if (!data.has("items") || data.get("items").isJsonNull()) {
                postedList.clear();
                return;
            }

            JsonArray itemsArray = data.getAsJsonArray("items");
            List<DisplayItem> tempItems = new ArrayList<>();

            for (JsonElement element : itemsArray) {
                JsonObject obj = element.getAsJsonObject();

                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                String category = obj.has("category") ? obj.get("category").getAsString() : "";
                long startingPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsLong() : 0L;
                String auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsString() : "";
                AuctionStatus status = parseStatus(obj.has("auctionStatus")
                        ? obj.get("auctionStatus").getAsString() : "OPEN");

                tempItems.add(new DisplayItem(name, category, startingPrice, auctionId, status));
            }

            postedList.setAll(tempItems);
    }

    public void onGetMyItemFail(String errorCode, String errorMessage) {
            historyPending = false;
            String msg = (errorMessage == null || errorMessage.isBlank())
                    ? "Failed to load items" : errorMessage;
            showAlert(Alert.AlertType.ERROR, errorCode, msg);
        }

    // ══════════════════════════════════════════════════════
    //  DOUBLE-CLICK HANDLER
    // ══════════════════════════════════════════════════════

    private void handleStatusAction(DisplayItem item) {
        if (item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.OPEN) {
            try {
                BiddingNavigation.open(postedItemsTable, item.getAuctionId(), "/viewfxml/SellerHistory.fxml");
            } catch (Exception e) {
                System.err.println("Cannot open bidding screen: " + e.getMessage());
            }
        } else {
            String desc = switch (item.getStatus()) {
                case FINISHED -> "Time's up, someone has won";
                case PAID     -> "The winner has paid";
                case CANCELED -> "Cancelled - no bid placed or admin cancelled";
                default       -> "Undetermined state";
            };
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Bidding status");
            alert.setHeaderText("Status: " + item.getStatus().name());
            alert.setContentText(desc);
            alert.showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)     { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e)  { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)         { onOpenAddPopup(e); }
    @FXML public void goToProfile(ActionEvent e)         { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)          { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)              { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }

    // Bidder nav (hiển thị khi role = BIDDER, không dùng với Seller nhưng phải có để FXML không lỗi)
    @FXML public void goToBiddingHistory(ActionEvent e)  { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)       { switchScene(e, "/viewfxml/Watchlist.fxml"); }

    // Admin nav
    @FXML public void goToUserManagement(ActionEvent e)  { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e) { switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)         { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e)  { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        switchScene(e, "/viewfxml/Login.fxml");
    }

    // ══════════════════════════════════════════════════════
    //  ADD ITEM POPUP
    // ══════════════════════════════════════════════════════

    @FXML
    private void onOpenAddPopup(ActionEvent event) {
        try {
            resolveSellerFromSession();
            if (currentSeller == null) {
                showAlert(Alert.AlertType.WARNING, "Session", "Please login as a seller first.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/viewfxml/AddItem.fxml"));
            Parent root = loader.load();
            AddItemController addCtrl = loader.getController();
            Stage targetStage = resolveStage(event);
            Scene currentScene = targetStage.getScene();
            addCtrl.setData(currentSeller, this, targetStage, currentScene);
            targetStage.getScene().setRoot(root);
            targetStage.setMaximized(true);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot open AddItem: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  SESSION
    // ══════════════════════════════════════════════════════

    private void resolveSellerFromSession() {
        if (currentSeller != null) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getUsername() == null ? "" : currentUser.getUsername();
            role     = currentUser.getRole()     == null ? "" : currentUser.getRole();
        }
        if (currentUser instanceof Seller seller) {
            currentSeller = seller;
        }
    }

    /** Giữ lại để tương thích với Navigable interface. */
    @Override
    public void setNavigationContext(Stage stage, Scene previousScene) {
        this.stage         = stage;
        this.previousScene = previousScene;
    }

    public void setData(Seller seller) {
        this.currentSeller = seller;
        resolveSellerFromSession();
    }

    /** @deprecated Dùng SessionManager thay thế. */
    @Deprecated
    public void setUserInfo(String username, String role) {
        resolveSellerFromSession();
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    /**
     * Switch scene — giống pattern trong ActiveAuctionsController.
     */
    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Page not found",
                        "The feature \"" + fxml + "\" has not been implemented.");
                return;
            }
            Parent root = loader.load();
            Stage targetStage = resolveStage(event);

            Object controller = loader.getController();
            if (controller instanceof Navigable navigable) {
                navigable.setNavigationContext(targetStage, targetStage.getScene());
            }
            targetStage.getScene().setRoot(root);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error navigating", ex.getMessage());
        }
    }

    /**
     * Lấy Stage từ event. Nếu source là Node → dùng trực tiếp;
     * nếu là MenuItem → fallback về postedItemsTable (giống ActiveAuctionsController).
     */
    private Stage resolveStage(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        // MenuItem fallback
        if (stage != null) return stage;
        return (Stage) postedItemsTable.getScene().getWindow();
    }

    private AuctionStatus parseStatus(String raw) {
        try { return AuctionStatus.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return AuctionStatus.OPEN; }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
