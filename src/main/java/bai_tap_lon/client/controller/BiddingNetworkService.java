// Gom hết request/response của bidding vào một class cho dễ dùng

package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonObject;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BiddingNetworkService {

    public void send(String action, JsonObject payload) {
        Request req = new Request.Builder().action(action).payload(payload).build();
        Client.getInstance().sendRequest(req);
    }

    public void sendWatch(String auctionId) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        ServerMessageRouter.register("WATCH_AUCTION", d -> {}, (e, m) -> {});
        send("WATCH_AUCTION", p);
    }

    public void sendUnwatch(String auctionId) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        ServerMessageRouter.register("UNWATCH_AUCTION", d -> {}, (e, m) -> {});
        send("UNWATCH_AUCTION", p);
    }

    public void sendGetAuctionDetail(String auctionId, Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        ServerMessageRouter.register("GET_AUCTION_DETAIL", ok, fail);
        send("GET_AUCTION_DETAIL", p);
    }

    public void sendGetBidHistory(String auctionId, Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        ServerMessageRouter.register("GET_AUCTION_BID_HISTORY", ok, fail);
        send("GET_AUCTION_BID_HISTORY", p);
    }

    public void sendGetBalance(Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        ServerMessageRouter.register("GET_BALANCE", ok, fail);
        send("GET_BALANCE", new JsonObject());
    }

    public void sendPlaceBid(String auctionId, long amount, Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        p.addProperty("amount", amount);
        ServerMessageRouter.register("PLACE_BID", ok, fail);
        send("PLACE_BID", p);
    }

    public void sendSetAutoBid(String auctionId, long maxBid, long increment, Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        p.addProperty("maxBid", maxBid);
        p.addProperty("increment", increment);
        ServerMessageRouter.register("SET_AUTO_BID", ok, fail);
        send("SET_AUTO_BID", p);
    }

    public void sendCancelAutoBid(String auctionId, Consumer<JsonObject> ok, BiConsumer<String, String> fail) {
        JsonObject p = new JsonObject();
        p.addProperty("auctionId", auctionId);
        ServerMessageRouter.register("CANCEL_AUTO_BID", ok, fail);
        send("CANCEL_AUTO_BID", p);
    }
}