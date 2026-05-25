package bai_tap_lon.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Popup;
import javafx.stage.Window;

public class NotificationPopup {
    private static Popup currentPopup;

    private NotificationPopup() {
    }

    public static Popup showFor(Button anchor) {
        if (anchor == null || anchor.getScene() == null) return null;
        try {
            if (currentPopup != null && currentPopup.isShowing()) {
                currentPopup.hide();
            }

            FXMLLoader loader = new FXMLLoader(
                    NotificationPopup.class.getResource("/viewfxml/Notification.fxml"));
            Parent root = loader.load();

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Window owner = anchor.getScene().getWindow();
            Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            double popupWidth = 380;
            double x = b.getMaxX() - popupWidth;
            double y = b.getMaxY() + 6;

            popup.show(owner, x, y);
            popup.setOnHidden(e -> {
                if (currentPopup == popup) currentPopup = null;
            });
            currentPopup = popup;
            return popup;
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Không mở được thông báo: " + e.getMessage()).showAndWait();
            return null;
        }
    }
}
