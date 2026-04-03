package org.shda;

public class Event {
    public String id;
    public String title;
    public String date;
    public String description;
    public long timestamp;

    // Required by Firebase
    public Event() {}

    public Event(String id, String title, String date, String description, long timestamp) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.description = description;
        this.timestamp = timestamp;
    }
}
