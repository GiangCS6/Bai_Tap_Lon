package bai_tap_lon.common.model.entity;
public interface BidObserver {

    // Được gọi khi có bid mới hợp lệ
    void onBidPlaced(BidTransaction tx);

    // Được gọi khi phiên bắt đầu (OPEN → RUNNING)
    void onAuctionStarted(Auction auction);

    // Được gọi khi phiên kết thúc (RUNNING → FINISHED/CANCELED)
    void onAuctionEnd(Auction auction);

    // Được gọi khi phiên bị gia hạn (anti-sniping)
    void onTimeExtended(Auction auction);

    // Được gọi khi phiên bị hủy
    void onAuctionCancelled(Auction auction, String reason);
}