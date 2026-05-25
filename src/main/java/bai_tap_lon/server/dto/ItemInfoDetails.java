package bai_tap_lon.server.dto;

import bai_tap_lon.common.model.entity.AuctionStatus;
import com.google.gson.JsonObject;

public class ItemInfoDetails {
    private final String itemName;
    private final String description;
    private final String imageUrl;
    private final String category;
    private final String sellerName;
    private final long startingPrice;
    private final long currentPrice;
    private final AuctionStatus auctionStatus;


    public ItemInfoDetails(String itemName, String description, String imageUrl,
                           String category, String sellerName,
                           long startingPrice, long currentPrice, AuctionStatus auctionStatus) {
        this.itemName = itemName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.sellerName = sellerName;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.auctionStatus = auctionStatus;
    }

    public String getItemName()      { return itemName; }
    public String getDescription()   { return description; }
    public String getImageUrl()      { return imageUrl; }
    public String getSellerName()    { return sellerName; }
    public long   getStartingPrice() { return startingPrice; }
    public long   getCurrentPrice()  { return currentPrice; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("itemName",      itemName);
        o.addProperty("description",   description);
        o.addProperty("imageUrl",      imageUrl);
        o.addProperty("category",      category);
        o.addProperty("sellerName",    sellerName);
        o.addProperty("startingPrice", startingPrice);
        o.addProperty("currentPrice",  currentPrice);
        o.addProperty("auctionStatus", auctionStatus.toString());
        return o;
    }
}
