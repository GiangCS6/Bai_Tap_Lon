package bai_tap_lon.client.controller;

import bai_tap_lon.client.controller.AuctionActionHandlers;
import bai_tap_lon.client.controller.AuctionStrategyFactory;
import bai_tap_lon.client.controller.AuctionViewStrategy;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Màn hình "Auction List" (FXML: auction-list.fxml).
 *
 * REFACTOR — Strategy Pattern + SessionManager:
 *   - Đọc User từ SessionManager ngay trong initialize() → không còn
 *     setUserInfo / setLoggedInUsername / Platform.runLater.
 *   - Strategy lo navbar + action button của từng row.
 *
 * Pre-condition: SessionManager phải có currentUser trước khi vào màn này.
 */
public class AuctionListController {

    // ══════════════════════════════════════════════════════
    // ROW MODEL
    // ══════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════
    // FXML BINDINGS
    // ══════════════════════════════════════════════════════

    @FXML private HBox adminNav;
    @FXML private HBox sellerNav;
    @FXML private HBox bidderNav;

    @FXML private Label userNameLabel;
    @FXML private Label notificationBadge;
    @FXML private Button walletBtn;
    @FXML private Button bellBtn;

    @FXML private Button btnCatAll;
    @FXML private Button btnCatElectronics;
    @FXML private Button btnCatVehicles;
    @FXML private Button btnCatArts;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, Number> colOrder;
    @FXML private TableColumn<AuctionRow, String> colName;
    @FXML private TableColumn<AuctionRow, String> colCategory;
    @FXML private TableColumn<AuctionRow, String> colStatus;
    @FXML private TableColumn<AuctionRow, Number> colCurrentPrice;
    @FXML private TableColumn<AuctionRow, String> colStartTime;
    @FXML private TableColumn<AuctionRow, String> colEndTime;
    @FXML private TableColumn<AuctionRow, Void>   colAction;

    // ══════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════

    private String username;
    private String role;

    /** Strategy theo role — set 1 lần ở initialize(). */
    private AuctionViewStrategy strategy;

    private final AuctionActionHandlers handlers = AuctionActionHandlers.builder()
            .onPlaceBid  (this::handlePlaceBid)
            .onWatch     (this::handleWatch)
            .onCancel    (this::handleCancel)
            .onOpenDetail(this::openDetail)
            .build();

    private final ObservableList<AuctionRow> masterList = FXCollections.observableArrayList();
    private FilteredList<AuctionRow> filteredList;

    private String activeCategory = "ALL";

    // ══════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Đọc user từ SessionManager — bắt buộc đã login
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException(
                    "AuctionListController: SessionManager.getCurrentUser() == null. " +
                    "Đã vào màn mà chưa login? Kiểm tra LoginController.onLoginSuccess()");
        }

        this.username = user.getUsername();
        this.role     = user.getRole();
        this.strategy = AuctionStrategyFactory.forRole(role);

        // Apply UI ngay
        if (userNameLabel != null) userNameLabel.setText(username);
        NotificationStore.getInstance().bindBadge(notificationBadge);
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);

        // Admin không có ví → ẩn icon ví
        if (walletBtn != null && "ADMIN".equalsIgnoreCase(role)) {
            walletBtn.setVisible(false);
            walletBtn.setManaged(false);
        }

        setupStatusFilter();
        setupTableColumns();
        setupSearchListener();
        setupRowDoubleClick();

        filteredList = new FilteredList<>(masterList, r -> true);
        auctionTable.setItems(filteredList);

        // Realtime: server broadcast khi seller post item mới → append row
        ServerMessageRouter.subscribePush("AUCTION_CREATED_ALL", this::onAuctionCreated);
        // Realtime: phiên chuyển OPEN → RUNNING → update status row
        ServerMessageRouter.subscribePush("AUCTION_STARTED_ALL", this::onAuctionStarted);
        // Realtime: phiên kết thúc (hết giờ) hoặc admin cancel → update status row
        ServerMessageRouter.subscribePush("AUCTION_ENDED_ALL",     this::onAuctionFinalStatus);
        ServerMessageRouter.subscribePush("AUCTION_CANCELLED_ALL", this::onAuctionFinalStatus);

        loadAuctions();
    }

    /**
     * Phiên đã kết thúc / bị huỷ — update status row theo payload.
     * Dùng chung cho ENDED và CANCELLED vì payload đều là auction summary
     * với field "status" đã được server set đúng (FINISHED / CANCELED).
     * applyFilters() chạy lại để filter "Running" tự ẩn các phiên này.
     */
    private void onAuctionFinalStatus(JsonObject a) {
        if (a == null) return;
        String id = optString(a, "id", "");
        if (id.isEmpty()) return;
        String newStatus = optString(a, "status", "").toUpperCase(Locale.ROOT);
        if (newStatus.isEmpty()) return;

        for (AuctionRow r : masterList) {
            if (id.equals(r.getId())) {
                r.statusProperty().set(newStatus);
                r.currentPriceProperty().set(optLong(a, "currentPrice", r.getCurrentPrice()));
                break;
            }
        }
        applyFilters();
        // colAction là Void column — refresh thủ công để strategy.buildRowActions
        // chạy lại với status mới (vd: ẩn nút Place Bid)
        auctionTable.refresh();
    }

    /** Phiên đã chuyển sang RUNNING — update status property của row tương ứng. */
    private void onAuctionStarted(JsonObject a) {
        if (a == null) return;
        String id = optString(a, "id", "");
        if (id.isEmpty()) return;
        for (AuctionRow r : masterList) {
            if (id.equals(r.getId())) {
                r.statusProperty().set("RUNNING");
                break;
            }
        }
        applyFilters();
        // colAction là Void column — không bind theo property nên cần refresh thủ công
        // để strategy.buildRowActions chạy lại với status mới
        auctionTable.refresh();
    }

    /**
     * Push handler — server vừa thông báo có phiên mới được tạo.
     * Build AuctionRow từ summary JSON và append vào masterList.
     * FilteredList + TableView tự re-render, predicate hiện tại tự lọc theo
     * category/status/search.
     */
    private void onAuctionCreated(JsonObject a) {
        if (a == null) return;
        String id = optString(a, "id", "");
        if (id.isEmpty()) return;
        // Đề phòng duplicate (vd: race với loadAuctions ban đầu)
        for (AuctionRow existing : masterList) {
            if (id.equals(existing.getId())) return;
        }

        AuctionRow r = new AuctionRow();
        r.order       .set(masterList.size() + 1);
        r.id          .set(id);
        r.name        .set(optString(a, "itemName", ""));
        r.category    .set(optString(a, "category", ""));
        r.status      .set(optString(a, "status", "OPEN").toUpperCase(Locale.ROOT));
        r.currentPrice.set(optLong(a, "currentPrice", 0L));
        r.startTime   .set(optString(a, "startTime", ""));
        r.endTime     .set(optString(a, "endTime", ""));
        masterList.add(r);
        applyFilters();
    }

    /**
     * Double-click vào 1 row → mở màn Bidding. Status nào được phép do
     * Strategy quyết định:
     *   - Bidder/Admin: chỉ OPEN (preview phiên chưa bắt đầu)
     *   - Seller: OPEN + RUNNING (xem cả phiên đang chạy của mình)
     */
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
                if (!strategy.canOpenBiddingFromRow(status)) return;

                BiddingNavigation.open(auctionTable, item.getId(), "/viewfxml/auction-list.fxml");
            });
            return row;
        });
    }

    // ══════════════════════════════════════════════════════
    // SETUP TABLE / FILTERS
    // ══════════════════════════════════════════════════════

    private void setupStatusFilter() {
        if (statusFilter == null) return;
        statusFilter.setItems(FXCollections.observableArrayList(
                "All statuses", "Running", "Open (upcoming)"));
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void setupTableColumns() {
        colOrder       .setCellValueFactory(new PropertyValueFactory<>("order"));
        colName        .setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory    .setCellValueFactory(new PropertyValueFactory<>("category"));
        colStatus      .setCellValueFactory(new PropertyValueFactory<>("status"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStartTime   .setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime     .setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Format giá
        colCurrentPrice.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : formatCurrency(v.longValue()));
            }
        });

        // ★ Action column delegate cho Strategy
        colAction.setCellFactory(buildActionCellFactory());
    }

    private void setupSearchListener() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    /**
     * Cell factory gọn — strategy đã set trong initialize() nên không cần guard null.
     */
    private Callback<TableColumn<AuctionRow, Void>, TableCell<AuctionRow, Void>> buildActionCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                AuctionRow row = getTableRow().getItem();
                String status = row.getStatus() == null ? "" : row.getStatus().toUpperCase(Locale.ROOT);

                Node actions = strategy.buildRowActions(row.getId(), status, handlers);
                setGraphic(actions);
            }
        };
    }

    // ══════════════════════════════════════════════════════
    // CATEGORY TOGGLE
    // ══════════════════════════════════════════════════════

    @FXML private void onCategoryAll(ActionEvent e)         { setActiveCategory("ALL",         btnCatAll); }
    @FXML private void onCategoryElectronics(ActionEvent e) { setActiveCategory("ELECTRONICS", btnCatElectronics); }
    @FXML private void onCategoryVehicles(ActionEvent e)    { setActiveCategory("VEHICLE",     btnCatVehicles); }
    @FXML private void onCategoryArts(ActionEvent e)        { setActiveCategory("ART",         btnCatArts); }

    private void setActiveCategory(String category, Button activeBtn) {
        activeCategory = category;
        Button[] all = { btnCatAll, btnCatElectronics, btnCatVehicles, btnCatArts };
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("active-toggle");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("active-toggle")) {
            activeBtn.getStyleClass().add("active-toggle");
        }
        applyFilters();
    }

    private void applyFilters() {
        if (filteredList == null) return;
        String q = searchField == null ? "" : searchField.getText();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String statusSel = statusFilter == null ? "All statuses" : statusFilter.getValue();

        filteredList.setPredicate(r -> {
            if (r == null) return false;

            if (!"ALL".equals(activeCategory)) {
                String cat = r.getCategory() == null ? "" : r.getCategory().toUpperCase(Locale.ROOT);
                if (!cat.equals(activeCategory)) return false;
            }

            String st = r.getStatus() == null ? "" : r.getStatus().toUpperCase(Locale.ROOT);
            if ("Running".equals(statusSel) && !"RUNNING".equals(st)) return false;
            if ("Open (upcoming)".equals(statusSel) && !"OPEN".equals(st)) return false;

            if (!needle.isEmpty()) {
                String n = r.getName() == null ? "" : r.getName().toLowerCase(Locale.ROOT);
                if (!n.contains(needle)) return false;
            }
            return true;
        });
    }

    // ══════════════════════════════════════════════════════
    // LOAD DATA
    // ══════════════════════════════════════════════════════

    private void loadAuctions() {
        JsonObject payload = new JsonObject();
        Request request = new Request.Builder()
                .action("GET_OPEN_AND_RUNNING_AUCTIONS")
                .payload(payload)
                .build();

        // Register callback TRƯỚC khi sendRequest — router đã wrap Platform.runLater
        ServerMessageRouter.register("GET_OPEN_AND_RUNNING_AUCTIONS",
                this::onGetAllAuctionsSuccess,
                this::onGetAllAuctionsFail);
        try {
            Client.getInstance().sendRequest(request);
        } catch (Exception e) {
            System.err.println("[AuctionList] Không gửi được request: " + e.getMessage());
        }
    }

    public void onGetAllAuctionsSuccess(JsonObject data) {
        masterList.clear();
        if (data == null || !data.has("auctions")) {
            applyFilters();
            return;
        }

        JsonArray arr = data.getAsJsonArray("auctions");
        int idx = 1;
        for (JsonElement el : arr) {
            JsonObject a = el.getAsJsonObject();
            String status = optString(a, "status", "");
            if (!"RUNNING".equalsIgnoreCase(status) && !"OPEN".equalsIgnoreCase(status)) continue;

            AuctionRow r = new AuctionRow();
            r.order       .set(idx++);
            r.id          .set(optString(a, "id", ""));
            r.name        .set(optString(a, "itemName", ""));
            r.category    .set(optString(a, "category", ""));
            r.status      .set(status.toUpperCase(Locale.ROOT));
            r.currentPrice.set(optLong(a, "currentPrice", 0L));
            r.startTime   .set(formatIsoTime(optString(a, "startTime", "")));
            r.endTime     .set(formatIsoTime(optString(a, "endTime", "")));
            masterList.add(r);
        }
        applyFilters();
    }

    private static String formatIsoTime(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            java.time.LocalDateTime t = TimeUtil.fromIso(iso);
            return t != null ? TimeUtil.formatTime(t) : "";
        } catch (Exception e) {
            return iso;
        }
    }

    private void onGetAllAuctionsFail(String errorCode, String errorMessage) {
        System.err.println("[AuctionList] GET_OPEN_AND_RUNNING_AUCTIONS lỗi: " + errorCode + " — " + errorMessage);
        showAlert(Alert.AlertType.ERROR, "Lỗi tải danh sách",
                errorMessage == null ? errorCode : errorMessage);
    }

    // ══════════════════════════════════════════════════════
    // ACTION HANDLERS — strategy gọi vào đây
    // ══════════════════════════════════════════════════════

    private void handlePlaceBid(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Auction ID không hợp lệ.");
            return;
        }
        autoWatch(auctionId);
        BiddingNavigation.open(auctionTable, auctionId, "/viewfxml/auction-list.fxml");
    }

    private void handleWatch(String auctionId) {
        autoWatch(auctionId);
        showAlert(Alert.AlertType.INFORMATION, "Watchlist", "Đã thêm vào danh sách theo dõi.");
    }

    /** Gửi WATCH_AUCTION fire-and-forget — không chặn UI nếu lỗi. */
    private void autoWatch(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) return;
        if (role == null || !"BIDDER".equalsIgnoreCase(role)) return;
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        Request req = new Request.Builder()
                .action("SAVE_WATCH_AUCTION")
                .payload(p)
                .build();
        ServerMessageRouter.register("SAVE_WATCH_AUCTION", d -> {}, (c, m) -> {});
        try {
            Client.getInstance().sendRequest(req);
        } catch (Exception ignore) {
        }
    }

    private void handleCancel(String auctionId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn huỷ phiên đấu giá này?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                JsonObject p = new JsonObject();
                p.addProperty("auctionId", auctionId);
                p.addProperty("reason", "Cancelled by admin");
                Request req = new Request.Builder()
                        .action("CANCEL_AUCTION")
                        .payload(p)
                        .build();
                try {
                    Client.getInstance().sendRequest(req);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không huỷ được: " + e.getMessage());
                }
            }
        });
    }

    private void openDetail(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Auction ID không hợp lệ.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewfxml/ItemInfo.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            ItemInfoController controller = loader.getController();
            if (controller != null) {
                controller.initData(auctionId, stage.getScene());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không mở được chi tiết: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)    { /* đang ở đây */ }
    @FXML public void goToActiveAuctions(ActionEvent e) { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToProfile(ActionEvent e)        { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)         { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)             { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }

    @FXML public void goToBiddingHistory(ActionEvent e) { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)      { switchScene(e, "/viewfxml/Watchlist.fxml"); }

    @FXML public void goToSellerHistory(ActionEvent e)  { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)        { switchScene(e, "/viewfxml/AddItem.fxml"); }

    @FXML public void goToUserManagement(ActionEvent e) { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e){ switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)        { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e) { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        switchScene(e, "/viewfxml/Login.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Chưa có trang",
                        "Không tìm thấy trang: " + fxml);
                return;
            }
            Parent root = loader.load();
            Stage stage = resolveStage(event);
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", ex.getMessage());
        }
    }

    /*private Stage resolveStage(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return (Stage) auctionTable.getScene().getWindow();
    }*/
    private Stage resolveStage(ActionEvent event) {
        if (event != null) {
            Object src = event.getSource();
            if (src instanceof Node node && node.getScene() != null) {
                return (Stage) node.getScene().getWindow();
            }
            // Handle MenuItem (common in navbar)
            if (src instanceof javafx.scene.control.MenuItem menuItem) {
                var popup = menuItem.getParentPopup();
                if (popup != null && popup.getOwnerWindow() instanceof Stage s) {
                    return s;
                }
            }
        }
        // Fallback
        return (Stage) auctionTable.getScene().getWindow();  // or auctionContainer
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    private static String formatCurrency(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }

    private static String optString(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }

    private static long optLong(JsonObject o, String key, long def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsLong() : def;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ══════════════════════════════════════════════════════
// SEARCH / REFRESH BUTTONS
// ══════════════════════════════════════════════════════

    @FXML
    private void onSearch(ActionEvent e) {
        applyFilters();
    }

    @FXML
    private void onRefresh(ActionEvent e) {
        // Clear search + reset filter rồi reload từ server
        if (searchField != null) searchField.clear();
        if (statusFilter != null) statusFilter.getSelectionModel().selectFirst();
        setActiveCategory("ALL", btnCatAll);
        loadAuctions();
    }
}
