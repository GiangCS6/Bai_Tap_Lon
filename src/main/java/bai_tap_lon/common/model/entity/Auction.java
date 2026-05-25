package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.exception.BusinessException;
import bai_tap_lon.common.model.item.Item;
import bai_tap_lon.common.model.user.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Auction — thuần RAM, không DAO, không scheduler.
 *
 * Trách nhiệm:
 *   - Giữ trạng thái phiên (status, currentPrice, currentWinner, bidHistory)
 *   - Validate và xử lý bid (placeBid)
 *   - Notify observers khi có sự kiện
 *   - Trigger AutoBid
 *
 * KHÔNG làm:
 *   - Đụng DB (không có DAO)
 *   - Tự đếm giờ (AuctionTimer lo)
 *   - Xử lý tài chính (RequestRouter lo)
 *   - Cập nhật DB khi start/end/cancel (AuctionManager lo)
 */
public class Auction extends Entity {

    private final Item item;
    private long currentPrice;
    private Bidder currentWinner;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;          // có thể thay đổi khi anti-sniping
    private volatile AuctionStatus status;

    private final List<BidTransaction> bidHistory;
    private final List<BidObserver> observers;
    private final AutoBidManager autoBidManager;

    private static final Logger logger = Logger.getLogger(Auction.class.getName());

    // AutoBid chạy thread riêng để không block synchronized placeBid
    public final ExecutorService autoBidExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("AutoBid-" + getId().substring(0, 8));
        return t;
    });

    // Timer inject từ AuctionManager — Auction hỏi secondsLeft() qua đây
    private AuctionTimer timer;

    // ---------------------------------------------------------------
    // Constructor — nhận BidExecutor inject từ RequestRouter
    // BidExecutor là callback thực hiện bid đầy đủ (tài chính + DB)
    // ---------------------------------------------------------------
    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime,
                   BidExecutor bidExecutor) {
        super();
        this.item          = item;
        this.currentPrice  = item.getStartingPrice();
        this.currentWinner = null;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.status        = AuctionStatus.OPEN;
        this.bidHistory    = new ArrayList<>();
        this.observers     = new CopyOnWriteArrayList<>();
        this.autoBidManager = new AutoBidManager(bidExecutor);
    }

    /**
     * AuctionManager tạo AuctionTimer rồi inject vào đây sau khi tạo Auction.
     */
    public void setTimer(AuctionTimer timer) {
        this.timer = timer;
    }

    /**
     * Inject BidExecutor sau khi Auction được load từ DB (recovery flow).
     * Với auction mới tạo qua handlePostItem, BidExecutor được truyền qua constructor.
     */
    public void setBidExecutor(BidExecutor bidExecutor) {
        this.autoBidManager.setBidExecutor(bidExecutor);
    }

    // ---------------------------------------------------------------
    // placeBid — thuần RAM, không DB, không tài chính
    //
    // Tài chính (decrementBalance bidder mới, incrementBalance oldWinner)
    // đã được RequestRouter.handlePlaceBid() xử lý TRƯỚC khi gọi vào đây.
    // ---------------------------------------------------------------
    public synchronized BidTransaction placeBid(Bidder bidder, long amount) {

        if (status != AuctionStatus.RUNNING) {
            throw new BusinessException("AUCTION_NOT_RUNNING",
                    "Auction hasn't started or already ended");
        }
        if (amount <= currentPrice) {
            throw new BusinessException("BID_TOO_LOW",
                    "Bid must be higher than current price: " + currentPrice);
        }
        if (!bidder.isActive()) {
            throw new BusinessException("ACCOUNT_BANNED", "Your account has been banned");
        }
        if(currentWinner !=null && bidder.getId().equals(currentWinner.getId())){
            throw new BusinessException("Bid_Error","You are WINNER now");
        }

        // Không check balance RAM — DB đã đảm bảo trước khi vào đây

        long   oldPrice  = currentPrice;
        Bidder oldWinner = currentWinner;

        currentPrice  = amount;
        currentWinner = bidder;
        BidTransaction tx = new BidTransaction(bidder, amount, this.getId());

        try {
            bidHistory.add(tx);

            // Anti-sniping: còn <= 10s → gia hạn thêm 60s
            // timer.extendTime() tự reschedule, trả về endTime mới
            if (timer != null && timer.secondsLeft() <= 300) {
                LocalDateTime newEndTime = timer.extendTime(60);
                this.endTime = newEndTime; // đồng bộ RAM với timer
                notifyTimeExtended();
            }

            notifyBidPlaced(tx);
            autoBidExecutor.submit(() -> autoBidManager.trigger(this, tx));

        } catch (Exception e) {
            // Rollback RAM nếu có lỗi
            currentPrice  = oldPrice;
            currentWinner = oldWinner;
            if (!bidHistory.isEmpty()) bidHistory.removeLast();
            throw new BusinessException("BID_FAILED", "Failed to place bid: " + e.getMessage());
        }

        return tx;
    }

    // ---------------------------------------------------------------
    // Lifecycle — CHỈ đổi status RAM + notify observers
    // Cập nhật DB do AuctionManager lo sau khi nhận callback từ timer
    // ---------------------------------------------------------------

    /**
     * Gọi bởi AuctionManager khi đến giờ bắt đầu.
     */
    public synchronized void start() {
        if (status != AuctionStatus.OPEN) return;
        this.status = AuctionStatus.RUNNING;
        notifyAuctionStarted();
        logger.info("[AUCTION] Started: " + getId().substring(0, 8));
    }

    /**
     * Gọi bởi AuctionManager khi timer báo hết giờ.
     * Chỉ đổi status RAM + shutdown AutoBid + notify.
     * DB và tài chính do AuctionManager.settle() xử lý tiếp.
     */
    public synchronized void end() {
        if (status != AuctionStatus.RUNNING) return;
        this.status = (currentWinner != null)
                ? AuctionStatus.FINISHED
                : AuctionStatus.CANCELED;
        autoBidExecutor.shutdownNow();
        notifyAuctionEnd();
        logger.info("[AUCTION] Ended: " + getId().substring(0, 8)
                + " | status: " + status
                + (currentWinner != null ? " | winner: " + currentWinner.getUsername() : " | no winner"));
    }

    /**
     * Gọi bởi AuctionManager khi admin cancel.
     * Chỉ đổi status RAM + notify.
     * DB và hoàn tiền do AuctionManager.cancelAuction() xử lý.
     */
    public synchronized void cancel(String reason) {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED
                || status == AuctionStatus.PAID) return;
        this.status = AuctionStatus.CANCELED;
        autoBidExecutor.shutdownNow();
        notifyAuctionCancelled(reason);
        logger.info("[AUCTION] Cancelled: " + getId().substring(0, 8) + " — " + reason);
    }

    // ---------------------------------------------------------------
    // Observer helpers
    // ---------------------------------------------------------------
    public void notifyBidPlaced(BidTransaction tx)    { for (BidObserver o : observers) o.onBidPlaced(tx); }
    public void notifyAuctionStarted()                { for (BidObserver o : observers) o.onAuctionStarted(this); }
    public void notifyAuctionEnd()                    { for (BidObserver o : observers) o.onAuctionEnd(this); }
    public void notifyAuctionCancelled(String reason) { for (BidObserver o : observers) o.onAuctionCancelled(this, reason); }
    public void notifyTimeExtended()                  { for (BidObserver o : observers) o.onTimeExtended(this); }
    public void addObserver(BidObserver observer)     { if (!observers.contains(observer)) observers.add(observer); }
    public void removeObserver(BidObserver observer)  { observers.remove(observer); }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    public long secondsLeft() {
        return (timer != null) ? timer.secondsLeft() : 0;
    }

    public boolean isActive() { return status == AuctionStatus.RUNNING; }

    /** Chỉ trả winner khi FINISHED hoặc PAID. */
    public Bidder getWinner() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID)
            return currentWinner;
        return null;
    }

    // ---------------------------------------------------------------
    // Getters / Setters
    // ---------------------------------------------------------------
    public Item getItem()                               { return item; }
    public long getCurrentPrice()                       { return currentPrice; }
    public void setCurrentPrice(long price)             { this.currentPrice = price; }
    public Bidder getCurrentWinner()                    { return currentWinner; }
    public void setCurrentWinner(Bidder w)              { this.currentWinner = w; }
    public LocalDateTime getStartTime()                 { return startTime; }
    public LocalDateTime getEndTime()                   { return endTime; }
    public void setEndTime(LocalDateTime endTime)       { this.endTime = endTime; }
    public AuctionStatus getStatus()                    { return status; }
    public void setStatus(AuctionStatus status)         { this.status = status; }
    public List<BidTransaction> getBidHistory()         { return bidHistory; }
    public AuctionTimer getTimer()                      { return timer; }
    public AutoBidManager getAutoBidManager()           { return autoBidManager; }

    @Override
    public void printInfo() {
        logger.info("Auction     : " + getId());
        logger.info("Item        : " + item.getName());
        logger.info("CurrentPrice: " + currentPrice);
        logger.info("Status      : " + status);
        logger.info("EndTime     : " + endTime);
        if (currentWinner != null)
            logger.info("Leading     : " + currentWinner.getUsername());
    }
}