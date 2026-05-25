package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.model.user.Bidder;

import java.util.logging.Logger;

public class BidTransaction extends Entity {

    private final Bidder bidder;
    private final long amount;
    private final String auctionId;
    private static final Logger logger = Logger.getLogger(BidTransaction.class.getName());

    public BidTransaction(Bidder bidder, long amount, String auctionId) {
        super();
        this.bidder = bidder;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    public Bidder getBidder() { return bidder; }
    public long getAmount() { return amount; }
    public String getAuctionId() { return auctionId; }

    @Override
    public void printInfo() {
        logger.info("Transaction: " + getId());
        logger.info("Bidder: " + bidder.getUsername());
        logger.info("Amount: " + amount);
        logger.info("Time: " + getCreatedAt());
    }
}