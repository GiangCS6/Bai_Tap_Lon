package com.example.bai_tap_lon.server;
// quan sát các bid của client
import com.example.bai_tap_lon.model.AuctionSession;
import com.example.bai_tap_lon.model.Bid;

public interface BidObserver {
    void onNewBid(AuctionSession session, Bid bid);
}
