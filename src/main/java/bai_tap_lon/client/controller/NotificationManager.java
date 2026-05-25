package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    private boolean started;
    private Consumer<JsonObject> bidPlacedHandler;
    private Consumer<JsonObject> auctionStartedHandler;
    private Consumer<JsonObject> auctionEndedHandler;
    private Consumer<JsonObject> auctionExtendedHandler;
    private Consumer<JsonObject> auctionCancelledHandler;

    private NotificationManager() {
    }

    public synchronized void start() {
        if (started) return;

        bidPlacedHandler = this::onBidPlaced;
        auctionStartedHandler = this::onAuctionStarted;
        auctionEndedHandler = this::onAuctionEnded;
        auctionExtendedHandler = this::onAuctionTimeExtended;
        auctionCancelledHandler = this::onAuctionCancelled;

        ServerMessageRouter.subscribePush("BID_PLACED", bidPlacedHandler);
        ServerMessageRouter.subscribePush("AUCTION_STARTED", auctionStartedHandler);
        ServerMessageRouter.subscribePush("AUCTION_ENDED", auctionEndedHandler);
        ServerMessageRouter.subscribePush("AUCTION_TIME_EXTENDED", auctionExtendedHandler);
        ServerMessageRouter.subscribePush("AUCTION_CANCELLED", auctionCancelledHandler);
        started = true;
    }

    private void onBidPlaced(JsonObject data) {
        JsonObject tx = data != null && data.has("transaction") && data.get("transaction").isJsonObject()
                ? data.getAsJsonObject("transaction")
                : data;

        String auctionId = optString(tx, "auctionId", optString(data, "auctionId", "N/A"));
        String bidderName = optString(tx, "bidderName", "Unknown");
        long amount = optLong(tx, "amount", 0L);
        LocalDateTime ts = parseIso(optString(tx, "timestamp", null));

        String body = "Phiên **" + auctionId + "** có lượt bid mới từ **" + bidderName
                + "** với giá **" + formatMoney(amount) + "**.";
        push("Có lượt ra giá mới", body, "BID_PLACED", ts);
    }

    private void onAuctionStarted(JsonObject data) {
        String auctionId = optString(data, "auctionId", "N/A");
        long startingPrice = optLong(data, "startingPrice", 0L);
        LocalDateTime ts = parseIso(optString(data, "startTime", null));

        String body = "Phiên **" + auctionId + "** đã bắt đầu với giá khởi điểm **"
                + formatMoney(startingPrice) + "**.";
        push("Phiên đấu giá bắt đầu", body, "AUCTION_STARTED", ts);
    }

    private void onAuctionEnded(JsonObject data) {
        String auctionId = optString(data, "auctionId", "N/A");
        String status = optString(data, "status", "FINISHED");
        String winnerName = optString(data, "winnerName", "");
        long finalPrice = optLong(data, "finalPrice", 0L);

        String body;
        if (winnerName != null && !winnerName.isBlank() && !"null".equalsIgnoreCase(winnerName)) {
            body = "Phiên **" + auctionId + "** đã kết thúc. Người thắng: **" + winnerName
                    + "**, giá cuối: **" + formatMoney(finalPrice) + "**, trạng thái **" + status + "**.";
        } else {
            body = "Phiên **" + auctionId + "** đã kết thúc, không có người thắng. Trạng thái **"
                    + status + "**.";
        }
        push("Phiên đấu giá kết thúc", body, "AUCTION_ENDED", TimeUtil.now());
    }

    private void onAuctionTimeExtended(JsonObject data) {
        String auctionId = optString(data, "auctionId", "N/A");
        LocalDateTime newEndTime = parseIso(optString(data, "newEndTime", null));
        String endAt = newEndTime != null ? TimeUtil.formatTime(newEndTime) : "N/A";

        String body = "Phiên **" + auctionId + "** được gia hạn đến **" + endAt + "**.";
        push("Gia hạn thời gian đấu giá", body, "AUCTION_TIME_EXTENDED", TimeUtil.now());
    }

    private void onAuctionCancelled(JsonObject data) {
        String auctionId = optString(data, "auctionId", "N/A");
        String reason = optString(data, "reason", "Không có lý do");
        String body = "Phiên **" + auctionId + "** đã bị hủy. Lý do: **" + reason + "**.";
        push("Phiên đấu giá bị hủy", body, "AUCTION_CANCELLED", TimeUtil.now());
    }

    private void push(String title, String body, String type, LocalDateTime timestamp) {
        NotificationStore.getInstance().add(
                new Notification(
                        title,
                        body,
                        type,
                        timestamp != null ? timestamp : TimeUtil.now(),
                        false
                )
        );
    }

    private static String formatMoney(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long optLong(JsonObject obj, String key, long fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        try {
            return obj.get(key).getAsLong();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static LocalDateTime parseIso(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return TimeUtil.fromIso(iso);
        } catch (Exception e) {
            return null;
        }
    }
}
