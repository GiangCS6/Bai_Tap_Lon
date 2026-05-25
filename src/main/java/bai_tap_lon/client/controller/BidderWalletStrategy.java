package bai_tap_lon.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Bidder: được nạp và rút tiền. */
public class BidderWalletStrategy implements WalletStrategy {

    @Override
    public void applyButtons(Button depositBtn, Button withdrawBtn, Label hint) {
        WalletStrategy.show(depositBtn,  true);
        WalletStrategy.show(withdrawBtn, true);
        if (hint != null) hint.setText("Bidder · Có thể nạp và rút tiền");
    }

    @Override public boolean canDeposit()  { return true; }
    @Override public boolean canWithdraw() { return true; }
    @Override public boolean hasWallet()   { return true; }
}