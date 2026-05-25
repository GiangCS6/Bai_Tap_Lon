package bai_tap_lon.client.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Strategy cho Admin.
 *
 * Navbar:    adminNav
 * Card:      registerBtn = "Cancel" | detailBtn = "Details"
 * List row:  chỉ "Cancel"
 */
public class AdminAuctionStrategy implements AuctionViewStrategy {

    @Override public String roleName() { return "ADMIN"; }

    @Override
    public void applyNavbar(HBox adminNav, HBox sellerNav, HBox bidderNav) {
        StrategyUiUtils.setVisible(adminNav, true);
        StrategyUiUtils.setVisible(sellerNav, false);
        StrategyUiUtils.setVisible(bidderNav, false);
    }

    @Override
    public void configureCard(AuctionCardController card,
                              String auctionId,
                              String status,
                              AuctionActionHandlers handlers) {
        card.configureForRole("admin",
                handlers.onCancel,
                handlers.onOpenDetail);
    }

    @Override
    public Node buildRowActions(String auctionId,
                                String status,
                                AuctionActionHandlers handlers) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);

        Button cancel = StrategyUiUtils.primaryBtn("Cancel",
                e -> handlers.onCancel.accept(auctionId));
        box.getChildren().add(cancel);
        return box;
    }
}
