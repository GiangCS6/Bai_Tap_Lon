package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.TimeUtil;
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
import javafx.scene.control.TableCell;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidderHistoryController implements Navigable {

    // ═══════════════════════════════════════════════════
    //  INNER DTOs
    // ═══════════════════════════════════════════════════

    public static class ActiveBidDTO {
        private final String  auctionId;
        private final String  itemName;
        private final long    myLastBid;
        private final long    currentPrice;
        private final boolean isWinning;
        private final String  endTime;

        public ActiveBidDTO(String auctionId, String itemName, long myLastBid,
                           long currentPrice, boolean isWinning, String endTime) {
            this.auctionId    = auctionId;
            this.itemName     = itemName;
            this.myLastBid    = myLastBid;
            this.currentPrice = currentPrice;
            this.isWinning    = isWinning;
            this.endTime      = endTime;
        }

        public String  getAuctionId()  { return auctionId; }
        public String  getItemName()    { return itemName; }
        public long    getMyLastBid()   { return myLastBid; }
        public long    getCurrentPrice() { return currentPrice; }
        public boolean isWinning()      { return isWinning; }
        public String  getEndTime()     { return endTime; }
    }

    public static class WonItemDTO {
        private final String auctionId;
        private final String itemName;
        private final long   finalPrice;
        private final String auctionStatus;
        private final String endTime;

        public WonItemDTO(String auctionId, String itemName, long finalPrice,
                          String auctionStatus, String endTime) {
            this.auctionId     = auctionId;
            this.itemName      = itemName;
            this.finalPrice    = finalPrice;
            this.auctionStatus = auctionStatus;
            this.endTime       = endTime;
        }

        public String getAuctionId()     { return auctionId; }
        public String getItemName()      { return itemName; }
        public long   getFinalPrice()    { return finalPrice; }
        public String getAuctionStatus() { return auctionStatus; }
        public String getEndTime()       { return endTime; }
    }

    // ═══════════════════════════════════════════════════
    //  FXML BINDINGS — Navbar
    // ═══════════════════════════════════════════════════

    @FXML private HBox  adminNav;
    @FXML private HBox  sellerNav;
    @FXML private HBox  bidderNav;

    @FXML private Label  userNameLabel;
    @FXML private Label  notificationBadge;
    @FXML private Button walletBtn;
    @FXML private Button bellBtn;

    // ═══════════════════════════════════════════════════
    //  FXML BINDINGS — Content (Table 1: Active Bids)
    // ═══════════════════════════════════════════════════

    @FXML private TableView<ActiveBidDTO>        activeBidsTable;
    @FXML private TableColumn<ActiveBidDTO, String>  colActiveName;
    @FXML private TableColumn<ActiveBidDTO, Long>    colActiveMyBid;
    @FXML private TableColumn<ActiveBidDTO, Long>    colActiveCurrentPrice;
    @FXML private TableColumn<ActiveBidDTO, Boolean> colActiveWinning;
    @FXML private TableColumn<ActiveBidDTO, String>  colActiveTimeLeft;

    // ═══════════════════════════════════════════════════
    //  FXML BINDINGS — Content (Table 2: Won Items)
    // ═══════════════════════════════════════════════════

    @FXML private TableView<WonItemDTO>           wonItemsTable;
    @FXML private TableColumn<WonItemDTO, String> colWonName;
    @FXML private TableColumn<WonItemDTO, Long>   colWonFinalPrice;
    @FXML private TableColumn<WonItemDTO, String> colWonStatus;
    @FXML private TableColumn<WonItemDTO, String> colWonDate;

    // ═══════════════════════════════════════════════════
    //  STATE
    // ═══════════════════════════════════════════════════

    private final ObservableList<ActiveBidDTO> activeList = FXCollections.observableArrayList();
    private final ObservableList<WonItemDTO>   wonList    = FXCollections.observableArrayList();

    private Bidder currentBidder;
    private String username = "";
    private String role     = "";

    private AuctionViewStrategy strategy;

    private Stage stage;
    private Scene previousScene;

    // ═══════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════

    @FXML
    public void initialize() {
        resolveBidderFromSession();

        // ── Navbar ──────────────────────────────────────────
        strategy = AuctionStrategyFactory.forRole(role);
        if (userNameLabel     != null) userNameLabel.setText(username);
        NotificationStore.getInstance().bindBadge(notificationBadge);
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);

        // ── Table 1: Active Bids ───────────────────────────
        colActiveName        .setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colActiveMyBid       .setCellValueFactory(new PropertyValueFactory<>("myLastBid"));
        colActiveCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colActiveWinning     .setCellValueFactory(new PropertyValueFactory<>("winning"));
        colActiveWinning     .setCellFactory(col -> new TableCell<ActiveBidDTO, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item ? "Yes" : "No");
            }
        });
        colActiveTimeLeft    .setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // ── Table 2: Won Items ─────────────────────────────
        colWonName      .setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colWonFinalPrice.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        colWonStatus    .setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));
        colWonStatus    .setCellFactory(col -> new TableCell<WonItemDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "FINISHED" -> setText("Finished");
                        case "PAID"     -> setText("Paid");
                        default         -> setText(item);
                    }
                }
            }
        });
        colWonDate      .setCellValueFactory(new PropertyValueFactory<>("endTime"));

        activeBidsTable.setItems(activeList);
        wonItemsTable  .setItems(wonList);

        // Double-click → open bidding screen
        activeBidsTable.setRowFactory(tv -> {
            TableRow<ActiveBidDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    ActiveBidDTO item = row.getItem();
                    openBiddingScreen(item.getAuctionId());
                }
            });
            return row;
        });

        loadBidderHistory();
    }

    // ═══════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════

    public void loadBidderHistory() {
        resolveBidderFromSession();
        if (currentBidder == null) {
            showAlert(Alert.AlertType.WARNING, "Session", "Please login as a bidder first.");
            return;
        }

        // Request 1: Get active bid summaries
        Request req1 = new Request.Builder()
                .action("GET_ACTIVE_BID_SUMMARY")
                .payload(new JsonObject())
                .build();

        ServerMessageRouter.register("GET_ACTIVE_BID_SUMMARY",
                this::onGetActiveBidSummarySuccess,
                this::onGetActiveBidSummaryFail);
        Client.getInstance().sendRequest(req1);

        // Request 2: Get won items
        Request req2 = new Request.Builder()
                .action("GET_WON_ITEMS")
                .payload(new JsonObject())
                .build();

        ServerMessageRouter.register("GET_WON_ITEMS",
                this::onGetWonItemsSuccess,
                this::onGetWonItemsFail);
        Client.getInstance().sendRequest(req2);
    }

    public void onGetActiveBidSummarySuccess(JsonObject data) {
        List<ActiveBidDTO> list = new ArrayList<>();
        if (data.has("activeBids")) {
            for (JsonElement element : data.getAsJsonArray("activeBids")) {
                JsonObject obj = element.getAsJsonObject();
                ActiveBidDTO dto = new ActiveBidDTO(
                        obj.has("auctionId")  ? obj.get("auctionId").getAsString()  : "",
                        obj.has("itemName")   ? obj.get("itemName").getAsString()   : "",
                        obj.has("myLastBid")  ? obj.get("myLastBid").getAsLong()    : 0L,
                        obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0L,
                        obj.has("isWinning")  && obj.get("isWinning").getAsBoolean(),
                        obj.has("endTime")    ? formatTimeLeft(obj.get("endTime").getAsString()) : ""
                );
                list.add(dto);
            }
        }
        Platform.runLater(() -> activeList.setAll(list));
    }

    public void onGetActiveBidSummaryFail(String errorCode, String errorMessage) {
        String msg = (errorMessage == null || errorMessage.isBlank()) ? "No active bids" : errorMessage;
        showAlert(Alert.AlertType.INFORMATION, errorCode, msg);
    }

    public void onGetWonItemsSuccess(JsonObject data) {
        List<WonItemDTO> list = new ArrayList<>();
        if (data.has("wonItems")) {
            for (JsonElement element : data.getAsJsonArray("wonItems")) {
                JsonObject obj = element.getAsJsonObject();
                WonItemDTO dto = new WonItemDTO(
                        obj.has("auctionId")     ? obj.get("auctionId").getAsString()     : "",
                        obj.has("itemName")      ? obj.get("itemName").getAsString()      : "",
                        obj.has("finalPrice")    ? obj.get("finalPrice").getAsLong()      : 0L,
                        obj.has("auctionStatus") ? obj.get("auctionStatus").getAsString() : "",
                        obj.has("endTime")       ? formatDateTime(obj.get("endTime").getAsString()) : ""
                );
                list.add(dto);
            }
        }
        Platform.runLater(() -> wonList.setAll(list));
    }

    public void onGetWonItemsFail(String errorCode, String errorMessage) {
        String msg = (errorMessage == null || errorMessage.isBlank()) ? "No won items yet" : errorMessage;
        showAlert(Alert.AlertType.INFORMATION, errorCode, msg);
    }

    // ═══════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)     { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e)  { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)       { switchScene(e, "/viewfxml/Watchlist.fxml"); }
    @FXML public void goToProfile(ActionEvent e)         { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)          { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)              { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }

    // Seller nav
    @FXML public void goToSellerHistory(ActionEvent e)   { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)         { switchScene(e, "/viewfxml/AddItem.fxml"); }

    // Admin nav
    @FXML public void goToUserManagement(ActionEvent e)  { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e) { switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)         { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e) { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        switchScene(e, "/viewfxml/Login.fxml");
    }

    private void openBiddingScreen(String auctionId) {
        try {
            BiddingNavigation.open(activeBidsTable, auctionId, "/viewfxml/BidderHistory.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot open bidding screen: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    //  SESSION
    // ═══════════════════════════════════════════════════

    private void resolveBidderFromSession() {
        if (currentBidder != null) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getUsername() == null ? "" : currentUser.getUsername();
            role     = currentUser.getRole()     == null ? "" : currentUser.getRole();
        }
        if (currentUser instanceof Bidder bidder) {
            currentBidder = bidder;
        }
    }

    @Override
    public void setNavigationContext(Stage stage, Scene previousScene) {
        this.stage         = stage;
        this.previousScene = previousScene;
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Chưa có trang",
                        "Chức năng \"" + fxml + "\" chưa được tạo.");
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
            showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", ex.getMessage());
        }
    }

    private Stage resolveStage(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        if (stage != null) return stage;
        return (Stage) activeBidsTable.getScene().getWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String formatTimeLeft(String isoTime) {
        try {
            LocalDateTime end = TimeUtil.fromIso(isoTime);
            LocalDateTime now = TimeUtil.now();
            long minutes = java.time.Duration.between(now, end).toMinutes();
            if (minutes <= 0) return "Ended";
            if (minutes < 60) return minutes + "m";
            long hours = minutes / 60;
            long mins = minutes % 60;
            return hours + "h " + mins + "m";
        } catch (Exception e) {
            return isoTime;
        }
    }

    private String formatDateTime(String isoTime) {
        try {
            LocalDateTime t = TimeUtil.fromIso(isoTime);
            return TimeUtil.formatTime(t);
        } catch (Exception e) {
            return isoTime;
        }
    }
}
