package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.HasBalance;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * Controller cho popup ví ({@code Wallet.fxml}).
 *
 * Logic role-specific (nút nào hiện, có quyền nạp / rút không) được
 * uỷ quyền cho {@link WalletStrategy}. Controller chỉ giữ:
 *   - Network (DEPOSIT / WITHDRAW / GET_BALANCE)
 *   - UI glue (binding label, validate input, hiển thị status)
 *   - Lifecycle popup (đóng khi cần)
 *
 * Cách hiện popup: gọi {@link #showFor(Button)} từ controller có icon ví.
 * Popup tự bám vào button anchor, auto-hide khi click ngoài.
 */
public class WalletController {

    @FXML private Label     balanceLabel;
    @FXML private Label     roleHintLabel;
    @FXML private Label     statusLabel;
    @FXML private TextField amountField;
    @FXML private Button    depositBtn;
    @FXML private Button    withdrawBtn;
    @FXML private Button    closeBtn;

    private WalletStrategy strategy;
    private Popup popup;

    // ══════════════════════════════════════════════════════════════
    // FACTORY — entry point để các màn khác mở popup
    // ══════════════════════════════════════════════════════════════

    /**
     * Hiện popup ví ngay dưới {@code anchor}. Tự attach vào window của anchor,
     * auto-hide khi user click ngoài. Trả về Popup để caller có thể đóng tay
     * (ví dụ chuyển scene). Nếu role không có ví → trả {@code null} và alert.
     */
    public static Popup showFor(Button anchor) {
        if (anchor == null || anchor.getScene() == null) return null;

        try {
            FXMLLoader loader = new FXMLLoader(
                    WalletController.class.getResource("/viewfxml/Wallet.fxml"));
            Parent root = loader.load();
            WalletController ctrl = loader.getController();

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            ctrl.bindPopup(popup);
            if (!ctrl.initWithSession()) {
                // Admin / role lạ → không mở popup, đã alert sẵn
                return null;
            }

            Window owner = anchor.getScene().getWindow();
            Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            // Anchor góc phải của button → mép phải popup
            double popupWidth = 320;
            double x = b.getMaxX() - popupWidth;
            double y = b.getMaxY() + 6;
            popup.show(owner, x, y);
            return popup;
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Không mở được ví: " + ex.getMessage()).showAndWait();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // INIT — chạy sau showFor()
    // ══════════════════════════════════════════════════════════════

    void bindPopup(Popup popup) { this.popup = popup; }

    /** @return false nếu role không có ví → caller không mở popup. */
    boolean initWithSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Phiên đã hết hạn, vui lòng đăng nhập lại.").showAndWait();
            return false;
        }

        this.strategy = WalletStrategyFactory.forRole(user.getRole());
        strategy.applyButtons(depositBtn, withdrawBtn, roleHintLabel);

        if (!strategy.hasWallet()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Tài khoản admin không có ví, không sử dụng được tính năng này.")
                    .showAndWait();
            return false;
        }

        // Hiển thị balance từ session ngay, rồi đồng bộ với DB
        if (user instanceof HasBalance hb) updateBalance(hb.getBalance());
        requestBalance();
        return true;
    }

    // ══════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onDeposit(ActionEvent e) {
        if (!strategy.canDeposit()) {
            showError("Role hiện tại không được phép nạp.");
            return;
        }
        long amount = parseAmountOrFail();
        if (amount <= 0) return;
        sendBalanceRequest("DEPOSIT", amount, "amountDeposited", "nạp");
    }

    @FXML
    private void onWithdraw(ActionEvent e) {
        if (!strategy.canWithdraw()) {
            showError("Role hiện tại không được phép rút.");
            return;
        }
        long amount = parseAmountOrFail();
        if (amount <= 0) return;
        sendBalanceRequest("WITHDRAW", amount, "amountWithdrawn", "rút");
    }

    @FXML
    private void onClose(ActionEvent e) {
        if (popup != null) popup.hide();
    }

    // ══════════════════════════════════════════════════════════════
    // NETWORK
    // ══════════════════════════════════════════════════════════════

    private void sendBalanceRequest(String action, long amount,
                                    String responseField, String verbVi) {
        JsonObject payload = new JsonObject();
        payload.addProperty("amount", amount);
        Request req = new Request.Builder().action(action).payload(payload).build();

        ServerMessageRouter.register(action,
                data -> onBalanceSuccess(data, responseField, verbVi),
                this::onBalanceFail);

        setBusy(true);
        showStatus("Đang " + verbVi + " tiền...", "#64FFDA");
        try {
            Client.getInstance().sendRequest(req);
        } catch (Exception ex) {
            setBusy(false);
            showError("Không gửi được yêu cầu: " + ex.getMessage());
        }
    }

    private void onBalanceSuccess(JsonObject data, String amountField, String verbVi) {
        setBusy(false);
        if (data == null) return;

        long newBalance = data.has("newBalance") && !data.get("newBalance").isJsonNull()
                ? data.get("newBalance").getAsLong() : 0L;
        long delta = data.has(amountField) && !data.get(amountField).isJsonNull()
                ? data.get(amountField).getAsLong() : 0L;

        updateBalance(newBalance);
        // Đồng bộ ngược về session để các màn khác nhất quán
        User u = SessionManager.getInstance().getCurrentUser();
        if (u instanceof HasBalance hb) hb.setBalance(newBalance);

        this.amountField.clear();
        showStatus("✓ Đã " + verbVi + " " + format(delta) + " thành công.", "#64FFDA");
    }

    private void onBalanceFail(String code, String message) {
        setBusy(false);
        String msg = (message == null || message.isBlank()) ? code : message;
        showError("✗ " + (msg == null ? "Lỗi không xác định" : msg));
    }

    private void requestBalance() {
        Request req = new Request.Builder()
                .action("GET_BALANCE")
                .payload(new JsonObject())
                .build();
        ServerMessageRouter.register("GET_BALANCE",
                data -> {
                    if (data != null && data.has("balance") && !data.get("balance").isJsonNull()) {
                        long b = data.get("balance").getAsLong();
                        updateBalance(b);
                        User u = SessionManager.getInstance().getCurrentUser();
                        if (u instanceof HasBalance hb) hb.setBalance(b);
                    }
                },
                (code, message) -> { /* im lặng — đã có balance từ session */ });
        try {
            Client.getInstance().sendRequest(req);
        } catch (Exception ignored) {
        }
    }

    // ══════════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════════

    private long parseAmountOrFail() {
        String text = amountField.getText() == null ? "" : amountField.getText().trim();
        if (text.isEmpty()) {
            showError("Vui lòng nhập số tiền.");
            amountField.requestFocus();
            return -1;
        }
        try {
            long val = Long.parseLong(text);
            if (val < 1000L) {
                showError("Số tiền tối thiểu là 1.000 VNĐ.");
                return -1;
            }
            return val;
        } catch (NumberFormatException ex) {
            showError("Số tiền phải là số nguyên dương.");
            return -1;
        }
    }

    private void updateBalance(long balance) {
        Platform.runLater(() -> balanceLabel.setText(format(balance)));
    }

    private static String format(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }

    private void setBusy(boolean busy) {
        if (depositBtn != null)  depositBtn.setDisable(busy && strategy.canDeposit());
        if (withdrawBtn != null) withdrawBtn.setDisable(busy && strategy.canWithdraw());
        if (amountField != null) amountField.setDisable(busy);
    }

    private void showStatus(String msg, String color) {
        if (statusLabel == null) return;
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        statusLabel.setText(msg);
    }

    private void showError(String msg) { showStatus(msg, "#FF6B6B"); }
}