package bai_tap_lon.common.model.entity;

import bai_tap_lon.client.network.Client;

import bai_tap_lon.common.network.TimeUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Tách hoàn toàn logic đếm ngược / scheduling ra khỏi Auction.
 *
 * Trách nhiệm duy nhất:
 *   - Đặt lịch gọi onStart() và onEnd() đúng thời điểm
 *   - Hỗ trợ extendTime() để reschedule onEnd() (anti-sniping)
 *   - Hỗ trợ cancel() để huỷ timer khi admin cancel auction
 *
 * Auction chỉ cần tạo AuctionTimer và truyền callback vào —
 * không cần biết gì về ScheduledExecutorService hay ScheduledFuture.
 */
public class AuctionTimer {

    private final String auctionId ;
    private final Runnable onStart;
    private final Runnable onEnd;

    private LocalDateTime endTime;

    private final ScheduledExecutorService scheduler ;

    private ScheduledFuture<?> endFuture;

    private static final Logger logger = Logger.getLogger(AuctionTimer.class.getName());



    /**
     * @param auctionId  dùng để đặt tên thread, dễ debug
     * @param startTime  thời điểm bắt đầu phiên
     * @param endTime    thời điểm kết thúc phieen
     * @param onStart    callback được gọi khi đến giờ bắt đầu
     * @param onEnd      callback được gọi khi hết giờ
     */
    public AuctionTimer(String auctionId,
                        LocalDateTime startTime,
                        LocalDateTime endTime,
                        Runnable onStart,
                        Runnable onEnd) {
        this.auctionId = auctionId;
        this.endTime   = endTime;
        this.onStart   = onStart;
        this.onEnd     = onEnd;

        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);

            String safeId = (auctionId != null && auctionId.length() >= 8)
                    ? auctionId.substring(0, 8)
                    : String.valueOf(auctionId);

            t.setName("AuctionTimer-" + safeId);
            return t;
        });
    }

    /**
     * Bắt đầu đếm ngược — đặt lịch cho cả start và end.
     */
    public void start(LocalDateTime startTime) {
        long secondsUntilStart = ChronoUnit.SECONDS.between(TimeUtil.now(), startTime);
        long secondsUntilEnd   = ChronoUnit.SECONDS.between(TimeUtil.now(), endTime);
        if (secondsUntilStart < 0) secondsUntilStart = 0;

        scheduler.schedule(onStart, secondsUntilStart, TimeUnit.SECONDS);
        endFuture = scheduler.schedule(onEnd, Math.max(secondsUntilEnd, 0), TimeUnit.SECONDS);

        logger.info("[TIMER] Auction " + auctionId.substring(0, 8)
                + " start after " + secondsUntilStart + "s"
                + " | end after " + secondsUntilEnd + "s");
    }

    /**
     * Gia hạn thêm  giây (anti-sniping).
     * Huỷ timer cũ, đặt lại timer mới với endTime mới.
     */
    public synchronized LocalDateTime extendTime(int extraSeconds) {
        if (endFuture != null && !endFuture.isDone()) {
            endFuture.cancel(false);
        }
        endTime = endTime.plusSeconds(extraSeconds);
        long newDelay = ChronoUnit.SECONDS.between(TimeUtil.now(), endTime);
        endFuture = scheduler.schedule(onEnd, Math.max(newDelay, 0), TimeUnit.SECONDS);

        logger.info("[TIMER] Auction " + auctionId.substring(0, 8)
                + " extended +" + extraSeconds + "s → " + endTime);
        return endTime;
    }

    /**
     * Huỷ toàn bộ timer — dùng khi admin cancel auction.
     */
    public void cancel() {
        if (endFuture != null && !endFuture.isDone()) {
            endFuture.cancel(false);
        }
        scheduler.shutdownNow();
        logger.info("[TIMER] Auction " + auctionId.substring(0, 8) + " timer cancelled");
    }


    public void shutdown() {
        scheduler.shutdownNow();
    }

    public LocalDateTime getEndTime() { return endTime; }

    public synchronized long secondsLeft() {
        return ChronoUnit.SECONDS.between(TimeUtil.now(), endTime);
    }
}