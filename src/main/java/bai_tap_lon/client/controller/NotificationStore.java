package bai_tap_lon.client.controller;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;

public class NotificationStore {
    private static final NotificationStore INSTANCE = new NotificationStore();

    public static NotificationStore getInstance() {
        return INSTANCE;
    }

    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    private NotificationStore() {
    }

    public ObservableList<Notification> getNotifications() {
        return notifications;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount.get();
    }

    public void add(Notification notification) {
        if (notification == null) return;
        runOnFxThread(() -> {
            notifications.add(0, notification);
            if (!notification.isRead()) {
                unreadCount.set(unreadCount.get() + 1);
            }
        });
    }

    public void clear() {
        runOnFxThread(() -> {
            notifications.clear();
            unreadCount.set(0);
        });
    }

    public void markAllAsRead() {
        runOnFxThread(() -> {
            for (Notification notification : notifications) {
                notification.setRead(true);
            }
            unreadCount.set(0);
        });
    }

    public void bindBadge(Label badge) {
        if (badge == null) return;
        badge.textProperty().unbind();
        badge.textProperty().bind(unreadCountProperty().asString());
    }

    private static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
