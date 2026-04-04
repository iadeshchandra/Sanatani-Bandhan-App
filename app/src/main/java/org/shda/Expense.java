package org.shda;

public class Expense {
    public String id;
    public float amount;
    public String purpose;
    public String comment;
    public long timestamp;
    public String loggedBy;

    // Required empty constructor for Firebase
    public Expense() {}

    public Expense(String id, float amount, String purpose, String comment, long timestamp, String loggedBy) {
        this.id = id;
        this.amount = amount;
        this.purpose = purpose;
        this.comment = comment;
        this.timestamp = timestamp;
        this.loggedBy = loggedBy;
    }
}
