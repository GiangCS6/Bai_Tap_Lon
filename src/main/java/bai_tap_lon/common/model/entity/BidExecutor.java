package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.model.user.Bidder;

public interface BidExecutor {
    BidTransaction placeBid(Bidder bidder, long amount);
}
