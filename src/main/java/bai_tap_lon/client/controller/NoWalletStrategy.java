package bai_tap_lon.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Admin / role không hỗ trợ ví. Tất cả nút bị ẩn, mọi thao tác bị từ chối.
 * Controller dùng {@link #hasWallet()} = false để đóng popup ngay.
 */
public class NoWalletStrategy implements WalletStrategy {

    @Override
    public void applyButtons(Button depositBtn, Button withdrawBtn, Label hint) {
        WalletStrategy.show(depositBtn,  false);
        WalletStrategy.show(withdrawBtn, false);
        if (hint != null) hint.setText("Tài khoản này không có ví");
    }

    @Override public boolean canDeposit()  { return false; }
    @Override public boolean canWithdraw() { return false; }
    @Override public boolean hasWallet()   { return false; }
}