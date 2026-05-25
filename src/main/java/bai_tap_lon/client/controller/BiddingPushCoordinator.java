// Quản lý subscribe/unsubscribe push events cho bidding

package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.ServerMessageRouter;
import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class BiddingPushCoordinator {
    private Consumer<JsonObject> bidPlaced;
    private Consumer<JsonObject> started;
    private Consumer<JsonObject> ended;
    private Consumer<JsonObject> extended;
    private Consumer<JsonObject> cancelled;

    public void subscribe(
            Consumer<JsonObject> onBidPlaced,
            Consumer<JsonObject> onStarted,
            Consumer<JsonObject> onEnded,
            Consumer<JsonObject> onExtended,
            Consumer<JsonObject> onCancelled
    ) {
        this.bidPlaced = onBidPlaced;
        this.started = onStarted;
        this.ended = onEnded;
        this.extended = onExtended;
        this.cancelled = onCancelled;

        ServerMessageRouter.subscribePush("BID_PLACED", bidPlaced);
        ServerMessageRouter.subscribePush("AUCTION_STARTED", started);
        ServerMessageRouter.subscribePush("AUCTION_ENDED", ended);
        ServerMessageRouter.subscribePush("AUCTION_TIME_EXTENDED", extended);
        ServerMessageRouter.subscribePush("AUCTION_CANCELLED", cancelled);
    }

    public void unsubscribe() {
        if (bidPlaced != null) ServerMessageRouter.unsubscribePush("BID_PLACED", bidPlaced);
        if (started != null) ServerMessageRouter.unsubscribePush("AUCTION_STARTED", started);
        if (ended != null) ServerMessageRouter.unsubscribePush("AUCTION_ENDED", ended);
        if (extended != null) ServerMessageRouter.unsubscribePush("AUCTION_TIME_EXTENDED", extended);
        if (cancelled != null) ServerMessageRouter.unsubscribePush("AUCTION_CANCELLED", cancelled);
    }
}