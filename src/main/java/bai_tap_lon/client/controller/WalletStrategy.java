package bai_tap_lon.client.controller;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Chiến lược ví theo role — quyết định nút nào hiện, hint text gì,
 * và có quyền thực hiện thao tác nạp / rút hay không.
 *
 * Triển khai cụ thể:
 *   - {@link BidderWalletStrategy} : nạp + rút.
 *   - {@link SellerWalletStrategy} : chỉ rút.
 *   - {@link NoWalletStrategy}     : không có ví (admin / role lạ).
 *
 * Mục đích Strategy: cách ly logic role-specific khỏi {@code WalletController},
 * controller chỉ giữ network + glue UI.
 */
public interface WalletStrategy {

    /** Cấu hình tính nhìn-được của 2 nút và set hint text theo role. */
    void applyButtons(Button depositBtn, Button withdrawBtn, Label hint);

    boolean canDeposit();
    boolean canWithdraw();

    /** Trả false → controller phải đóng popup ngay, không cho thao tác. */
    boolean hasWallet();

    // ── helper dùng chung ──
    static void show(Node n, boolean visible) {
        if (n == null) return;
        n.setVisible(visible);
        n.setManaged(visible);
    }
}