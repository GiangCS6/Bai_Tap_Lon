package bai_tap_lon.server.dto;

import com.google.gson.JsonObject;
import bai_tap_lon.common.network.TimeUtil;

import java.time.LocalDateTime;

public class ActiveBidSummary {
    private final String auctionId;
    private final String itemName;
    private final long myLastBid;
    private final long currentPrice;
    private final boolean isWinning;
    private final LocalDateTime endTime;

    public ActiveBidSummary(String auctionId, String itemName, long myLastBid,
                            long currentPrice, boolean isWinning, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.myLastBid = myLastBid;
        this.currentPrice = currentPrice;
        this.isWinning = isWinning;
        this.endTime = endTime;
    }

    public String getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public long getMyLastBid() { return myLastBid; }
    public long getCurrentPrice() { return currentPrice; }
    public boolean isWinning() { return isWinning; }
    public LocalDateTime getEndTime() { return endTime; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("auctionId", auctionId);
        o.addProperty("itemName", itemName);
        o.addProperty("myLastBid", myLastBid);
        o.addProperty("currentPrice", currentPrice);
        o.addProperty("isWinning", isWinning);
        o.addProperty("endTime", TimeUtil.toIso(endTime));
        return o;
    }
}
