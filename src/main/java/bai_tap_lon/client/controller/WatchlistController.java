package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Màn "Watchlist" — chỉ dành cho bidder.
 * Bảng hiển thị các auction bidder đã theo dõi (qua Care/Join/Place Bid).
 * Cột Action chỉ có nút Delete → gửi UNWATCH_AUCTION.
 */
public class WatchlistController {

    public static class AuctionRow {
        private final SimpleIntegerProperty order        = new SimpleIntegerProperty();
        private final SimpleStringProperty  id           = new SimpleStringProperty();
        private final SimpleStringProperty  name         = new SimpleStringProperty();
        private final SimpleStringProperty  category     = new SimpleStringProperty();
        private final SimpleStringProperty  status       = new SimpleStringProperty();
        private final SimpleLongProperty    currentPrice = new SimpleLongProperty();
        private final SimpleStringProperty  startTime    = new SimpleStringProperty();
        private final SimpleStringProperty  endTime      = new SimpleStringProperty();

        public int    getOrder()        { return order.get(); }
        public String getId()           { return id.get(); }
        public String getName()         { return name.get(); }
        public String getCategory()     { return category.get(); }
        public String getStatus()       { return status.get(); }
        public long   getCurrentPrice() { return currentPrice.get(); }
        public String getStartTime()    { return startTime.get(); }
        public String getEndTime()      { return endTime.get(); }

        public SimpleIntegerProperty orderProperty()        { return order; }
        public SimpleStringProperty  nameProperty()         { return name; }
        public SimpleStringProperty  categoryProperty()     { return category; }
        public SimpleStringProperty  statusProperty()       { return status; }
        public SimpleLongProperty    currentPriceProperty() { return currentPrice; }
        public SimpleStringProperty  startTimeProperty()    { return startTime; }
        public SimpleStringProperty  endTimeProperty()      { return endTime; }
    }

    @FXML private HBox adminNav;
    @FXML private HBox sellerNav;
    @FXML private HBox bidderNav;

    @FXML private Label userNameLabel;
    @FXML private Label notificationBadge;
    @FXML private Button walletBtn;
    @FXML private Button bellBtn;

    @FXML private TextField searchField;
    @FXML private Button btnRefresh;

    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, Number> colOrder;
    @FXML private TableColumn<AuctionRow, String> colName;
    @FXML private TableColumn<AuctionRow, String> colCategory;
    @FXML private TableColumn<AuctionRow, String> colStatus;
    @FXML private TableColumn<AuctionRow, Number> colCurrentPrice;
    @FXML private TableColumn<AuctionRow, String> colStartTime;
    @FXML private TableColumn<AuctionRow, String> colEndTime;
    @FXML private TableColumn<AuctionRow, Void>   colAction;

    private final ObservableList<AuctionRow> masterList = FXCollections.observableArrayList();
    private FilteredList<AuctionRow> filteredList;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException(
                    "WatchlistController: SessionManager.getCurrentUser() == null.");
        }
        if (!"BIDDER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException(
                    "Watchlist chỉ dành cho bidder, role hiện tại: " + user.getRole());
        }

        if (userNameLabel != null) userNameLabel.setText(user.getUsername());
        NotificationStore.getInstance().bindBadge(notificationBadge);

        // Bidder thì luôn show bidderNav (dùng strategy cho consistent với các màn khác)
        AuctionStrategyFactory.forRole(user.getRole())
                .applyNavbar(adminNav, sellerNav, bidderNav);

        setupTableColumns();
        setupSearchListener();
        setupRowDoubleClick();

        filteredList = new FilteredList<>(masterList, r -> true);
        auctionTable.setItems(filteredList);

        loadWatchlist();
    }

    private void setupRowDoubleClick() {
        if (auctionTable == null) return;
        auctionTable.setRowFactory(tv -> {
            TableRow<AuctionRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) return;
                if (event.getClickCount() != 2) return;
                if (row.isEmpty() || row.getItem() == null) return;

                AuctionRow item = row.getItem();
                String status = item.getStatus() == null ? "" : item.getStatus().toUpperCase(Locale.ROOT);
                if (!"OPEN".equals(status) && !"RUNNING".equals(status)) return;

                BiddingNavigation.open(auctionTable, item.getId(), "/viewfxml/Watchlist.fxml");
            });
            return row;
        });
    }

    private void setupTableColumns() {
        colOrder       .setCellValueFactory(new PropertyValueFactory<>("order"));
        colName        .setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory    .setCellValueFactory(new PropertyValueFactory<>("category"));
        colStatus      .setCellValueFactory(new PropertyValueFactory<>("status"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStartTime   .setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime     .setCellValueFactory(new PropertyValueFactory<>("endTime"));

        colCurrentPrice.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : formatCurrency(v.longValue()));
            }
        });

        colAction.setCellFactory(buildDeleteCellFactory());
    }

    private Callback<TableColumn<AuctionRow, Void>, TableCell<AuctionRow, Void>> buildDeleteCellFactory() {
        return col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                deleteBtn.getStyleClass().add("btn-primary");
                deleteBtn.setOnAction(e -> {
                    AuctionRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) handleDelete(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        };
    }

    private void setupSearchListener() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        if (filteredList == null) return;
        String q = searchField == null ? "" : searchField.getText();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        filteredList.setPredicate(r -> {
            if (r == null) return false;
            if (needle.isEmpty()) return true;
            String n = r.getName() == null ? "" : r.getName().toLowerCase(Locale.ROOT);
            return n.contains(needle);
        });
    }

    // ══════════════════════════════════════════════════════
    // LOAD / DELETE
    // ══════════════════════════════════════════════════════

    private void loadWatchlist() {
        Request req = new Request.Builder()
                .action("GET_WATCHLIST")
                .payload(new JsonObject())
                .build();
        ServerMessageRouter.register("GET_WATCHLIST",
                this::onGetWatchlistSuccess,
                this::onGetWatchlistFail);
        try {
            Client.getInstance().sendRequest(req);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không gửi được request: " + e.getMessage());
        }
    }

    private void onGetWatchlistSuccess(JsonObject data) {
        masterList.clear();
        if (data == null || !data.has("auctions")) {
            applyFilters();
            return;
        }
        JsonArray arr = data.getAsJsonArray("auctions");
        int idx = 1;
        for (JsonElement el : arr) {
            JsonObject a = el.getAsJsonObject();
            AuctionRow r = new AuctionRow();
            r.order       .set(idx++);
            r.id          .set(optString(a, "id", ""));
            r.name        .set(optString(a, "itemName", ""));
            r.category    .set(optString(a, "category", ""));
            r.status      .set(optString(a, "status", "").toUpperCase(Locale.ROOT));
            r.currentPrice.set(optLong(a, "currentPrice", 0L));
            r.startTime   .set(formatIso(optString(a, "startTime", "")));
            r.endTime     .set(formatIso(optString(a, "endTime", "")));
            masterList.add(r);
        }
        applyFilters();
    }

    private void onGetWatchlistFail(String code, String msg) {
        showAlert(Alert.AlertType.ERROR, "Lỗi tải watchlist",
                msg == null || msg.isBlank() ? code : msg);
    }

    private void handleDelete(AuctionRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoá \"" + row.getName() + "\" khỏi watchlist?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;

            JsonObject p = new JsonObject();
            p.addProperty("auctionId", row.getId());
            Request req = new Request.Builder()
                    .action("DELETE_WATCH_AUCTION")
                    .payload(p)
                    .build();
            ServerMessageRouter.register("DELETE_WATCH_AUCTION",
                    data -> {
                        masterList.remove(row);
                        reindexOrder();
                        applyFilters();
                    },
                    (code, m) -> showAlert(Alert.AlertType.ERROR, "Lỗi xoá",
                            m == null || m.isBlank() ? code : m));
            try {
                Client.getInstance().sendRequest(req);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không gửi được request: " + ex.getMessage());
            }
        });
    }

    private void reindexOrder() {
        int i = 1;
        for (AuctionRow r : masterList) r.orderProperty().set(i++);
    }

    // ══════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)    { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e) { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToProfile(ActionEvent e)        { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)         { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)             { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }
    @FXML public void goToBiddingHistory(ActionEvent e) { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)      { /* đang ở đây */ }

    // Seller / Admin nav handlers — không hiện trong UI nhưng fxml reference nên cần
    @FXML public void goToSellerHistory(ActionEvent e)  { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)        { switchScene(e, "/viewfxml/AddItem.fxml"); }
    @FXML public void goToUserManagement(ActionEvent e) { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToReports(ActionEvent e)        { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e) { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        switchScene(e, "/viewfxml/Login.fxml");
    }

    @FXML
    public void onRefresh(ActionEvent e) {
        if (searchField != null) searchField.clear();
        loadWatchlist();
    }

    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Chưa có trang", "Không tìm thấy: " + fxml);
                return;
            }
            Parent root = loader.load();
            Stage stage = resolveStage(event);
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", ex.getMessage());
        }
    }

    private Stage resolveStage(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return (Stage) auctionTable.getScene().getWindow();
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    private static String formatIso(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            LocalDateTime t = TimeUtil.fromIso(iso);
            return t != null ? TimeUtil.formatTime(t) : "";
        } catch (Exception e) {
            return iso;
        }
    }

    private static String formatCurrency(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }

    private static String optString(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
    }

    private static long optLong(JsonObject o, String key, long def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsLong() : def;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
