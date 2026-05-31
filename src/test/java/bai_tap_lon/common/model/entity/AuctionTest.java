package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.exception.BusinessException;
import bai_tap_lon.common.model.item.Electronics;
import bai_tap_lon.common.model.item.Item;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private final List<Auction> auctionsToShutdown = new ArrayList<>();

    @AfterEach
    void shutdownAutoBidExecutors() {
        auctionsToShutdown.forEach(auction -> {
            auction.autoBidExecutor.shutdown();
            try {
                if (!auction.autoBidExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    auction.autoBidExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                auction.autoBidExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    @Test
    void placeBidUpdatesWinnerPriceHistoryAndNotifiesObserver() {
        Auction auction = runningAuction(1_000);
        Bidder bidder = bidder("bidder-1");
        RecordingObserver observer = new RecordingObserver();
        auction.addObserver(observer);

        BidTransaction tx = auction.placeBid(bidder, 1_500);

        assertSame(bidder, auction.getCurrentWinner());
        assertEquals(1_500, auction.getCurrentPrice());
        assertEquals(1, auction.getBidHistory().size());
        assertSame(tx, auction.getBidHistory().getFirst());
        assertSame(tx, observer.lastBid);
        assertEquals(1, observer.bidPlacedCount);
    }

    @Test
    void placeBidRejectsAuctionThatIsNotRunning() {
        Auction auction = newAuction(1_000);
        Bidder bidder = bidder("bidder-1");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> auction.placeBid(bidder, 1_500)
        );

        assertEquals("AUCTION_NOT_RUNNING", ex.getCode());
        assertEquals(1_000, auction.getCurrentPrice());
        assertNull(auction.getCurrentWinner());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void placeBidRejectsAmountNotHigherThanCurrentPrice() {
        Auction auction = runningAuction(1_000);
        Bidder bidder = bidder("bidder-1");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> auction.placeBid(bidder, 1_000)
        );

        assertEquals("BID_TOO_LOW", ex.getCode());
        assertEquals(1_000, auction.getCurrentPrice());
        assertNull(auction.getCurrentWinner());
    }

    @Test
    void placeBidRejectsCurrentWinnerBiddingAgain() {
        Auction auction = runningAuction(1_000);
        Bidder bidder = bidder("bidder-1");
        auction.placeBid(bidder, 1_500);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> auction.placeBid(bidder, 2_000)
        );

        assertEquals("Bid_Error", ex.getCode());
        assertEquals(1_500, auction.getCurrentPrice());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void endFinishesAuctionOnlyWhenThereIsWinner() {
        Auction auction = runningAuction(1_000);
        Bidder bidder = bidder("bidder-1");
        auction.placeBid(bidder, 1_500);

        auction.end();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertSame(bidder, auction.getWinner());
    }

    @Test
    void endCancelsRunningAuctionWithoutWinner() {
        Auction auction = runningAuction(1_000);

        auction.end();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertNull(auction.getWinner());
    }

    private Auction runningAuction(long startingPrice) {
        Auction auction = newAuction(startingPrice);
        auction.start();
        return auction;
    }

    private Auction newAuction(long startingPrice) {
        Seller seller = new Seller("seller", "password", "seller@example.com");
        Item item = new Electronics(
                "Laptop",
                "Gaming laptop",
                startingPrice,
                seller,
                "/uploads/laptop.png",
                "Dell",
                12
        );
        Auction auction = new Auction(
                item,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                null
        );
        auctionsToShutdown.add(auction);
        return auction;
    }

    private Bidder bidder(String username) {
        Bidder bidder = new Bidder(username, "password", username + "@example.com");
        bidder.setBalance(10_000);
        return bidder;
    }

    private static class RecordingObserver implements BidObserver {
        private int bidPlacedCount;
        private BidTransaction lastBid;

        @Override
        public void onBidPlaced(BidTransaction tx) {
            bidPlacedCount++;
            lastBid = tx;
        }

        @Override
        public void onAuctionStarted(Auction auction) {
        }

        @Override
        public void onAuctionEnd(Auction auction) {
        }

        @Override
        public void onTimeExtended(Auction auction) {
        }

        @Override
        public void onAuctionCancelled(Auction auction, String reason) {
        }
    }
}
