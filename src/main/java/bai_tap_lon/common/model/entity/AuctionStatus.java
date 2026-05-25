package bai_tap_lon.common.model.entity;
public enum AuctionStatus {
    OPEN,       // phiên vừa tạo, chờ đến giờ bắt đầu
    RUNNING,    // đang diễn ra, nhận bid
    FINISHED,   // hết giờ, có người thắng
    PAID,       // người thắng đã thanh toán
    CANCELED    // bị hủy — không ai đặt giá hoặc admin hủy
}