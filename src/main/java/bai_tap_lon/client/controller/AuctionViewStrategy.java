package bai_tap_lon.client.controller;

import javafx.scene.Node;
import javafx.scene.layout.HBox;

/**
 * Strategy chứa toàn bộ phần UI / hành vi PHỤ THUỘC ROLE
 * cho 2 màn AuctionList và ActiveAuctions.
 *
 * 3 concrete implementations: BidderAuctionStrategy, SellerAuctionStrategy,
 * AdminAuctionStrategy. Lấy strategy qua {@link AuctionStrategyFactory}.
 *
 * Khi cần thêm role mới (vd: Moderator) → chỉ cần thêm 1 class implement
 * interface này + đăng ký ở Factory. Controller KHÔNG phải sửa.
 */
public interface AuctionViewStrategy {

    /** Tên role để debug / log. Trả về dạng UPPER (BIDDER/SELLER/ADMIN). */
    String roleName();

    // ────────────────────────────────────────────────────────────
    // 1. NAVBAR — hiện đúng nav theo role, ẩn 2 nav còn lại
    // ────────────────────────────────────────────────────────────
    void applyNavbar(HBox adminNav, HBox sellerNav, HBox bidderNav);

    // ────────────────────────────────────────────────────────────
    // 2. ACTIVE AUCTIONS CARD — cấu hình 2 nút primary/detail trên card
    // ────────────────────────────────────────────────────────────
    /**
     * Cấu hình 2 nút (registerBtn + detailBtn) của 1 AuctionCard.
     *
     * @param card           controller của card vừa load
     * @param auctionId      id phiên (để bind vào handler)
     * @param status         status phiên (RUNNING/OPEN/...) — strategy có thể tuỳ
     *                       theo status mà đổi text/visibility
     * @param handlers       các callback chung (placeBid, watch, cancel, openDetail)
     */
    void configureCard(AuctionCardController card,
                       String auctionId,
                       String status,
                       AuctionActionHandlers handlers);

    // ────────────────────────────────────────────────────────────
    // 3. AUCTION LIST ROW — build Node chứa các nút action cho 1 row
    // ────────────────────────────────────────────────────────────
    /**
     * Build Node hiển thị action buttons cho 1 row trong AuctionList table.
     * Node trả về sẽ được set vào TableCell.setGraphic().
     *
     * @param auctionId  id phiên
     * @param status     status phiên (đã uppercase)
     * @param handlers   các callback chung
     * @return           HBox chứa các nút (hoặc empty HBox nếu không có action)
     */
    Node buildRowActions(String auctionId,
                         String status,
                         AuctionActionHandlers handlers);

    // ────────────────────────────────────────────────────────────
    // 4. ROW DOUBLE-CLICK — status nào được phép mở Bidding screen
    // ────────────────────────────────────────────────────────────
    /**
     * Mặc định: chỉ status OPEN cho double-click (preview phiên chưa bắt đầu).
     * Strategy override khi cần: vd Seller cho phép cả RUNNING.
     */
    default boolean canOpenBiddingFromRow(String status) {
        return "OPEN".equals(status);
    }
}
