package bai_tap_lon.server.dto;

import bai_tap_lon.common.model.entity.AuctionStatus;
import com.google.gson.JsonObject;

public class ItemDetails {
    private final String id;
    private final String name;
    private final long startingPrice;
    private final String category;
    private final String auctionId;
    private final AuctionStatus auctionStatus;

    public ItemDetails(String id, String name, long startingPrice,
                       String category, String auctionId, AuctionStatus auctionStatus) {
        this.id           = id;
        this.name         = name;
        this.startingPrice = startingPrice;
        this.category     = category;
        this.auctionId    = auctionId;
        this.auctionStatus = auctionStatus;
    }

    public String getId()                  { return id; }
    public String getName()                { return name; }
    public long getStartingPrice()         { return startingPrice; }
    public String getCategory()            { return category; }
    public String getAuctionId()           { return auctionId; }
    public AuctionStatus getAuctionStatus(){ return auctionStatus; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id",            id);
        o.addProperty("name",          name);
        o.addProperty("startingPrice", startingPrice);
        o.addProperty("category",      category);
        o.addProperty("auctionId",     auctionId);
        o.addProperty("auctionStatus", auctionStatus.name());
        return o;
    }
}