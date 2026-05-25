package bai_tap_lon.server.dto;

import bai_tap_lon.common.model.entity.AuctionStatus;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

public class BidDetails {
    private String auctionId;
    private String itemName;
    private long amount;
    private LocalDateTime timestamp;
    private AuctionStatus auctionStatus;
    private boolean isWinning;

    public BidDetails(String auctionId, String itemName, long amount, LocalDateTime timestamp, AuctionStatus auctionStatus, boolean isWinning){
        this.auctionStatus = auctionStatus;
        this.auctionId = auctionId;
        this.isWinning = isWinning;
        this.itemName = itemName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getAuctionId() {
        return auctionId;
    }
    public String getItemName() {
        return itemName;
    }
    public long getAmount() {
        return amount;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public AuctionStatus getAuctionStatus() {
        return auctionStatus;
    }
    public boolean isWinning() {
        return isWinning;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("auctionId", auctionId);
        o.addProperty("itemName", itemName);
        o.addProperty("amount", amount);
        o.addProperty("timestamp", TimeUtil.toIso(timestamp));
        o.addProperty("auctionStatus", auctionStatus.name());
        o.addProperty("isWinning", isWinning);
        return o;
    }
}
