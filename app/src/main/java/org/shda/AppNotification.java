package org.shda;

public class AppNotification {
    public String id;
    public String title;
    public String message;
    public long timestamp;
    public boolean isRead;
    public String type; // "CHANDA", "EVENT", "POLL"

    public AppNotification() {}

    public AppNotification(String id, String title, String message, long timestamp, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
        this.type = type;
    }
}
