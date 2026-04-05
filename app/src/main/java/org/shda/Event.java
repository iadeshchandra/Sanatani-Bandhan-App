package org.shda;

public class Event {
    public String id, title, dateStr, description, location, createdBy;
    public long timestamp, eventDateTs;

    public Event() {}

    public Event(String id, String title, String dateStr, long eventDateTs, String description, String location, long timestamp, String createdBy) {
        this.id = id;
        this.title = title;
        this.dateStr = dateStr;
        this.eventDateTs = eventDateTs;
        this.description = description;
        this.location = location;
        this.timestamp = timestamp;
        this.createdBy = createdBy;
    }
}
