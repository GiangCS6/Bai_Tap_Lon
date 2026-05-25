package bai_tap_lon.client.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Strategy cho Seller.
 *
 * Navbar:    sellerNav
 * Card:      registerBtn ẨN | detailBtn = "Details"
 * List row:  chỉ "View"
 */
public class SellerAuctionStrategy implements AuctionViewStrategy {

    @Override public String roleName() { return "SELLER"; }

    @Override
    public void applyNavbar(HBox adminNav, HBox sellerNav, HBox bidderNav) {
        StrategyUiUtils.setVisible(adminNav, false);
        StrategyUiUtils.setVisible(sellerNav, true);
        StrategyUiUtils.setVisible(bidderNav, false);
    }

    @Override
    public void configureCard(AuctionCardController card,
                              String auctionId,
                              String status,
                              AuctionActionHandlers handlers) {
        card.configureForRole("seller",
                null,
                handlers.onOpenDetail);
    }

    @Override
    public boolean canOpenBiddingFromRow(String status) {
        // Seller xem được cả phiên chưa bắt đầu (OPEN) lẫn đang chạy (RUNNING)
        return "OPEN".equals(status) || "RUNNING".equals(status);
    }

    @Override
    public Node buildRowActions(String auctionId,
                                String status,
                                AuctionActionHandlers handlers) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);

        Button view = StrategyUiUtils.primaryBtn("View",
                e -> handlers.onOpenDetail.accept(auctionId));
        box.getChildren().add(view);
        return box;
    }
}
