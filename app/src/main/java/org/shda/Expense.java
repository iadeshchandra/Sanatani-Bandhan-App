package org.shda;

public class Expense {
    public String id, eventName, itemName, involvedPerson, loggedBy;
    public float amount;
    public long timestamp;

    // Required by Firebase
    public Expense() {}

    public Expense(String id, String eventName, String itemName, float amount, String involvedPerson, long timestamp, String loggedBy) {
        this.id = id;
        this.eventName = eventName;
        this.itemName = itemName;
        this.amount = amount;
        this.involvedPerson = involvedPerson;
        this.timestamp = timestamp;
        this.loggedBy = loggedBy;
    }
}
