package bai_tap_lon.client.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Strategy cho Bidder.
 *
 * Navbar:    bidderNav
 * Card:      registerBtn = "Place Bid" (RUNNING) | detailBtn = "Details"
 * List row:  "Join" (RUNNING → mở màn Bidding) | "Care" (OPEN → thêm Watchlist)
 */
public class BidderAuctionStrategy implements AuctionViewStrategy {

    @Override public String roleName() { return "BIDDER"; }

    @Override
    public void applyNavbar(HBox adminNav, HBox sellerNav, HBox bidderNav) {
        StrategyUiUtils.setVisible(adminNav, false);
        StrategyUiUtils.setVisible(sellerNav, false);
        StrategyUiUtils.setVisible(bidderNav, true);
    }

    @Override
    public void configureCard(AuctionCardController card,
                              String auctionId,
                              String status,
                              AuctionActionHandlers handlers) {
        card.configureForRole("bidder",
                handlers.onPlaceBid,
                handlers.onOpenDetail);
    }

    @Override
    public Node buildRowActions(String auctionId,
                                String status,
                                AuctionActionHandlers handlers) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);

        if ("RUNNING".equals(status)) {
            Button join = StrategyUiUtils.primaryBtn("Join",
                    e -> handlers.onPlaceBid.accept(auctionId));
            Button care = StrategyUiUtils.primaryBtn("Care",
                    e -> handlers.onWatch.accept(auctionId));
            box.getChildren().addAll(join,care);
        } else if ("OPEN".equals(status)) {
            Button join = StrategyUiUtils.primaryBtn("Join",
                    e -> handlers.onPlaceBid.accept(auctionId));
            Button care = StrategyUiUtils.primaryBtn("Care",
                    e -> handlers.onWatch.accept(auctionId));
            box.getChildren().addAll(join, care);
        }
        return box;
    }
}
