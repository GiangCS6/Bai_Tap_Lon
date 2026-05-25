package bai_tap_lon.client.controller;

import java.time.LocalDateTime;

public class Notification {
    private String title;
    private String body;
    private String type;
    private LocalDateTime timestamp;
    private boolean isRead;

    public Notification(String title, String body, String type, LocalDateTime timestamp, boolean isRead) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
