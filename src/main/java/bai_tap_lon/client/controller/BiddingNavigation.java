package bai_tap_lon.client.controller;

import bai_tap_lon.client.ClientStart;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
public final class BiddingNavigation {

    private static final String DEFAULT_BACK_FXML = "/viewfxml/active-auctions.fxml";

    private BiddingNavigation() {}

    public static void open(Node sourceNode, String auctionId) {
        open(sourceNode, auctionId, DEFAULT_BACK_FXML);
    }

    public static void open(Node sourceNode, String auctionId, String backFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(BiddingNavigation.class.getResource("/viewfxml/Bidding.fxml"));
            Parent root = loader.load();

            BiddingController ctrl = loader.getController();
            ctrl.initWithId(auctionId, backFxml);
            // Use ClientApp's method to ensure scene dimensions match the maximized stage
            ClientStart.setSceneWithCurrentDimensions(root);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Cannot open bidding: " + e.getMessage()).showAndWait();
        }
    }
}