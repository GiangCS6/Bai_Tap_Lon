package bai_tap_lon.common.model.entity;

import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.network.TimeUtil;
import bai_tap_lon.server.dao.AuctionDao;
import bai_tap_lon.server.dao.AutoBidDao;
import bai_tap_lon.server.dao.UserDao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AuctionManger — quản lý vòng đời auction, cập nhật DB, settlement tài chính.
 *
 * Trách nhiệm:
 *   1. Giữ map các auction đang hoạt động (OPEN / RUNNING)
 *   2. Tạo AuctionTimer, inject vào Auction, wire callback onStart/onEnd
 *   3. Khi timer báo bắt đầu  → auction.start() + updateStatus DB
 *   4. Khi timer báo kết thúc → auction.end() + updateStatus DB + cộng tiền seller
 *   5. Khi admin cancel       → dừng timer + hoàn tiền winner + auction.cancel() + updateStatus DB
 *   6. Khi recover sau crash: phiên đã quá end_time → settle thẳng,
 *      KHÔNG dùng timer để tránh race condition giữa onStart và onEnd.
 *
 * Auction không biết DB tồn tại — mọi thao tác DB đi qua AuctionManager.
 */
public class AuctionManager {

    private static volatile AuctionManager instance;
    private final ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // DAO inject từ Server.init() — không tự new
    private UserDao userDao;
    private AuctionDao auctionDao;
    private AutoBidDao autoBidDao;

    private static final Logger logger =
            Logger.getLogger(AuctionManager.class.getName());

    // Listener vòng đời (broadcast / audit / metrics) — register từ Server
    private final List<AuctionLifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();


    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager();
            }
        }
        return instance;
    }



    /**
     * Inject DAO — gọi một lần khi khởi động Server.
     */
    public void init(UserDao userDao, AuctionDao auctionDao) {
        this.userDao    = userDao;
        this.auctionDao = auctionDao;
        this.autoBidDao = new AutoBidDao();
    }


    public void recoverFromDb(BidExecutorFactory factory) {
        try {
            List<Auction> pending = auctionDao.findAllOpenAndRunning();
            int recoveredCount = 0;
            int settledCount = 0;
            LocalDateTime now = TimeUtil.now();

            for (Auction auction : pending) {
                // Bỏ qua nếu đã được add rồi (tránh duplicate)
                if (activeAuctions.containsKey(auction.getId())) continue;

                // Wire BidExecutor — factory tạo lambda trỏ vào executeBid() của RequestRouter
                BidExecutor executor = factory.create(auction);
                auction.setBidExecutor(executor);

                // ─────────────────────────────────────────────────────────
                // CASE 1: Phiên đã quá end_time (server tắt khi phiên đang chạy)
                // → Settle thẳng tại đây, KHÔNG dùng timer
                //   Nếu dùng timer với delay=0 cho cả onStart và onEnd, có thể
                //   xảy ra race: onEnd chạy trước onStart → status guard fail
                //   → phiên kẹt OPEN trong DB.
                // ─────────────────────────────────────────────────────────
                if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
                    AuctionStatus finalStatus = (auction.getCurrentWinner() != null)
                            ? AuctionStatus.FINISHED
                            : AuctionStatus.CANCELED;
                    auction.setStatus(finalStatus);
                    auctionDao.updateStatus(auction.getId(), finalStatus);

                    // Settlement: nếu có winner → cộng tiền cho seller
                    if (finalStatus == AuctionStatus.FINISHED) {
                        userDao.incrementBalance(
                                auction.getItem().getSeller().getId(),
                                auction.getCurrentPrice());
                        System.out.println("[RECOVER-SETTLE] Paid " + auction.getCurrentPrice()
                                + " → seller: " + auction.getItem().getSeller().getUsername());
                    }

                    autoBidDao.deactivateAllForAuction(auction.getId());
                    settledCount++;
                    System.out.println("[RECOVER] Auto-settled expired auction: "
                            + auction.getId().substring(0, 8) + " → " + finalStatus);
                    continue; // KHÔNG add vào activeAuctions, KHÔNG tạo timer
                }

                // ─────────────────────────────────────────────────────────
                // CASE 2: Phiên còn hạn — recover bình thường
                // Nếu RUNNING mà server vừa crash: reset về OPEN để timer start lại đúng
                // ─────────────────────────────────────────────────────────
                if (auction.getStatus() == AuctionStatus.RUNNING) {
                    auction.setStatus(AuctionStatus.OPEN);
                    auctionDao.updateStatus(auction.getId(), AuctionStatus.OPEN);
                }

                addAuction(auction);
                recoveredCount++;
                logger.info("[RECOVER] Loaded auction: " + auction.getId().substring(0, 8)
                        + " | item: " + auction.getItem().getName()
                        + " | startTime: " + auction.getStartTime()
                        + " | endTime: " + auction.getEndTime());
            }

            logger.info("[RECOVER] Recovered " + recoveredCount + " auction(s), settled "
                    + settledCount + " expired auction(s)");

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "[ERROR] recoverFromDb failed",
                    e);
        }
    }

    public interface BidExecutorFactory {
        BidExecutor create(Auction auction);
    }

    // ---------------------------------------------------------------
    // Lifecycle listener API — observer pattern phạm vi toàn manager.
    // ClientHandler tự register/unregister; AuctionManager không biết
    // gì về network, chỉ biết "có ai đó đang nghe".
    // ---------------------------------------------------------------
    public void addLifecycleListener(AuctionLifecycleListener listener) {
        if (listener != null) lifecycleListeners.add(listener);
    }

    public void removeLifecycleListener(AuctionLifecycleListener listener) {
        if (listener != null) lifecycleListeners.remove(listener);
    }

    private void notifyAuctionCreated(Auction auction) {
        for (AuctionLifecycleListener l : lifecycleListeners) {
            try { l.onAuctionCreated(auction); }
            catch (Exception e) {
                System.err.println("[AuctionManager] Listener onAuctionCreated failed: " + e.getMessage());
            }
        }
    }

    private void notifyAuctionStarted(Auction auction) {
        for (AuctionLifecycleListener l : lifecycleListeners) {
            try { l.onAuctionStarted(auction); }
            catch (Exception e) {
                System.err.println("[AuctionManager] Listener onAuctionStarted failed: " + e.getMessage());
            }
        }
    }

    private void notifyAuctionEnded(Auction auction) {
        for (AuctionLifecycleListener l : lifecycleListeners) {
            try { l.onAuctionEnded(auction); }
            catch (Exception e) {
                System.err.println("[AuctionManager] Listener onAuctionEnded failed: " + e.getMessage());
            }
        }
    }

    private void notifyAuctionCancelled(Auction auction, String reason) {
        for (AuctionLifecycleListener l : lifecycleListeners) {
            try { l.onAuctionCancelled(auction, reason); }
            catch (Exception e) {
                System.err.println("[AuctionManager] Listener onAuctionCancelled failed: " + e.getMessage());
            }
        }
    }

    /**
     * Đăng ký phiên mới do seller vừa POST_ITEM:
     * gồm bước add (timer + map) và bắn event lifecycle để
     * các listener (broadcast realtime tới client, ...) biết.
     *
     * Recover khi server start KHÔNG đi qua đây — recover gọi {@link #addAuction}
     * trực tiếp để không bắn event giả.
     */
    public void onAuctionPosted(Auction auction) {
        addAuction(auction);
        notifyAuctionCreated(auction);
    }

    // addAuction — tạo timer, wire callback, đặt lịch
    public void addAuction(Auction auction) {
        AuctionTimer timer = new AuctionTimer(
                auction.getId(),
                auction.getStartTime(),
                auction.getEndTime(),
                () -> onAuctionStart(auction),
                () -> onAuctionEnd(auction)
        );
        auction.setTimer(timer);
        activeAuctions.put(auction.getId(), auction);
        timer.start(auction.getStartTime());

        logger.info("[AuctionManager] Added: " + auction.getId().substring(0, 8)
                + " | item: " + auction.getItem().getName());
    }

    // onAuctionStart — callback từ AuctionTimer
    private void onAuctionStart(Auction auction) {
        auction.start(); // đổi status RAM + notify (per-auction observers)
        auctionDao.updateStatus(auction.getId(), AuctionStatus.RUNNING);
        notifyAuctionStarted(auction); // lifecycle: broadcast tới mọi client lobby
    }


    // onAuctionEnd — callback từ AuctionTimer khi hết giờ

    private void onAuctionEnd(Auction auction) {
        auction.end();
        // đổi status RAM + notify
        AuctionStatus finalStatus = auction.getStatus();

        // Cập nhật status DB
        auctionDao.updateStatus(auction.getId(), finalStatus);

        // Settlement: cộng tiền seller — tiền winner đã bị trừ lúc bid
        if (finalStatus == AuctionStatus.FINISHED && auction.getCurrentWinner() != null) {
            userDao.incrementBalance(
                    auction.getItem().getSeller().getId(),
                    auction.getCurrentPrice());
            logger.info("[SETTLEMENT] Paid " + auction.getCurrentPrice()
                    + " → seller: " + auction.getItem().getSeller().getUsername());
        }

        autoBidDao.deactivateAllForAuction(auction.getId());

        // Lifecycle: broadcast tới mọi client lobby để remove auction khỏi list
        notifyAuctionEnded(auction);

        removeAuction(auction.getId());
    }


    // cancelAuction — admin hủy

    public void cancelAuction(String auctionId, String reason) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        AuctionStatus st = auction.getStatus();
        if (st == AuctionStatus.FINISHED || st == AuctionStatus.CANCELED
                || st == AuctionStatus.PAID) return;

        // 1. Dừng timer — tránh onAuctionEnd fire sau khi đã cancel
        if (auction.getTimer() != null) auction.getTimer().cancel();

        // 2. Hoàn tiền currentWinner — tiền đang bị escrow
        Bidder winner = auction.getCurrentWinner();
        if (winner != null) {
            userDao.incrementBalance(winner.getId(), auction.getCurrentPrice());
            winner.setBalance(winner.getBalance() + auction.getCurrentPrice());
            logger.warning("[CANCEL] Refunded " + auction.getCurrentPrice()
                    + " → " + winner.getUsername());
        }

        // 3. Đổi status RAM + notify (per-auction observers)
        auction.cancel(reason);

        // 4. Cập nhật DB
        auctionDao.updateStatus(auctionId, AuctionStatus.CANCELED);

        // 5. Deactivate tất cả autobid của auction này trong DB
        autoBidDao.deactivateAllForAuction(auctionId);

        // 6. Lifecycle: broadcast tới mọi client lobby
        notifyAuctionCancelled(auction, reason);

        removeAuction(auctionId);
    }

    // ---------------------------------------------------------------
    // Map management
    // ---------------------------------------------------------------
    public void removeAuction(String auctionId) {
        Auction removed = activeAuctions.remove(auctionId);
        if (removed != null)
            logger.info("[AuctionManager] Removed: " + auctionId.substring(0, 8));
    }

    public Auction getAuction(String auctionId)  { return activeAuctions.get(auctionId); }
    public boolean exists(String auctionId)       { return activeAuctions.containsKey(auctionId); }
    public int getActiveCount()                   { return activeAuctions.size(); }
    public void delete(String auctionId)          { removeAuction(auctionId); }

    public List<Auction> getAllRunning() {
        List<Auction> r = new ArrayList<>();
        for (Auction a : activeAuctions.values())
            if (a.getStatus() == AuctionStatus.RUNNING) r.add(a);
        return r;
    }

    public List<Auction> getAllOpen() {
        List<Auction> r = new ArrayList<>();
        for (Auction a : activeAuctions.values())
            if (a.getStatus() == AuctionStatus.OPEN) r.add(a);
        return r;
    }

    public List<Auction> getAllActive() {
        return new ArrayList<>(activeAuctions.values());
    }
}