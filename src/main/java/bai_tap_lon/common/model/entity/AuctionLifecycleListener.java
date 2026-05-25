package bai_tap_lon.common.model.entity;

/**
 * Listener cho các sự kiện vòng đời auction ở phạm vi toàn manager
 * (broadcast cho mọi client đang kết nối — KHÔNG phải observer per-auction
 * như {@link BidObserver}).
 *
 * Dùng cho lobby (AuctionList / ActiveAuctions) cần cập nhật realtime khi:
 *   - Có phiên mới được tạo
 *   - Phiên chuyển OPEN → RUNNING
 *   - Phiên kết thúc (FINISHED / CANCELED do hết giờ)
 *   - Phiên bị admin cancel
 *
 * Tất cả method là default no-op để impl chỉ cần override cái mình quan tâm.
 */
public interface AuctionLifecycleListener {

    /** Phiên vừa được tạo (sau POST_ITEM thành công). */
    default void onAuctionCreated(Auction auction) {}

    /** Phiên vừa chuyển OPEN → RUNNING (timer fire). */
    default void onAuctionStarted(Auction auction) {}

    /** Phiên vừa kết thúc do hết giờ (RUNNING → FINISHED/CANCELED). */
    default void onAuctionEnded(Auction auction) {}

    /** Phiên bị admin cancel. */
    default void onAuctionCancelled(Auction auction, String reason) {}
}