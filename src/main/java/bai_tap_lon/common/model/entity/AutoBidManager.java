package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.exception.BusinessException;
import bai_tap_lon.common.model.user.Bidder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoBidManager {

    private final Map<String, Bidder> bidders =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * BidExecutor là callback inject từ ngoài — thực hiện bid đầy đủ
     * Trong Auction constructor, lambda này sẽ được truyền từ RequestRouter
     */
    private BidExecutor bidExecutor;

    private final Object lock = new Object();
    private volatile boolean isRunning = false;
    private static final Logger logger = Logger.getLogger(AutoBidManager.class.getName());


    public AutoBidManager(BidExecutor bidExecutor) {
        this.bidExecutor = bidExecutor;
    }

    public void setBidExecutor(BidExecutor bidExecutor){
        this.bidExecutor=bidExecutor;
    }
    public void registerBidder(Bidder bidder) {
        bidders.putIfAbsent(bidder.getId(), bidder);
    }

    public void unregisterBidder(String bidderId) {
        bidders.remove(bidderId);
    }

    /**
     * Trigger AutoBid sau mỗi lần có bid mới (kể cả autobid trước đó).
     *
     * isRunning guard: chỉ cho 1 vòng autobid chạy tại một thời điểm.
     * Nếu 2 manual bid đến gần nhau, vòng thứ 2 bị skip vì vòng thứ 1
     * sẽ findCandidate với currentPrice mới nhất khi xong.
     *
     * BidExecutor.placeBid() ném BusinessException nếu thất bại →
     * vònx`g lặp dừng an toàn, không bị stuck.
     */
    public void trigger(Auction auction, BidTransaction lastTx) {
        synchronized (lock) {
            if (isRunning) return;
            if (auction.getStatus() != AuctionStatus.RUNNING) return;
            isRunning = true;
        }
        try {
            BidTransaction current = lastTx;

            while (true) {

                try {
                    Thread.sleep(1000);
                }
                catch(InterruptedException e){
                    logger.log(Level.SEVERE,":",e);
                    break;
                }

                if (auction.getStatus() != AuctionStatus.RUNNING) break;

                AutoBidCandidate candidate = findCandidate(auction, current);
                if (candidate == null) break;

                try {
                    // BidExecutor thực hiện đầy đủ: tài chính + DB + RAM
                    BidTransaction tx = bidExecutor.placeBid(
                            candidate.bidder,
                            candidate.amount
                    );
                    logger.info(String.format("[AUTOBID] %s → %d",
                            candidate.bidder.getUsername(), candidate.amount));
                    current = tx;

                } catch (BusinessException e) {
                    // Bid thất bại: không đủ tiền, auction ended, v.v. → dừng
                    logger.info("[AUTOBID] %s failed — %s%n"+
                            candidate.bidder.getUsername()+ e.getMessage());
                    break;
                }
                logger.info("[AUTOBID] trigger called, lastBidder="
                        + lastTx.getBidder().getUsername()
                        + ", currentWinner=" + auction.getCurrentWinner().getUsername()
                        + ", isRunning=" + isRunning);
            }

        } finally {
            synchronized (lock) {
                isRunning = false;
            }
        }
    }

    /**
     * Tìm candidate hợp lệ theo thứ tự ưu tiên:
     *
     *  1. Bỏ qua bidder vừa đặt
     *  2. Bỏ qua bidder không active hoặc không có AutoBidSetting active
     *  3. Chỉ xét bidder mà (currentPrice + increment) <= maxBid
     *  4. Ưu tiên maxBid CAO NHẤT — tiebreaker: đăng ký sớm hơn (FIFO)
     *  5. Bỏ qua crWinner
     */
    private AutoBidCandidate findCandidate(Auction auction, BidTransaction lastTx) {

        String lastBidderId = lastTx.getBidder().getId();
        long   currentPrice = auction.getCurrentPrice();

        List<Bidder> ordered;
        synchronized (bidders) {
            ordered = new ArrayList<>(bidders.values());
        }

        ordered.sort(Comparator.comparingLong(
                (Bidder b) -> b.getAutoBidSetting() != null
                        ? b.getAutoBidSetting().getMaxBid() : 0L
        ).reversed());

        for (Bidder b : ordered) {
            if (b.getId().equals(lastBidderId)) continue;
            if (!b.isActive()) continue;
            if (b.getId().equals(auction.getCurrentWinner().getId())) continue;

            AutoBidSetting s = b.getAutoBidSetting();
            if (s == null || !s.isActive()) continue;

            long next = currentPrice + s.getIncrement();
            if (next > s.getMaxBid()) continue;
            if (next > b.getBalance()) continue;

            return new AutoBidCandidate(b, next);
        }

        return null;
    }

    private static class AutoBidCandidate {
        final Bidder bidder;
        final long   amount;

        AutoBidCandidate(Bidder b, long a) {
            bidder = b;
            amount = a;
        }
    }
}