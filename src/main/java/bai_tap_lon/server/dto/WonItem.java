package bai_tap_lon.server.dto;

import com.google.gson.JsonObject;
import bai_tap_lon.common.network.TimeUtil;

import java.time.LocalDateTime;

public class WonItem {
    private final String auctionId;
    private final String itemName;
    private final long finalPrice;
    private final String auctionStatus;
    private final LocalDateTime endTime;

    public WonItem(String auctionId, String itemName, long finalPrice,
                   String auctionStatus, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.finalPrice = finalPrice;
        this.auctionStatus = auctionStatus;
        this.endTime = endTime;
    }

    public String getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public long getFinalPrice() { return finalPrice; }
    public String getAuctionStatus() { return auctionStatus; }
    public LocalDateTime getEndTime() { return endTime; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("auctionId", auctionId);
        o.addProperty("itemName", itemName);
        o.addProperty("finalPrice", finalPrice);
        o.addProperty("auctionStatus", auctionStatus);
        o.addProperty("endTime", TimeUtil.toIso(endTime));
        return o;
    }
}
