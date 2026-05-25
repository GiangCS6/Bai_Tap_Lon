package bai_tap_lon.client.controller;

import com.google.gson.JsonObject;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.network.TimeUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.function.Consumer;

/**
 *  Controller gắn với auction-card.fxml.
 *
 *  Mỗi card là 1 phiên đấu giá. Parent (ActiveAuctionsController) nạp FXML này,
 *  lấy controller, rồi gọi:
 *      setData(jsonAuction)              - đổ dữ liệu lên các Label
 *      configureForRole(role, onPri, onDt) - cấu hình 2 nút theo vai trò
 *
 *  FXML có sẵn 2 nút: registerBtn + detailBtn.
 *      Admin : registerBtn = "Cancel"
 *      Bidder: registerBtn = "Place Bid"
 *      Seller: registerBtn ẩn
 *      Cả 3 role: detailBtn = "Details"
 */
public class AuctionCardController {

    // ══════════════════════════════════════════════════════
    //  FXML - khớp với fx:id trong auction-card.fxml
    // ══════════════════════════════════════════════════════

    @FXML private StackPane imageBox;
    @FXML private Label     itemNameLabel;
    @FXML private Label     statusLabel;
    @FXML private Label     priceValueLabel;
    @FXML private Label     timeValueLabel;
    @FXML private Button    registerBtn;
    @FXML private Button    detailBtn;

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════

    private String auctionId;
    private LocalDateTime endTime;
    private Timeline countdown;
    private Consumer<JsonObject> onTimeExtendedHandler;

    // ══════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════

    /**
     *  Đổ dữ liệu phiên đấu giá lên card.
     *  JSON khớp với toAuctionSummary() ở RequestRouter:
     *    { id, itemName, category, currentPrice, bidCount, endTime, status }
     */
    public void setData(JsonObject a) {
        this.auctionId = optString(a, "id", "");

        String name = optString(a, "itemName", "Unknown");
        itemNameLabel.setText(name);

        statusLabel.setText(optString(a, "status", "RUNNING"));
        priceValueLabel.setText(formatCurrency(optLong(a, "currentPrice", 0L)));

        this.endTime = parseEndTime(optString(a, "endTime", null));
        startCountdown();
        subscribeTimeExtended();

        renderImage(optString(a, "imageUrl", ""), name);
    }

    /**
     *  Hiện ảnh sản phẩm nếu có URL hợp lệ, fallback về chữ cái đầu khi
     *  URL trống hoặc load lỗi.
     */
    private void renderImage(String imageUrl, String name) {
        imageBox.getChildren().clear();

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, true); // background loading
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.fitWidthProperty().bind(imageBox.widthProperty());
                iv.fitHeightProperty().bind(imageBox.heightProperty());
                imageBox.getChildren().add(iv);

                // Nếu load fail (404, malformed) thì rơi về placeholder
                img.errorProperty().addListener((obs, was, isErr) -> {
                    if (Boolean.TRUE.equals(isErr)) showInitialPlaceholder(name);
                });
                return;
            } catch (Exception ignore) {
                // rơi xuống fallback dưới
            }
        }
        showInitialPlaceholder(name);
    }

    private void showInitialPlaceholder(String name) {
        imageBox.getChildren().clear();
        Label initial = new Label(initialOf(name));
        initial.getStyleClass().add("plate-text-line1");
        imageBox.getChildren().add(initial);
    }

    /**
     *  Gắn hành vi cho 2 nút theo role.
     *      onPrimary: callback khi bấm registerBtn (Place Bid / Cancel), nhận auctionId
     *      onDetail : callback khi bấm detailBtn, nhận auctionId
     */
    public void configureForRole(String role,
                                 Consumer<String> onPrimary,
                                 Consumer<String> onDetail) {

        // Details chung cho tất cả
        if (detailBtn != null && onDetail != null) {
            detailBtn.setOnAction(e -> onDetail.accept(auctionId));
        }

        if (registerBtn == null) return;

        String r = (role == null) ? "" : role.trim().toLowerCase(Locale.ROOT);
        switch (r) {
            case "admin" -> {
                registerBtn.setText("Cancel");
                registerBtn.setVisible(true);
                registerBtn.setManaged(true);
                if (onPrimary != null) registerBtn.setOnAction(e -> onPrimary.accept(auctionId));
            }
            case "bidder" -> {
                registerBtn.setText("Place Bid");
                registerBtn.setVisible(true);
                registerBtn.setManaged(true);
                if (onPrimary != null) registerBtn.setOnAction(e -> onPrimary.accept(auctionId));
            }
            case "seller" -> {
                // Seller không có hành động primary trên phiên này
                registerBtn.setVisible(false);
                registerBtn.setManaged(false);
            }
            default -> {
                registerBtn.setVisible(false);
                registerBtn.setManaged(false);
            }
        }
    }

    public String getAuctionId() { return auctionId; }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private String initialOf(String name) {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private String formatCurrency(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " VNĐ";
    }

    private LocalDateTime parseEndTime(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return TimeUtil.fromIso(iso);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void startCountdown() {
        stopCountdown();
        if (endTime == null) {
            timeValueLabel.setText("--:--:--");
            return;
        }
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdown.setCycleCount(Animation.INDEFINITE);
        countdown.play();
        tick();

        // Tự dừng khi card bị remove khỏi scene → tránh leak Timeline
        timeValueLabel.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS == null) stopCountdown();
        });
    }

    private void subscribeTimeExtended() {
        if (onTimeExtendedHandler != null) return;
        onTimeExtendedHandler = this::onTimeExtended;
        ServerMessageRouter.subscribePush("AUCTION_TIME_EXTENDED", onTimeExtendedHandler);
    }

    private void onTimeExtended(JsonObject data) {
        String id = optString(data, "auctionId", "");
        if (!id.equals(this.auctionId)) return;

        String newEndTimeStr = optString(data, "newEndTime", null);
        if (newEndTimeStr == null) return;

        this.endTime = parseEndTime(newEndTimeStr);
        tick(); // refresh UI ngay, Timeline đang chạy sẽ tự đọc endTime mới ở tick kế tiếp
    }

    private void stopCountdown() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
        if (onTimeExtendedHandler != null) {
            ServerMessageRouter.unsubscribePush("AUCTION_TIME_EXTENDED", onTimeExtendedHandler);
            onTimeExtendedHandler = null;
        }
    }

    private void tick() {
        if (endTime == null) return;
        long secs = TimeUtil.now().until(endTime, ChronoUnit.SECONDS);
        if (secs <= 0) {
            timeValueLabel.setText("00:00:00");
            timeValueLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            stopCountdown();
            return;
        }
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        timeValueLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        String color = secs <= 60 ? "#ef4444" : secs <= 300 ? "#f59e0b" : "#f8fafc";
        timeValueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private static String optString(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }
    private static long optLong(JsonObject o, String key, long def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsLong() : def;
    }
    private static int optInt(JsonObject o, String key, int def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsInt() : def;
    }
}
