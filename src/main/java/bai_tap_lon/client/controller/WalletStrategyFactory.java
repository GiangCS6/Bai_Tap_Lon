package bai_tap_lon.client.controller;

import java.util.Locale;

/** Trả {@link WalletStrategy} phù hợp với role của user hiện tại. */
public final class WalletStrategyFactory {

    private WalletStrategyFactory() {}

    public static WalletStrategy forRole(String role) {
        if (role == null) return new NoWalletStrategy();
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "BIDDER" -> new BidderWalletStrategy();
            case "SELLER" -> new SellerWalletStrategy();
            default       -> new NoWalletStrategy();
        };
    }
}