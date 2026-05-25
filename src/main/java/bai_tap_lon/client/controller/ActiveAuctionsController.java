package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Màn hình "Active Auctions" (FXML: active-auctions.fxml).
 *
 * REFACTOR — Strategy Pattern + SessionManager:
 *   - Đọc User từ SessionManager ngay trong initialize() → không còn
 *     setUserInfo / setLoggedInUsername / setUserRole / Platform.runLater.
 *   - Strategy lo toàn bộ phần phụ-thuộc-role (navbar + card buttons).
 *   - Controller chỉ giữ: network call, action handlers, navigation.
 *
 * Pre-condition: SessionManager.getCurrentUser() phải != null trước khi
 * vào màn này. LoginController có trách nhiệm setCurrentUser() trước khi
 * load FXML. Nếu null → throw → fail-fast để dev nhận ra bug ngay.
 */
public class ActiveAuctionsController {

    // ══════════════════════════════════════════════════════
    // FXML BINDINGS
    // ══════════════════════════════════════════════════════

    @FXML
    private HBox adminNav;
    @FXML
    private HBox sellerNav;
    @FXML
    private HBox bidderNav;

    @FXML private Label userNameLabel;
    @FXML private Label notificationBadge;
    @FXML private javafx.scene.control.Button walletBtn;
    @FXML private javafx.scene.control.Button bellBtn;

    @FXML private FlowPane auctionContainer;

    // ══════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════

    private String username;
    private String role;
    private Consumer<JsonObject> onAuctionEndedHandler;

    /** Strategy theo role — set 1 lần ở initialize(), không bao giờ null sau đó. */
    private AuctionViewStrategy strategy;

    /** Bundle handler dùng chung cho strategy. */
    private final AuctionActionHandlers handlers = AuctionActionHandlers.builder()
            .onPlaceBid  (this::handlePlaceBid)
            .onCancel    (this::handleCancel)
            .onOpenDetail(this::openDetail)
            .build();

    // ══════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Đọc user — bắt buộc phải có (đã login mới vào được màn này)
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException(
                    "ActiveAuctionsController: SessionManager.getCurrentUser() == null. " +
                    "Đã vào màn mà chưa login? Kiểm tra LoginController.onLoginSuccess()");
        }

        this.username = user.getUsername();
        this.role     = user.getRole();
        this.strategy = AuctionStrategyFactory.forRole(role);

        // Apply UI ngay — initialize() đã chạy trên FX thread, không cần runLater
        if (userNameLabel != null) userNameLabel.setText(username);
        NotificationStore.getInstance().bindBadge(notificationBadge);
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);

        // xoa card khi auction het gio
        ServerMessageRouter.subscribePush("AUCTION_ENDED_ALL", this::onAuctionEnded);
        ServerMessageRouter.subscribePush("AUCTION_CANCELLED_ALL", this::onAuctionEnded);

        // realtime: seller post phien moi → server broadcast AUCTION_CREATED → render card ngay
        //ServerMessageRouter.subscribePush("AUCTION_CREATED", this::onAuctionCreated);
        // realtime: phien chuyen OPEN → RUNNING → re-render card de update status
        ServerMessageRouter.subscribePush("AUCTION_STARTED_ALL", this::onAuctionStarted);

        loadActiveAuctions();
    }

    /** Xóa Label placeholder "Hiện chưa có phiên..." để không kẹt khi card đầu tiên render. */
    private void clearEmptyPlaceholder() {
        if (auctionContainer == null) return;
        auctionContainer.getChildren().removeIf(node -> node instanceof Label);
    }


    // ══════════════════════════════════════════════════════
    // ROLE-BASED UI
    // ══════════════════════════════════════════════════════

    private void hideAllNavs() {
        setNavVisible(adminNav, false);
        setNavVisible(sellerNav, false);
        setNavVisible(bidderNav, false);
    }

    private void applyRoleBasedNav() {
        hideAllNavs();
        if (role == null)
            return;
        switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "admin" -> setNavVisible(adminNav, true);
            case "seller" -> setNavVisible(sellerNav, true);
            case "bidder" -> setNavVisible(bidderNav, true);
        }
    }

    private void setNavVisible(HBox nav, boolean show) {
        if (nav == null)
            return;
        nav.setVisible(show);
        nav.setManaged(show);
    }

    // ══════════════════════════════════════════════════════
    // LOAD DATA
    // ══════════════════════════════════════════════════════

    private void loadActiveAuctions() {
        JsonObject payload = new JsonObject();
        Request request = new Request.Builder()
                .action("GET_ACTIVE_AUCTIONS")
                .payload(payload)
                .build();
        // Register callback truoc khi sendRequest
        ServerMessageRouter.register("GET_ACTIVE_AUCTIONS",
                this::onGetRunningAuctionsSuccess,
                this::onGetRunningAuctionsFail);
        try {
            Client.getInstance().sendRequest(request);
        } catch (Exception e) {
            System.err.println("[ActiveAuctions] Không gửi được request: " + e.getMessage());
        }
    }

    /** Callback từ ServerMessageRouter sau khi nhận GET_ACTIVE_AUCTIONS. */
    public void onGetRunningAuctionsSuccess(JsonObject data) {
        //khong co platform.runlater nua vi serverMessageRouter.responeRoute da wrap callback bang Platform.runlater roi
        if (auctionContainer == null) return;
        auctionContainer.getChildren().clear();

        if (data == null || !data.has("auctions")) return;

        JsonArray auctions = data.getAsJsonArray("auctions");
        if (auctions.size() == 0) {
            Label empty = new Label("Hiện chưa có phiên đấu giá nào đang diễn ra.");
            auctionContainer.getChildren().add(empty);
            return;
        }
        for (JsonElement el : auctions) {
            if (!el.isJsonObject()) continue;
            renderCard(el.getAsJsonObject());
        }
    }
    public void onGetRunningAuctionsFail(String errorCode, String errorMessage) {
//        Platform.runLater(() -> {
            showAlert(Alert.AlertType.ERROR,
                    errorCode == null ? "Error" : errorCode,
                    (errorMessage == null || errorMessage.isBlank())
                            ? "Không tải được danh sách phiên đấu giá."
                            : errorMessage);
//        });
    }

    public void onGetRunningAuctionsFailure(String errorMessage) {
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                "Lỗi", "Không tải được danh sách phiên: " + errorMessage));
    }

    private void onAuctionEnded(JsonObject data) {
        // Payload lifecycle là auction summary → field là "id" (không phải "auctionId")
        String endedId = data.has("id") && !data.get("id").isJsonNull()
                ? data.get("id").getAsString() : "";
        if (endedId.isEmpty() || auctionContainer == null) return;
        auctionContainer.getChildren().removeIf(node -> {
            Object userData = node.getUserData();
            return endedId.equals(userData);
        });
    }

    /**
     * Push handler: server vừa thông báo có auction mới được POST.
     * Append card vào container (kiểm duplicate theo auctionId trên userData
     * để tránh trùng nếu push tới trước response của loadActiveAuctions).
     */
    private void onAuctionCreated(JsonObject data) {
        if (auctionContainer == null || data == null) return;
        String newId = data.has("id") && !data.get("id").isJsonNull()
                ? data.get("id").getAsString() : "";
        if (newId.isEmpty()) return;

        for (Node node : auctionContainer.getChildren()) {
            if (newId.equals(node.getUserData())) return; // đã có
        }
        clearEmptyPlaceholder();
        renderCard(data);
    }

    /**
     * Push handler: phiên đã chuyển OPEN → RUNNING.
     * Tìm card cũ theo auctionId, remove rồi render lại với data mới (status RUNNING).
     */
    private void onAuctionStarted(JsonObject data) {
        if (auctionContainer == null || data == null) return;
        String id = data.has("id") && !data.get("id").isJsonNull()
                ? data.get("id").getAsString() : "";
        if (id.isEmpty()) return;
        auctionContainer.getChildren().removeIf(node -> id.equals(node.getUserData()));
        clearEmptyPlaceholder();
        renderCard(data);
    }

    /** Nạp 1 card và để Strategy cấu hình các nút. */
    private void renderCard(JsonObject a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewfxml/auction-card.fxml"));
            VBox card = loader.load();

            AuctionCardController cardCtrl = loader.getController();
            cardCtrl.setData(a);

            String auctionId = a.has("id") && !a.get("id").isJsonNull()
                    ? a.get("id").getAsString() : "";
            String status = a.has("status") && !a.get("status").isJsonNull()
                    ? a.get("status").getAsString() : "";
            card.setUserData(auctionId);

            // ★ Strategy quyết định nút gì, gắn handler nào
            strategy.configureCard(cardCtrl, auctionId, status, handlers);

            auctionContainer.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("[ActiveAuctions] Không nạp được auction-card: " + e.getMessage());
        }

    }

    // ══════════════════════════════════════════════════════
    // ACTION HANDLERS — strategy gọi vào đây
    // ══════════════════════════════════════════════════════

    /** Nút "primary" của card (Place Bid / Cancel) tuỳ role */
    private void handlePrimaryAction(String auctionId) {
        String r = (role == null) ? "" : role.trim().toLowerCase(Locale.ROOT);
        switch (r) {
            case "admin" -> handleCancel(auctionId);
            case "bidder" -> handlePlaceBid(auctionId);
            default -> {
                /* seller: không có hành động primary */ }
        }
    }

    private void onOpenDetail(String auctionId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", auctionId);
        Request req = new Request.Builder()
                .action("GET_AUCTION_SUMMARY")
                .payload(payload)
                .build();
        try {
            Client.getInstance().sendRequest(req);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không thể mở chi tiết: " + ex.getMessage());
        }
    }

    private void handlePlaceBid(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Auction ID không hợp lệ.");
            return;
        }
        if (auctionContainer == null || auctionContainer.getScene() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có scene để điều hướng.");
            return;
        }
        autoWatch(auctionId);
        BiddingNavigation.open(auctionContainer, auctionId, "/viewfxml/active-auctions.fxml");
    }

    /** Gửi WATCH_AUCTION fire-and-forget — chỉ áp dụng cho bidder. */
    private void autoWatch(String auctionId) {
        if (role == null || !"BIDDER".equalsIgnoreCase(role)) return;
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        Request req = new Request.Builder()
                .action("WATCH_AUCTION")
                .payload(p)
                .build();
        ServerMessageRouter.register("WATCH_AUCTION", d -> {}, (c, m) -> {});
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
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", auctionId);
                payload.addProperty("reason", "Cancelled by admin");
                Request req = new Request.Builder()
                        .action("CANCEL_AUCTION")
                        .payload(payload)
                        .build();
                try {
                    Client.getInstance().sendRequest(req);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            "Không huỷ được: " + ex.getMessage());
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
            Stage stage = (Stage) auctionContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            ItemInfoController controller = loader.getController();
            if (controller != null) {
                controller.initData(auctionId, stage.getScene());
            }
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không mở được chi tiết: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)    { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e) { /* đang ở đây */ }
    @FXML public void goToProfile(ActionEvent e)        { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)         { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)             { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }

    // Bidder
    @FXML public void goToBiddingHistory(ActionEvent e) { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)      { switchScene(e, "/viewfxml/Watchlist.fxml"); }

    // Seller
    @FXML public void goToSellerHistory(ActionEvent e)  { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)        { switchScene(e, "/viewfxml/AddItem.fxml"); }

    // Admin
    @FXML public void goToUserManagement(ActionEvent e) { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e){ switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)        { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e) { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        switchScene(e, "/viewfxml/Login.fxml");
    }

    /**
     * Switch scene đơn giản — không còn reflection, không còn truyền user info.
     * Controller đích sẽ tự đọc User từ SessionManager trong initialize() của nó.
     */
    private void switchScene(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Chưa có trang",
                        "Chức năng \"" + fxml + "\" chưa được tạo.");
                return;
            }
            Parent root = loader.load();
            Stage stage = resolveStage(event);
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", ex.getMessage());
        }
    }

    /**
     * Lấy Stage từ event. Nếu source là Node thì dùng trực tiếp;
     * nếu source là MenuItem (không phải Node) thì fallback về scene
     * của auctionContainer — luôn được attach khi controller đang sống.
     */
    /*private Stage resolveStage(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return (Stage) auctionContainer.getScene().getWindow();
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
        return (Stage) auctionContainer.getScene().getWindow();  // or auctionContainer
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
