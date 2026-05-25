package bai_tap_lon.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NotificationController {
    @FXML private VBox notificationListBox;
    @FXML private Button clearAllBtn;

    private final NotificationStore store = NotificationStore.getInstance();

    @FXML
    public void initialize() {
        render();
    }

    @FXML
    private void onClearAll(ActionEvent event) {
        store.clear();
        render();
    }

    private void render() {
        notificationListBox.getChildren().clear();
        if (store.getNotifications().isEmpty()) {
            Label emptyLabel = new Label("Chưa có thông báo nào.");
            emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            notificationListBox.getChildren().add(emptyLabel);
            return;
        }

        for (Notification notification : store.getNotifications()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/viewfxml/notification-card.fxml"));
                Parent card = loader.load();
                NotificationCardController controller = loader.getController();
                controller.setNotification(notification);
                notificationListBox.getChildren().add(card);
            } catch (Exception e) {
                Label fallback = new Label(notification.getTitle());
                fallback.setStyle("-fx-text-fill: #E2E8F0;");
                notificationListBox.getChildren().add(fallback);
            }
        }
    }
}
