package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.Locale;

public class ItemInfoController implements Navigable {

    // ══════════════════════════════════════════════════════
    //  FXML BINDINGS — Navbar
    // ══════════════════════════════════════════════════════

    @FXML private HBox  adminNav;
    @FXML private HBox  sellerNav;
    @FXML private HBox  bidderNav;
    @FXML private Label userNameLabel;
    @FXML private Label notificationBadge;
    @FXML private Button walletBtn;
    @FXML private Button bellBtn;

    // ══════════════════════════════════════════════════════
    //  FXML BINDINGS — Content
    // ══════════════════════════════════════════════════════

    @FXML private ImageView itemImageView;
    @FXML private Label    imagePlaceholderLabel;
    @FXML private Label    categoryLabel;
    @FXML private Label    statusLabel;
    @FXML private Label    itemNameLabel;
    @FXML private Label    priceLabel;
    @FXML private Label    sellerLabel;
    @FXML private VBox     attributesBox;
    @FXML private TextArea descriptionArea;

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════

    private String auctionId;
    private String username = "";
    private String role     = "";
    private Stage  stage;

    private AuctionViewStrategy strategy;

    // ══════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        setDefaultValues();

        syncFromSession();
        strategy = AuctionStrategyFactory.forRole(role);
        if (userNameLabel    != null) userNameLabel.setText(username.isEmpty() ? "Guest" : username);
        NotificationStore.getInstance().bindBadge(notificationBadge);
        strategy.applyNavbar(adminNav, sellerNav, bidderNav);
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════

    public void initData(String auctionId, Scene previousScene) {
        this.auctionId     = auctionId;
        //this.previousScene = previousScene;
        syncFromSession();
        loadAuctionDetail();
    }

    @Override
    public void setNavigationContext(Stage stage, Scene previousScene) {
        this.stage         = stage;
        //this.previousScene = previousScene;
    }

//    public void setPreviewInfo(String itemName, String category, long price) {
//        itemNameLabel.setText(itemName == null || itemName.isBlank() ? "Unknown item" : itemName);
//        categoryLabel.setText(category == null || category.isBlank() ? "UNKNOWN" : category.toUpperCase(Locale.ROOT));
//        priceLabel.setText("Current Price: " + formatMoney(price));
//    }

    // ══════════════════════════════════════════════════════
    //  LOAD DATA
    // ══════════════════════════════════════════════════════

    private void loadAuctionDetail() {
        if (auctionId == null || auctionId.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Missing auction id.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", auctionId);
        Request request = new Request.Builder()
                .action("GET_ITEM_DETAILS")
                .payload(payload)
                .build();

        ServerMessageRouter.register(
                "GET_ITEM_DETAILS",
                this::onGetItemDetailsSuccess,
                this::onGetItemDetailsFail
        );
        Client.getInstance().sendRequest(request);
    }

    private void onGetItemDetailsSuccess(JsonObject data) {
        if (data == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid item details response.");
            return;
        }

        String itemName = optString(data, "itemName", "Unknown item");
        String description = optString(data, "description", "");
        String imageUrl = optString(data, "imageUrl", "");
        String category = optString(data, "category", "N/A");
        String sellerName = optString(data, "sellerName", "Unknown");
        long startingPrice = optLong(data, "startingPrice", 0L);
        long currentPrice = optLong(data, "currentPrice", 0L);
        String auctionStatus = optString(data, "auctionStatus", "N/A");

        itemNameLabel.setText(itemName);
        sellerLabel.setText(sellerName);
        priceLabel.setText("Current Price: " + formatMoney(currentPrice));
        categoryLabel.setText(category.toUpperCase());
        statusLabel.setText(auctionStatus);

        descriptionArea.setText(description != null && !description.isBlank()
                ? description
                : "No item detail is available from server for this auction.");

        attributesBox.getChildren().clear();
        addAttributeRow("Starting Price", formatMoney(startingPrice));

        bindImage(imageUrl, itemName);
    }

    private void onGetItemDetailsFail(String errorCode, String errorMessage) {
        String message = (errorMessage == null || errorMessage.isBlank())
                ? "Cannot load item detail."
                : errorMessage;
        showAlert(Alert.AlertType.ERROR, errorCode == null ? "Error" : errorCode, message);
    }

    // ══════════════════════════════════════════════════════
    //  REGISTER / WATCHLIST
    // ══════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════
    //  NAVIGATION — Navbar actions
    // ══════════════════════════════════════════════════════

    @FXML public void goToAuctionList(ActionEvent e)     { switchScene(e, "/viewfxml/auction-list.fxml"); }
    @FXML public void goToActiveAuctions(ActionEvent e)  { switchScene(e, "/viewfxml/active-auctions.fxml"); }
    @FXML public void goToProfile(ActionEvent e)         { switchScene(e, "/viewfxml/profile.fxml"); }
    @FXML public void goToWallet(ActionEvent e)          { WalletController.showFor(walletBtn); }
    @FXML public void onBell(ActionEvent e)              { NotificationStore.getInstance().markAllAsRead(); NotificationPopup.showFor(bellBtn); }

    // Bidder
    @FXML public void goToBiddingHistory(ActionEvent e)  { switchScene(e, "/viewfxml/BidderHistory.fxml"); }
    @FXML public void goToWatchlist(ActionEvent e)       { switchScene(e, "/viewfxml/Watchlist.fxml"); }

    // Seller
    @FXML public void goToSellerHistory(ActionEvent e)   { switchScene(e, "/viewfxml/SellerHistory.fxml"); }
    @FXML public void goToAddItem(ActionEvent e)         { switchScene(e, "/viewfxml/AddItem.fxml"); }
    // Admin
    @FXML public void goToUserManagement(ActionEvent e)  { switchScene(e, "/viewfxml/UserManagement.fxml"); }
    @FXML public void goToAuctionApproval(ActionEvent e) { switchScene(e, "/viewfxml/AuctionApproval.fxml"); }
    @FXML public void goToReports(ActionEvent e)         { switchScene(e, "/viewfxml/Reports.fxml"); }
    @FXML public void goToSystemSettings(ActionEvent e)  { switchScene(e, "/viewfxml/SystemSettings.fxml"); }

    @FXML
    public void onLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        switchScene(event, "/viewfxml/Login.fxml");
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION — Helpers
    // ══════════════════════════════════════════════════════

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
            if (targetStage == null) return;

            Object ctrl = loader.getController();
            if (ctrl instanceof Navigable navigable) {
                navigable.setNavigationContext(targetStage, targetStage.getScene());
            }

            targetStage.getScene().setRoot(root);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Navigation error", ex.getMessage());
        }
    }

    private Stage resolveStage(ActionEvent event) {
        if (event != null) {
            Object src = event.getSource();

            // Trường hợp thông thường: Button, Label, etc.
            if (src instanceof javafx.scene.Node node && node.getScene() != null) {
                return (Stage) node.getScene().getWindow();
            }

            // ✅ Trường hợp MenuItem (không phải Node)
            if (src instanceof javafx.scene.control.MenuItem menuItem) {
                var popup = menuItem.getParentPopup();
                if (popup != null && popup.getOwnerWindow() instanceof Stage s) {
                    return s;
                }
            }
        }
        if (stage != null) return stage;
        return null;
    }

    // ══════════════════════════════════════════════════════
    //  RENDER HELPERS
    // ══════════════════════════════════════════════════════

    private void addAttributeRow(String key, String value) {
        HBox row = new HBox(14);
        Label keyLabel = new Label(key + ":");
        keyLabel.getStyleClass().add("meta-key");
        keyLabel.setMinWidth(140);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("meta-value");
        row.getChildren().addAll(keyLabel, valueLabel);
        attributesBox.getChildren().add(row);
    }

    private void bindImage(String imageUrl, String itemName) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, true);
                itemImageView.setImage(img);
                itemImageView.setPreserveRatio(true);
                imagePlaceholderLabel.setVisible(false);

                img.errorProperty().addListener((obs, was, isErr) -> {
                    if (Boolean.TRUE.equals(isErr)) {
                        itemImageView.setImage(null);
                        showImagePlaceholder(itemName);
                    }
                });
                return;
            } catch (Exception ignored) {}
        }
        showImagePlaceholder(itemName);
    }

    private void showImagePlaceholder(String name) {
        itemImageView.setImage(null);
        imagePlaceholderLabel.setVisible(true);
        imagePlaceholderLabel.setText(initialOf(name));
    }

    private static String initialOf(String name) {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private void setDefaultValues() {
        categoryLabel.setText("UNKNOWN");
        statusLabel.setText("N/A");
        itemNameLabel.setText("Item Name");
        priceLabel.setText("Current Price: " + formatMoney(0));
        sellerLabel.setText("Unknown");
        attributesBox.getChildren().clear();
        descriptionArea.setText("No detail yet.");
        imagePlaceholderLabel.setVisible(true);
        imagePlaceholderLabel.setText("?");
    }

    // ══════════════════════════════════════════════════════
    //  SESSION
    // ══════════════════════════════════════════════════════

    private void syncFromSession() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        username = currentUser.getUsername() == null ? username : currentUser.getUsername();
        role     = currentUser.getRole()     == null ? role     : currentUser.getRole();
    }

    // ══════════════════════════════════════════════════════
    //  UTIL
    // ══════════════════════════════════════════════════════

    private static String optString(JsonObject obj, String field, String fallback) {
        if (obj == null || !obj.has(field) || obj.get(field).isJsonNull()) return fallback;
        try { return obj.get(field).getAsString(); } catch (Exception e) { return fallback; }
    }

    private static long optLong(JsonObject obj, String field, long fallback) {
        if (obj == null || !obj.has(field) || obj.get(field).isJsonNull()) return fallback;
        try { return obj.get(field).getAsLong(); } catch (Exception e) { return fallback; }
    }

    private String formatMoney(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " VND";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
