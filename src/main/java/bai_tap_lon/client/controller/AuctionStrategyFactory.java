package bai_tap_lon.client.controller;

import java.util.Locale;

/**
 * Factory chọn Strategy theo role string.
 *
 * Role bắt buộc phải hợp lệ (BIDDER/SELLER/ADMIN). Nếu null/blank/unknown →
 * throw IllegalStateException. Triết lý fail-fast: nếu role bậy → có bug ở
 * SessionManager hoặc LoginController, không silent fallback.
 *
 * Khi thêm role mới: thêm 1 case ở {@link #forRole(String)} và 1 class
 * implement {@link AuctionViewStrategy}. Controller KHÔNG cần đụng vào.
 */
public final class AuctionStrategyFactory {

    private AuctionStrategyFactory() {}

    public static AuctionViewStrategy forRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalStateException(
                    "Role không được null/blank — SessionManager chưa có user? " +
                    "Kiểm tra LoginController.onLoginSuccess()");
        }
        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "admin"  -> new AdminAuctionStrategy();
            case "seller" -> new SellerAuctionStrategy();
            case "bidder" -> new BidderAuctionStrategy();
            default       -> throw new IllegalStateException("Unknown role: " + role);
        };
    }
}
