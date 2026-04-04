package org.shda;

import java.util.HashMap;

public class Poll {
    public String id;
    public String question;
    public String optionA;
    public String optionB;
    public long timestamp;
    public String createdBy;
    public String officialComment; // NEW: To store Admin/Manager signed comments
    public HashMap<String, String> votes = new HashMap<>(); 

    // Required by Firebase
    public Poll() {}

    public Poll(String id, String question, String optionA, String optionB, long timestamp, String createdBy) {
        this.id = id;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.timestamp = timestamp;
        this.createdBy = createdBy;
        this.officialComment = ""; // Starts empty
    }
}
