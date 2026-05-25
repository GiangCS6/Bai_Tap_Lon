package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.HasBalance;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ProfileController {

    @FXML private HBox adminNav;
    @FXML private HBox sellerNav;
    @FXML private HBox bidderNav;

    @FXML private Label notificationBadge;
    @FXML private Label userNameLabel;
    @FXML private Label userIdValue;
    @FXML private Label usernameValue;
    @FXML private Label emailValue;
    @FXML private Label roleLabel;
    @FXML private Label walletBalanceLabel;
    @FXML private Label createdAtValue;

    // ── Profile Header Card ──
    @FXML private Label fullNameLabel;
    @FXML private Label headerRoleLabel;
    @FXML private Label avatarInitialLabel;
    @FXML private Label statusLabel;

    // ── Stat Cards ──
    @FXML private Label balanceStatLabel;
    @FXML private Label joinedAuctionCountLabel;
    @FXML private Label activeAuctionCountLabel;
    @FXML private Button bellBtn;
    @FXML private Button walletBtn;

    private String username = "";
    private String role = "";
    private User currentUser;

    @FXML
    public void initialize() {
        // BUG FIX #8: Không hardcode "0" — để trống hoặc "..." cho đến khi server trả về
        joinedAuctionCountLabel.setText("...");
        activeAuctionCountLabel.setText("...");
        NotificationStore.getInstance().bindBadge(notificationBadge);
        applyCurrentSessionState();
        applyRoleNavbar();
        requestAuctionStats();
    }

    /** Áp navbar theo role qua Strategy (cùng Strategy với ActiveAuctions/AuctionList). */
    private void applyRoleNavbar() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getRole() == null || user.getRole().isBlank()) {
            return;
        }
        AuctionViewStrategy strategy = AuctionStrategyFactory.forRole(user.getRole());
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);
    }

    public void setLoginData(JsonObject data) {
        if (data == null) return;

        JsonObject user = (data.has("user") && data.get("user").isJsonObject())
                ? data.getAsJsonObject("user")
                : null;
        if (user != null) {
            applyUserData(user);
        }

        if (data.has("balance") && !data.get("balance").isJsonNull()) {
            updateBalanceLabels(data.get("balance").getAsLong());
        } else {
            refreshBalanceByRole();
        }
    }

    // Backward compatibility for old flow passing only "user" object
    public void setUserData(JsonObject user) {
        if (user == null) return;
        JsonObject data = new JsonObject();
        data.add("user", user);
        if (user.has("balance") && !user.get("balance").isJsonNull()) {
            data.addProperty("balance", user.get("balance").getAsLong());
        }
        setLoginData(data);
    }

    private void applyUserData(JsonObject user) {
        if (user == null) return;

        Platform.runLater(() -> {
            if (user.has("id"))
                userIdValue.setText(user.get("id").getAsString());

            if (user.has("email"))
                emailValue.setText(user.get("email").getAsString());

            if (user.has("createdAt")) {
                String iso = user.get("createdAt").getAsString();
                java.time.LocalDateTime t = TimeUtil.fromIso(iso);
                createdAtValue.setText(t != null ? TimeUtil.formatTime(t) : "");
            }

            if (user.has("role")) {
                this.role = user.get("role").getAsString();
                roleLabel.setText(this.role);
                headerRoleLabel.setText(this.role);
            }

            if (user.has("username")) {
                this.username = user.get("username").getAsString();
                usernameValue.setText(this.username);
                fullNameLabel.setText(this.username);
                if (userNameLabel != null) {
                    userNameLabel.setText(this.username);
                }
                if (!this.username.isEmpty()) {
                    avatarInitialLabel.setText(this.username.substring(0, 1).toUpperCase());
                }
            }

            if (user.has("isActive")) {
                boolean active = user.get("isActive").getAsBoolean();
                statusLabel.setText(active ? "Active" : "Inactive");
            }
        });
    }

    private void syncFromSession() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        this.username = currentUser.getUsername() == null ? "" : currentUser.getUsername();
        this.role = currentUser.getRole() == null ? "" : currentUser.getRole();
    }

    private void applyCurrentSessionState() {
        syncFromSession();
        if (currentUser != null) {
            applySessionUserLabels();
        } else {
            applyBasicUserLabels();
        }
    }

    private void applySessionUserLabels() {
        if (currentUser == null) {
            return;
        }

        Platform.runLater(() -> {
            if (userNameLabel != null) userNameLabel.setText(username.isEmpty() ? "Guest" : username);
            if (usernameValue != null && !username.isEmpty()) usernameValue.setText(username);
            if (fullNameLabel != null && !username.isEmpty()) fullNameLabel.setText(username);
            if (avatarInitialLabel != null && !username.isEmpty()) {
                avatarInitialLabel.setText(username.substring(0, 1).toUpperCase());
            }
            if (roleLabel != null && !role.isEmpty()) roleLabel.setText(role);
            if (headerRoleLabel != null && !role.isEmpty()) headerRoleLabel.setText(role);

            if (userIdValue != null) userIdValue.setText(currentUser.getId() == null ? "" : currentUser.getId());
            if (emailValue != null) emailValue.setText(currentUser.getEmail() == null ? "" : currentUser.getEmail());
            if (createdAtValue != null && currentUser.getCreatedAt() != null) {
                createdAtValue.setText(TimeUtil.formatTime(currentUser.getCreatedAt()));
            }
            if (statusLabel != null) {
                statusLabel.setText(currentUser.isActive() ? "Active" : "Inactive");
            }

            if (currentUser instanceof HasBalance hasBalance) {
                updateBalanceLabels(hasBalance.getBalance());
            }
        });
    }

    private void applyBasicUserLabels() {
        Platform.runLater(() -> {
            if (userNameLabel != null) userNameLabel.setText(this.username.isEmpty() ? "Guest" : this.username);
            if (usernameValue != null && !this.username.isEmpty()) usernameValue.setText(this.username);
            if (fullNameLabel != null && !this.username.isEmpty()) fullNameLabel.setText(this.username);
            if (avatarInitialLabel != null && !this.username.isEmpty()) {
                avatarInitialLabel.setText(this.username.substring(0, 1).toUpperCase());
            }
            if (roleLabel != null && !this.role.isEmpty()) roleLabel.setText(this.role);
            if (headerRoleLabel != null && !this.role.isEmpty()) headerRoleLabel.setText(this.role);
        });
    }

    private void requestLatestBalance() {
        Request request = new Request.Builder()
                .action("GET_BALANCE")
                .payload(new JsonObject())
                .build();

        ServerMessageRouter.register(
                "GET_BALANCE",
                this::onGetBalanceSuccess,
                this::onGetBalanceFail
        );
        Client.getInstance().sendRequest(request);
    }

    private void requestAuctionStats() {
        syncFromSession();
        if (currentUser == null) return;

        Request request = new Request.Builder()
                .action("GET_USER_STATS")
                .payload(new JsonObject())
                .build();

        ServerMessageRouter.register(
                "GET_USER_STATS",
                this::onGetUserStatsSuccess,
                (code, msg) -> Platform.runLater(() -> {
                    joinedAuctionCountLabel.setText("N/A");
                    activeAuctionCountLabel.setText("N/A");
                })
        );
        Client.getInstance().sendRequest(request);
    }

    private void onGetUserStatsSuccess(JsonObject data) {
        if (data == null) return;
            if (data.has("joinedCount") && !data.get("joinedCount").isJsonNull()) {
                joinedAuctionCountLabel.setText(String.valueOf(data.get("joinedCount").getAsInt()));
            } else {
                joinedAuctionCountLabel.setText("0");
            }
            if (data.has("activeCount") && !data.get("activeCount").isJsonNull()) {
                activeAuctionCountLabel.setText(String.valueOf(data.get("activeCount").getAsInt()));
            } else {
                activeAuctionCountLabel.setText("0");
            }
    }

    private void refreshBalanceByRole() {
        if (currentUser instanceof HasBalance) {
            requestLatestBalance();
        } else {
            setBalanceUnavailable();
        }
    }

    private void onGetBalanceSuccess(JsonObject data) {
        if (data == null || !data.has("balance") || data.get("balance").isJsonNull()) return;
        updateBalanceLabels(data.get("balance").getAsLong());
    }

    private void onGetBalanceFail(String errorCode, String errorMessage) {
        if ("UNAUTHORIZED".equalsIgnoreCase(errorCode)) {
            setBalanceUnavailable();
            return;
        }
        Platform.runLater(() -> {
            String message = (errorMessage == null || errorMessage.isBlank()) ? "Cannot load balance" : errorMessage;
            showAlert(Alert.AlertType.WARNING, errorCode, message);
        });
    }

    private void updateBalanceLabels(long balance) {
        Platform.runLater(() -> {
            String formatted = String.format("%,d VNĐ", balance);
            walletBalanceLabel.setText(formatted);
            balanceStatLabel.setText(formatted);
        });
    }

    private void setBalanceUnavailable() {
        Platform.runLater(() -> {
            if (walletBalanceLabel != null) walletBalanceLabel.setText("N/A");
            if (balanceStatLabel != null) balanceStatLabel.setText("N/A");
        });
    }

    @FXML public void goToProfile(ActionEvent e) { /* current page */ }
    @FXML public void goToAuctionList(ActionEvent e) { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e) { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e) { switchScene(e, "/viewfxml/Watchlist.fxml"); }
    @FXML public void goToBiddingHistory(ActionEvent e) { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToSellerHistory(ActionEvent e) { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e) { switchScene(e, "/viewfxml/AddItem.fxml"); }
    @FXML public void goToSalesHistory(ActionEvent e) { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    // Admin
    @FXML public void goToUserManagement(ActionEvent e)  { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e) { switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)         { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e)  { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML public void onLogout(ActionEvent e) {
        SessionManager.getInstance().clear();
        currentUser = null;
        username = "";
        role = "";
        switchScene(e, "/viewfxml/Login.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        if (loader.getLocation() == null) {
            showAlert(Alert.AlertType.WARNING, "Not Found", "Not found scene: " + fxml);
            return;
        }
        Parent root = loader.load();
        Scene previousScene = ((Node) event.getSource()).getScene();
        Stage stage = (Stage) previousScene.getWindow();

        Object ctrl = loader.getController();
        if (ctrl != null) {
            if (ctrl instanceof Navigable navigable) {
                navigable.setNavigationContext(stage, previousScene);
            }
            // Giữ nguyên phần initData vì không có interface cho nó
            try {
                String auctionId = null;
                Object src = event.getSource();
                if (src instanceof Node node && node.getUserData() != null) {
                    auctionId = node.getUserData().toString();
                }
                ctrl.getClass()
                        .getMethod("initData", String.class, Scene.class)
                        .invoke(ctrl, auctionId, previousScene);
            } catch (NoSuchMethodException ignored) {
            }
        }

        stage.getScene().setRoot(root);
        stage.setMaximized(true);
    } catch (Exception ex) {
        showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", ex.getMessage());
    }
}

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML public void goToWallet(ActionEvent e) {
        WalletController.showFor(walletBtn);
    }

    @FXML public void onBell(ActionEvent e) {
        NotificationStore.getInstance().markAllAsRead();
        NotificationPopup.showFor(bellBtn);
    }


}
