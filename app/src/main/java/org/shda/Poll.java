package org.shda;

import java.util.HashMap;

public class Poll {
    public String id, question, optionA, optionB, createdBy, officialComment;
    public long timestamp;
    public HashMap<String, String> votes;

    // 🔥 CRITICAL FIX: Firebase requires this empty constructor, or it crashes!
    public Poll() {}

    public Poll(String id, String question, String optionA, String optionB, long timestamp, String createdBy) {
        this.id = id;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.timestamp = timestamp;
        this.createdBy = createdBy;
        this.votes = new HashMap<>();
    }
}
