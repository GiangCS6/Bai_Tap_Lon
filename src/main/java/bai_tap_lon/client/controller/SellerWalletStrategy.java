package bai_tap_lon.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Seller: chỉ được rút tiền (ẩn nút nạp). */
public class SellerWalletStrategy implements WalletStrategy {

    @Override
    public void applyButtons(Button depositBtn, Button withdrawBtn, Label hint) {
        WalletStrategy.show(depositBtn,  false);
        WalletStrategy.show(withdrawBtn, true);
        if (hint != null) hint.setText("Seller · Chỉ rút tiền");
    }

    @Override public boolean canDeposit()  { return false; }
    @Override public boolean canWithdraw() { return true; }
    @Override public boolean hasWallet()   { return true; }
}